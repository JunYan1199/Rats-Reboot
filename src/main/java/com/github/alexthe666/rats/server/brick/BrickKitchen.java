package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import java.util.*;

public final class BrickKitchen {
    private final TamedRat rat;
    private BlockPos counter,station,output;
    private String kitchenDim="",outputDim="";
    private Direction outputSide=Direction.UP;
    private int progress,walkTicks;
    private long retry,lastMessage;
    private Component lastStatus=Component.empty();
    private final List<ItemStack> cargo=new ArrayList<>();
    private final List<ItemStack> ingredients=new ArrayList<>();
    private UUID reservedFor;
    private int toolIndex=-1;
    private void refund(){
        if(ingredients.isEmpty())return;
        for(ItemStack item:ingredients){
            int moved=((com.github.alexthe666.rats.server.inventory.container.RatContainer)rat.getInventory()).collect(item,item.getCount(),false);
            item.shrink(moved);if(!item.isEmpty())rat.spawnAtLocation(item);
        }
        ingredients.clear();reservedFor=null;toolIndex=-1;
    }
    public BrickKitchen(TamedRat rat){this.rat=rat;}
    private ServerLevel level(){return (ServerLevel)rat.level();}
    private String dimension(){return rat.level().dimension().location().toString();}
    boolean idle(){return counter==null&&cargo.isEmpty();}
    boolean at(BlockPos pos){return pos.equals(counter)&&kitchenDim.equals(dimension());}
    void assign(BlockPos pos){counter=pos.immutable();kitchenDim=dimension();station=null;progress=walkTicks=0;retry=0;}
    public void stopWork(){refund();counter=station=null;progress=walkTicks=0;rat.setBrickCooking(false);if(!cargo.isEmpty()){store();dropCargo();}}
    void cancelOrder(){release();rat.getNavigation().stop();}
    public void bind(BlockPos pos,Direction side){output=pos.immutable();outputSide=side;outputDim=dimension();}
    private void release(){refund();counter=station=null;progress=walkTicks=0;rat.setBrickCooking(false);}
    private ServerPlayer owner(){return rat.getOwnerUUID()==null?null:level().getServer().getPlayerList().getPlayer(rat.getOwnerUUID());}
    private void report(Component message){
        long now=level().getGameTime();
        if(!message.equals(lastStatus)||now-lastMessage>=400){
            var player=owner();if(player!=null)player.displayClientMessage(message,true);
            lastMessage=now;lastStatus=message;
        }
    }
    private boolean approach(BlockPos pos){
        Vec3 point=pos.getCenter();
        rat.getLookControl().setLookAt(point.x,point.y,point.z,35,30);
        if(rat.distanceToSqr(point)<6.25){
            var hit=level().clip(new ClipContext(rat.getEyePosition(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,rat));
            if(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(pos)){
                rat.getNavigation().stop();walkTicks=0;return true;
            }
        }
        if(walkTicks++%20==0){
            var path=rat.getNavigation().createPath(pos,1);
            if(path!=null)rat.getNavigation().moveTo(path,1.15);
        }
        return false;
    }
    public void tick(){
        if(rat.level().isClientSide())return;
        long now=level().getGameTime();
        if(now<retry)return;
        if(!cargo.isEmpty()){deliver();return;}
        if(counter==null)return;
        if(!dimension().equals(kitchenDim)||!level().hasChunkAt(counter)||!(level().getBlockEntity(counter) instanceof OrderCounter c)||!c.assigned(rat)){release();return;}
        var order=c.orders.peek();
        if(reservedFor!=null&&!reservedFor.equals(order.ticket())){refund();progress=0;}
        var dish=KitchenRecipes.all(level()).get(order.recipe());
        if(dish==null){release();return;}
        if(station==null||KitchenRecipes.station(level(),station)!=dish.station()){
            station=c.stations(false).getOrDefault(dish.station(),List.of()).stream().filter(p->KitchenRecipes.station(level(),p)==dish.station())
                .min(Comparator.comparingDouble(p->p.distSqr(rat.blockPosition()))).orElse(null);
            progress=walkTicks=0;rat.setBrickCooking(false);
            if(station==null){c.status=Component.translatable("brick.rats.no_station");report(c.status);retry=now+60;return;}
        }
        // A workstation in the counter's kitchen remains usable behind counters, walls or on shelves.
        if(rat.distanceToSqr(station.getCenter())>6.25 && walkTicks<30 && progress==0){
            if(!approach(station))return;
        }
        rat.getNavigation().stop();
        var target=station.getCenter();
        rat.getLookControl().setLookAt(target.x,target.y,target.z,35,30);
        if(reservedFor==null){
            var result=KitchenStock.pay(c,owner(),dish,this);
            if(result.missing()!=null){
                c.status=result.missing();report(c.status);rat.setBrickCooking(false);retry=now+100;return;
            }
            for(var paid:result.paid()){
                if(paid.tool())toolIndex=ingredients.size();
                ingredients.add(paid.item());
            }
            reservedFor=order.ticket();progress=0;
        }
        rat.setBrickCooking(true);
        c.status=Component.translatable("brick.rats.preparing",dish.display().getHoverName());
        if(++progress<Math.ceil(dish.ticks()/Math.max(0.5,rat.getRatHungerEfficiency())))return;
        for(int i=0;i<ingredients.size();i++){
            var item=ingredients.get(i);
            ItemStack remainder=ItemStack.EMPTY;
            if(i==toolIndex){
                if(item.isDamageableItem())item.hurtAndBreak(1,rat,e->{});
                remainder=item;
            }else if(item.hasCraftingRemainingItem())remainder=item.getCraftingRemainingItem();
            if(!remainder.isEmpty()){
                for(var storage:c.containers(owner())){
                    remainder=ItemHandlerHelper.insertItemStacked(storage,remainder,false);
                    if(remainder.isEmpty())break;
                }
                keep(remainder);
            }
        }
        ingredients.clear();reservedFor=null;toolIndex=-1;
        for(var yield:dish.yields())if(yield.chance()>=1||rat.getRandom().nextFloat()<yield.chance())keep(yield.stack().copy());
        c.finish(rat);release();
        if(!cargo.isEmpty())deliver();
    }
    void keep(ItemStack stack){
        if(stack==null||stack.isEmpty())return;
        cargo.add(stack.copy());
    }
    private void store(){
        for(var stack:cargo){
            int inserted=((com.github.alexthe666.rats.server.inventory.container.RatContainer)rat.getInventory()).collect(stack,stack.getCount(),false);
            stack.shrink(inserted);
        }
        cargo.removeIf(ItemStack::isEmpty);
    }
    private void deliver(){
        if(output!=null&&dimension().equals(outputDim)&&level().hasChunkAt(output)&&rat.distanceToSqr(output.getCenter())<64*64){
            var be=level().getBlockEntity(output);
            var handler=be==null?null:be.getCapability(ForgeCapabilities.ITEM_HANDLER,outputSide).resolve().orElse(null);
            if(handler!=null){
                if(!approach(output)&&walkTicks<200)return;
                if(walkTicks==0){
                    // 纽约披萨鼠：出餐时约 5% 被野鼠半路截胡（只有玩家亲手打到偷食鼠才算追回）
                    if(!cargo.isEmpty()&&rat.getRandom().nextFloat()<com.github.alexthe666.rats.server.pizza.PizzaRatEvents.INTERCEPT_CHANCE){
                        com.github.alexthe666.rats.server.pizza.PizzaRatEvents.intercept(rat,cargo.remove(0));
                        if(cargo.isEmpty())return;
                    }
                    for(int i=0;i<cargo.size();i++)cargo.set(i,ItemHandlerHelper.insertItemStacked(handler,cargo.get(i),false));
                    cargo.removeIf(ItemStack::isEmpty);
                    if(cargo.isEmpty())return;
                }
            }
        }
        report(Component.translatable(output==null?"brick.rats.no_output":"brick.rats.output_blocked"));store();walkTicks=0;retry=level().getGameTime()+60;
    }
    public void dropCargo(){for(var s:ingredients)rat.spawnAtLocation(s);ingredients.clear();reservedFor=null;for(var stack:cargo)rat.spawnAtLocation(stack);cargo.clear();}
    public void save(CompoundTag tag){
        var data=new CompoundTag();
        if(counter!=null){data.putLong("Counter",counter.asLong());data.putString("Dimension",kitchenDim);data.putInt("Progress",progress);if(station!=null)data.putLong("Station",station.asLong());}
        if(output!=null){data.putLong("Output",output.asLong());data.putString("OutputDimension",outputDim);data.putInt("Face",outputSide.ordinal());}
        if(reservedFor!=null){data.putUUID("ReservedFor",reservedFor);data.putInt("Tool",toolIndex);
            ListTag reserved=new ListTag();for(var s:ingredients)reserved.add(s.save(new CompoundTag()));data.put("Ingredients",reserved);}
        var list=new ListTag();for(var stack:cargo)list.add(stack.save(new CompoundTag()));if(!list.isEmpty())data.put("Cargo",list);
        if(!data.isEmpty())tag.put("BrickKitchen",data);
    }
    public void load(CompoundTag tag){
        var data=tag.getCompound("BrickKitchen");
        counter=data.contains("Counter")?BlockPos.of(data.getLong("Counter")):null;kitchenDim=data.getString("Dimension");
        station=data.contains("Station")?BlockPos.of(data.getLong("Station")):null;progress=Math.max(0,data.getInt("Progress"));
        output=data.contains("Output")?BlockPos.of(data.getLong("Output")):null;outputDim=data.getString("OutputDimension");outputSide=Direction.from3DDataValue(data.getInt("Face"));
        ingredients.clear();reservedFor=data.hasUUID("ReservedFor")?data.getUUID("ReservedFor"):null;toolIndex=data.getInt("Tool");
        for(var e:data.getList("Ingredients",Tag.TAG_COMPOUND))ingredients.add(ItemStack.of((CompoundTag)e));
        cargo.clear();for(var e:data.getList("Cargo",Tag.TAG_COMPOUND))keep(ItemStack.of((CompoundTag)e));
    }
}
