package com.github.alexthe666.rats.server.brick;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import com.mojang.logging.LogUtils;

/** The compat boundary only resolves RS classes when a bound wireless terminal is present. */
final class WirelessPantry {
    private static final String API="com.refinedmods.refinedstorage.api.";
    private static boolean warned;
    static void add(ServerPlayer owner,List<Ingredient> needs,List<KitchenStock.Supply> out){
        if(!ModList.get().isLoaded("refinedstorage"))return;
        Set<Object> networks=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=0;i<owner.getInventory().getContainerSize();i++){
            ItemStack terminal=owner.getInventory().getItem(i);
            if(terminal.isEmpty())continue;
            String id=ForgeRegistries.ITEMS.getKey(terminal.getItem()).toString();
            if(!Set.of("refinedstorage:wireless_grid","refinedstorage:creative_wireless_grid","refinedstorageaddons:wireless_crafting_grid","refinedstorageaddons:creative_wireless_crafting_grid").contains(id))continue;
            try{
                Class<?> cls=Class.forName("com.refinedmods.refinedstorage.item.NetworkItem");
                if(!(boolean)cls.getMethod("isValid",ItemStack.class).invoke(null,terminal))continue;
                Object[] result={null};
                Consumer<Object> found=n->result[0]=n,ignored=n->{};
                cls.getMethod("applyNetwork",MinecraftServer.class,ItemStack.class,Consumer.class,Consumer.class)
                    .invoke(terminal.getItem(),owner.getServer(),terminal,found,ignored);
                if(result[0]!=null&&!networks.contains(result[0])&&list(owner,terminal,result[0],needs,out))networks.add(result[0]);
            }catch(ReflectiveOperationException|LinkageError e){warn(e);}
        }
    }
    private static void warn(Throwable e){if(!warned){warned=true;LogUtils.getLogger().warn("Could not access Refined Storage wireless pantry",e);}}
    private static Object invoke(String api,Object obj,String method,Class<?>[] sig,Object... args)throws ReflectiveOperationException{
        return Class.forName(API+api).getMethod(method,sig).invoke(obj,args);
    }
    private static Object get(String api,Object obj,String name)throws ReflectiveOperationException{return invoke(api,obj,name,new Class<?>[0]);}
    private static boolean list(ServerPlayer p,ItemStack terminal,Object network,List<Ingredient> needs,List<KitchenStock.Supply> out)throws ReflectiveOperationException{
        String net="network.INetwork";
        if(!(boolean)get(net,network,"canRun"))return false;
        Object security=get(net,network,"getSecurityManager");
        Class<?> perm=Class.forName(API+"network.security.Permission");
        var permission=Class.forName(API+"network.security.ISecurityManager").getMethod("hasPermission",perm,Player.class);
        if(!(boolean)permission.invoke(security,perm.getField("EXTRACT").get(null),p))return false;
        Object graph=get(net,network,"getNodeGraph");
        var transmitter=Class.forName(API+"network.IWirelessTransmitter");
        boolean inRange=false;
        for(Object entry:(Collection<?>)get("network.INetworkNodeGraph",graph,"all")){
            Object node=get("network.INetworkNodeGraphEntry",entry,"getNode");
            if(!transmitter.isInstance(node)||!(boolean)get("network.node.INetworkNode",node,"isActive")||get("network.IWirelessTransmitter",node,"getDimension")!=p.level().dimension())continue;
            int range=(int)get("network.IWirelessTransmitter",node,"getRange");
            BlockPos pos=(BlockPos)get("network.IWirelessTransmitter",node,"getOrigin");
            if(pos.closerToCenterThan(p.position(),range)){inRange=true;break;}
        }
        if(!inRange)return false;
        var energy=terminal.getCapability(ForgeCapabilities.ENERGY).resolve().orElse(null);
        var config=Class.forName("com.refinedmods.refinedstorage.RS").getField("SERVER_CONFIG").get(null);
        var wireless=config.getClass().getMethod("getWirelessGrid").invoke(config);
        boolean charged=(boolean)wireless.getClass().getMethod("getUseEnergy").invoke(wireless)&&!ForgeRegistries.ITEMS.getKey(terminal.getItem()).getPath().startsWith("creative_");
        int cost=charged?(int)wireless.getClass().getMethod("getExtractUsage").invoke(wireless):0;
        if(cost>0&&(energy==null||energy.getEnergyStored()<cost))return false;
        Class<?> act=Class.forName(API+"util.Action");
        Object sim=act.getField("SIMULATE").get(null),perform=act.getField("PERFORM").get(null);
        Method take=Class.forName(API+net).getMethod("extractItem",ItemStack.class,int.class,act);
        Method put=Class.forName(API+net).getMethod("insertItem",ItemStack.class,int.class,act);
        Object cache=get(net,network,"getItemStorageCache");
        Object list=get("storage.cache.IStorageCache",cache,"getList");
        for(Object entry:(Collection<?>)get("util.IStackList",list,"getStacks")){
            var sample=(ItemStack)get("util.StackListEntry",entry,"getStack");
            if(needs.stream().noneMatch(x->x.test(sample)))continue;
            var available=(ItemStack)take.invoke(network,sample,needs.size(),sim);if(available==null||available.isEmpty())continue;
            out.add(new KitchenStock.Supply(){
                public ItemStack available(){return available;}
                public ItemStack take(int n){
                    try{
                        if(cost>0&&energy.getEnergyStored()<cost)return ItemStack.EMPTY;
                        var extracted=(ItemStack)take.invoke(network,available,n,perform);
                        if(extracted!=null&&!extracted.isEmpty()&&cost>0)energy.extractEnergy(cost,false);
                        return extracted==null?ItemStack.EMPTY:extracted;
                    }catch(ReflectiveOperationException e){warn(e);return ItemStack.EMPTY;}
                }
                public ItemStack giveBack(ItemStack s){
                    try{if(!(boolean)permission.invoke(security,perm.getField("INSERT").get(null),p))return s;
                        var remainder=(ItemStack)put.invoke(network,s,s.getCount(),perform);return remainder==null?ItemStack.EMPTY:remainder;
                    }catch(ReflectiveOperationException e){warn(e);return s;}
                }
            });
        }
        return true;
    }
}
