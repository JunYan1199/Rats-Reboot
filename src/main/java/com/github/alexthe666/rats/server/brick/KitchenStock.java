package com.github.alexthe666.rats.server.brick;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import java.util.*;

final class KitchenStock {
    interface Supply {
        ItemStack available();
        ItemStack take(int amount);
        ItemStack giveBack(ItemStack stack);
    }
    private record Local(IItemHandler handler,int slot,ItemStack available) implements Supply {
        public ItemStack take(int count){
            if(!ItemStack.isSameItemSameTags(available,handler.getStackInSlot(slot)))return ItemStack.EMPTY;
            return handler.extractItem(slot,count,false);
        }
        public ItemStack giveBack(ItemStack stack){return ItemHandlerHelper.insertItemStacked(handler,stack,false);}
    }
    record Paid(Supply source,ItemStack item,boolean tool){}
    record Result(Component missing,List<Paid> paid){}
    static Result pay(OrderCounter counter,ServerPlayer owner,KitchenRecipes.Dish dish,BrickKitchen kitchen){
        var supplies=new ArrayList<Supply>();
        for(IItemHandler handler:counter.containers(owner)){
            for(int slot=0;slot<handler.getSlots();slot++){
                var sample=handler.extractItem(slot,Math.max(1,dish.inputs().size()+1),true);
                if(!sample.isEmpty())supplies.add(new Local(handler,slot,sample.copy()));
            }
        }
        List<Ingredient> needs=new ArrayList<>(dish.inputs());
        if(!dish.tool().isEmpty())needs.add(dish.tool());
        int[] allocation=allocate(needs,supplies);
        if(Arrays.stream(allocation).anyMatch(n->n<0)&&owner!=null){
            WirelessPantry.add(owner,needs,supplies);
            allocation=allocate(needs,supplies);
        }
        for(int i=0;i<allocation.length;i++)if(allocation[i]<0){
            var options=needs.get(i).getItems();
            return new Result(Component.translatable("brick.rats.missing",options.length==0?"?":options[0].getHoverName()),List.of());
        }
        List<Paid> taken=new ArrayList<>();
        for(int i=0;i<allocation.length;i++){
            Supply s=supplies.get(allocation[i]);var got=s.take(1);
            if(!got.isEmpty())taken.add(new Paid(s,got,i==dish.inputs().size()));
            if(got.getCount()!=1||!needs.get(i).test(got)){
                for(int n=taken.size()-1;n>=0;n--){var p=taken.get(n);kitchen.keep(p.source.giveBack(p.item));}
                var options=needs.get(i).getItems();
                return new Result(Component.translatable("brick.rats.missing",options.length==0?"?":options[0].getHoverName()),List.of());
            }
        }
        return new Result(null,taken);
    }
    private static int[] allocate(List<Ingredient> needs,List<Supply> supplies){
        int[] assigned=new int[needs.size()];Arrays.fill(assigned,-1);
        for(int i=0;i<needs.size();i++)match(i,needs,supplies,assigned,new boolean[needs.size()],new boolean[supplies.size()]);
        return assigned;
    }
    private static boolean match(int idx,List<Ingredient> needs,List<Supply> supplies,int[] assigned,boolean[] visited,boolean[] seen){
        if(visited[idx])return false;visited[idx]=true;
        for(int s=0;s<supplies.size();s++){
            if(seen[s]||!needs.get(idx).test(supplies.get(s).available()))continue;
            seen[s]=true;int used=0;
            for(int v:assigned)if(v==s)used++;
            if(used<supplies.get(s).available().getCount()){assigned[idx]=s;return true;}
            for(int other=0;other<assigned.length;other++)if(assigned[other]==s&&match(other,needs,supplies,assigned,visited,seen)){
                assigned[idx]=s;return true;
            }
        }
        return false;
    }
}
