package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.entity.rat.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import java.util.*;

public final class OrderCounter extends BlockEntity {
    public record Order(UUID ticket,UUID customer,ResourceLocation recipe){}
    final Deque<Order> orders=new ArrayDeque<>();
    private UUID worker;
    Component status=Component.empty();
    private long refreshed=Long.MIN_VALUE;
    private final Map<KitchenRecipes.Station,List<BlockPos>> stations=new EnumMap<>(KitchenRecipes.Station.class);
    public OrderCounter(BlockPos pos,BlockState state){super(RatsBlockEntityRegistry.ORDER_COUNTER.get(),pos,state);}
    ServerLevel world(){return (ServerLevel)level;}
    private boolean inside(BlockPos p){
        int x=p.getX()-worldPosition.getX(),y=p.getY()-worldPosition.getY(),z=p.getZ()-worldPosition.getZ();
        return x>=-8&&x<8&&z>=-8&&z<8&&y>=-5&&y<5;
    }
    Map<KitchenRecipes.Station,List<BlockPos>> stations(boolean fresh){
        long now=level.getGameTime();
        if(!fresh&&refreshed!=Long.MIN_VALUE&&now-refreshed<60)return stations;
        stations.clear();refreshed=now;
        for(var p:BlockPos.betweenClosed(worldPosition.offset(-8,-5,-8),worldPosition.offset(7,4,7))){
            if(!world().hasChunkAt(p))continue;
            var type=KitchenRecipes.station(world(),p);
            if(type!=null)stations.computeIfAbsent(type,k->new ArrayList<>()).add(p.immutable());
        }
        return stations;
    }
    List<IItemHandler> containers(ServerPlayer owner){
        Set<IItemHandler> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        List<IItemHandler> list=new ArrayList<>();
        var p=worldPosition;
        List<BlockEntity> blocks=new ArrayList<>();
        for(int cx=(p.getX()-8)>>4;cx<=(p.getX()+7)>>4;cx++)for(int cz=(p.getZ()-8)>>4;cz<=(p.getZ()+7)>>4;cz++){
            var chunk=world().getChunkSource().getChunkNow(cx,cz);
            if(chunk!=null)for(var be:chunk.getBlockEntities().values())if(inside(be.getBlockPos())&&be!=this)blocks.add(be);
        }
        blocks.sort(Comparator.comparingDouble(be->be.getBlockPos().distSqr(p)));
        for(var be:blocks){
            if(be instanceof BaseContainerBlockEntity c){
                if(owner==null && c.saveWithoutMetadata().contains("Lock"))continue;
                if(owner!=null&&!c.canOpen(owner))continue;
            }
            // Kitchen workstations are in use; take ingredients from storage containers.
            var block=world().getBlockState(be.getBlockPos());
            String mod=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block.getBlock()).getNamespace();
            if(KitchenRecipes.isWorkstation(block)||mod.equals("refinedstorage")||mod.equals("refinedstorageaddons"))continue;
            be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h->{if(seen.add(h))list.add(h);});
        }
        return list;
    }
    boolean enqueue(ServerPlayer player,ResourceLocation id){
        var dish=KitchenRecipes.all(world()).get(id);
        if(dish==null||!stations(true).containsKey(dish.station())||orders.size()>=64)return false;
        orders.addLast(new Order(UUID.randomUUID(),player.getUUID(),id));setChanged();return true;
    }
    boolean cancel(ServerPlayer player,UUID ticket){
        var iter=orders.iterator();boolean head=true;
        while(iter.hasNext()){
            var order=iter.next();
            if(order.ticket().equals(ticket) && order.customer().equals(player.getUUID())){
                iter.remove();
                if(head){
                    UUID currentWorker=worker;worker=null;status=Component.empty();
                    if(currentWorker!=null && world().getEntity(currentWorker) instanceof TamedRat r && r.kitchen.at(worldPosition))
                        r.kitchen.cancelOrder();
                }
                setChanged();return true;
            }
            head=false;
        }
        return false;
    }
    boolean assigned(TamedRat rat){return rat.getUUID().equals(worker)&&!orders.isEmpty()&&orders.peek().customer.equals(rat.getOwnerUUID());}
    void finish(TamedRat rat){
        if(assigned(rat)){orders.removeFirst();worker=null;status=Component.empty();setChanged();}
    }
    public static void tick(Level world,BlockPos pos,BlockState state,OrderCounter c){
        if(c.orders.isEmpty()||world.getGameTime()%20!=0)return;
        var order=c.orders.peek();
        if(!KitchenRecipes.all(c.world()).containsKey(order.recipe)){c.orders.removeFirst();c.worker=null;c.setChanged();return;}
        if(c.worker!=null&&c.world().getEntity(c.worker) instanceof TamedRat r&&c.ready(r,order)&&r.kitchen.at(pos))return;
        c.worker=null;
        var rats=world.getEntitiesOfClass(TamedRat.class,new AABB(pos).inflate(16),r->c.ready(r,order)&&r.kitchen.idle());
        if(rats.isEmpty()){c.status=Component.translatable("brick.rats.await_cook");return;}
        var r=Collections.min(rats,Comparator.comparingDouble(x->x.distanceToSqr(pos.getCenter())));
        c.worker=r.getUUID();r.kitchen.assign(pos);c.status=Component.translatable("brick.rats.working");c.setChanged();
    }
    private boolean ready(TamedRat r,Order o){return r.isAlive()&&r.isBrick()&&o.customer.equals(r.getOwnerUUID())&&r.getCommand()==RatCommand.COOK&&r.distanceToSqr(worldPosition.getCenter())<32*32;}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);ListTag items=new ListTag();
        for(var o:orders){var q=new CompoundTag();q.putUUID("Ticket",o.ticket);q.putUUID("Owner",o.customer);q.putString("Recipe",o.recipe.toString());items.add(q);}
        tag.put("Orders",items);if(worker!=null)tag.putUUID("Worker",worker);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);orders.clear();for(var entry:tag.getList("Orders",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)entry;var id=ResourceLocation.tryParse(t.getString("Recipe"));
            if(t.hasUUID("Owner")&&id!=null&&orders.size()<64)orders.add(new Order(t.hasUUID("Ticket")?t.getUUID("Ticket"):UUID.randomUUID(),t.getUUID("Owner"),id));
        }
        worker=tag.hasUUID("Worker")?tag.getUUID("Worker"):null;refreshed=Long.MIN_VALUE;
    }
}
