package com.github.alexthe666.rats.server.brick;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;
import com.mojang.logging.LogUtils;

public final class CounterNetwork {
    public record Row(ResourceLocation recipe,ItemStack output,boolean eaten){}
    public record Catalog(BlockPos counter,List<Row> rows,boolean gourmet,int count,Component next,Component status){}
    public record Entry(UUID ticket,ResourceLocation recipe,ItemStack food,boolean mine){}
    public record QueueState(BlockPos counter,List<Entry> orders,Component status){}
    public record Request(BlockPos counter,int action,ResourceLocation recipe,Optional<UUID> ticket){}
    private static final boolean GOURMET=ModList.get().isLoaded("ageofmythology");
    @SuppressWarnings("unchecked")
    private static Set<String> eaten(ServerPlayer p){
        if(!GOURMET)return Set.of();
        try{
            var impl=Class.forName("com.kurome.ageofmythology.capability.PlayerDataCapability");
            var info=p.getCapability((Capability<Object>)impl.getField("INSTANCE").get(null)).resolve();
            if(info.isPresent())return (Set<String>)Class.forName("com.kurome.ageofmythology.capability.api.IPlayerData").getMethod("getFoodList").invoke(info.get());
        }catch(ReflectiveOperationException e){LogUtils.getLogger().debug("Gourmet food history unavailable",e);}
        return Set.of();
    }
    public static void show(ServerPlayer player,OrderCounter counter){
        var stations=counter.stations(false);Set<String> eaten=eaten(player);
        var dishes=KitchenRecipes.all(counter.world());List<Row> rows=new ArrayList<>();
        for(var d:dishes.values())if(stations.containsKey(d.station())){
            String id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(d.display().getItem()).toString();
            rows.add(new Row(d.id(),d.display(),eaten.contains(id)));
        }
        var next=counter.orders.peek();var first=next==null?null:dishes.get(next.recipe());
        var msg=new Catalog(counter.getBlockPos(),rows,GOURMET,counter.orders.size(),first==null?Component.empty():first.display().getHoverName(),counter.status);
        RatsNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),msg);
    }
    private static void sendQueue(ServerPlayer player,OrderCounter counter){
        var recipes=KitchenRecipes.all(counter.world());
        var entries=counter.orders.stream().map(o->{
            var dish=recipes.get(o.recipe());
            return new Entry(o.ticket(),o.recipe(),dish==null?ItemStack.EMPTY:dish.display(),o.customer().equals(player.getUUID()));
        }).toList();
        var status=entries.isEmpty()?Component.empty():counter.status;
        RatsNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new QueueState(counter.getBlockPos(),entries,status));
    }
    public static void register(SimpleChannel ch,int id){
        ch.registerMessage(id,Catalog.class,CounterNetwork::write,CounterNetwork::read,CounterNetwork::display,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        ch.registerMessage(id+1,Request.class,(m,b)->{b.writeBlockPos(m.counter);b.writeByte(m.action);b.writeResourceLocation(m.recipe);b.writeOptional(m.ticket,FriendlyByteBuf::writeUUID);},
            b->new Request(b.readBlockPos(),b.readUnsignedByte(),b.readResourceLocation(),b.readOptional(FriendlyByteBuf::readUUID)),CounterNetwork::request,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        ch.registerMessage(id+2,QueueState.class,(q,b)->{
            b.writeBlockPos(q.counter);b.writeComponent(q.status);
            b.writeCollection(q.orders,(buf,o)->{buf.writeUUID(o.ticket);buf.writeResourceLocation(o.recipe);buf.writeItem(o.food);buf.writeBoolean(o.mine);});
        },b->{
            var pos=b.readBlockPos();var status=b.readComponent();
            return new QueueState(pos,b.readList(buf->new Entry(buf.readUUID(),buf.readResourceLocation(),buf.readItem(),buf.readBoolean())),status);
        },(q,ctx)->{
            ctx.get().enqueueWork(()->com.github.alexthe666.rats.client.gui.BrickOrderScreen.update(q));ctx.get().setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    private static void write(Catalog m,FriendlyByteBuf b){
        b.writeBlockPos(m.counter);b.writeBoolean(m.gourmet);b.writeVarInt(m.count);b.writeComponent(m.next);b.writeComponent(m.status);
        b.writeCollection(m.rows,(buf,r)->{buf.writeResourceLocation(r.recipe);buf.writeItem(r.output);buf.writeBoolean(r.eaten);});
    }
    private static Catalog read(FriendlyByteBuf b){
        var pos=b.readBlockPos();boolean gourmet=b.readBoolean();int count=b.readVarInt();var next=b.readComponent();var status=b.readComponent();
        return new Catalog(pos,b.readList(buf->new Row(buf.readResourceLocation(),buf.readItem(),buf.readBoolean())),gourmet,count,next,status);
    }
    private static void display(Catalog m,Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->com.github.alexthe666.rats.client.gui.BrickOrderScreen.show(m));ctx.get().setPacketHandled(true);
    }
    public static void send(BlockPos pos,int action,ResourceLocation id){
        RatsNetworkHandler.CHANNEL.sendToServer(new Request(pos,action,id,Optional.empty()));
    }
    public static void cancel(BlockPos pos,UUID ticket){
        RatsNetworkHandler.CHANNEL.sendToServer(new Request(pos,2,new ResourceLocation("rats","order_counter"),Optional.of(ticket)));
    }
    private static void request(Request m,Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->{
            var p=ctx.get().getSender();
            if(p==null||p.isSpectator()||p.distanceToSqr(m.counter.getCenter())>64||!p.serverLevel().hasChunkAt(m.counter)||!(p.level().getBlockEntity(m.counter) instanceof OrderCounter c))return;
            if(m.action==1){
                if(!c.enqueue(p,m.recipe))p.displayClientMessage(Component.translatable("brick.rats.cannot_order"),true);
            }else if(m.action==2){
                if(m.ticket.isEmpty()||!c.cancel(p,m.ticket.get()))p.displayClientMessage(Component.translatable("brick.rats.gone"),true);
            }else if(m.action==0){show(p,c);return;}
            else if(m.action!=3)return;
            sendQueue(p,c);
        });
        ctx.get().setPacketHandled(true);
    }
}
