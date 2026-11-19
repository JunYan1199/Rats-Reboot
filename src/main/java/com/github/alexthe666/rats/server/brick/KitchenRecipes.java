package com.github.alexthe666.rats.server.brick;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.*;

public final class KitchenRecipes {
    private static final Logger LOG=LogUtils.getLogger();
    public enum Station { POT, BOARD, STOVE, MONSTER_POT, COOLER, SPRING, TANGYUAN }
    public record Yield(ItemStack stack,float chance){}
    public record Dish(ResourceLocation id,Station station,List<Ingredient> inputs,Ingredient tool,List<Yield> yields,int ticks){
        public ItemStack display(){return yields.get(0).stack();}
    }
    private static final TagKey<net.minecraft.world.level.block.Block> HEAT=TagKey.create(Registries.BLOCK,new ResourceLocation("farmersdelight","heat_sources"));
    // Explicit workstations for each installed add-on; ordinary Delight recipes share the pot and board serializers.
    private static final Map<String,Station> stations=new HashMap<>();
    static{
        add("farmersdelight",Station.POT,"cooking_pot");add("farmersdelight",Station.BOARD,"cutting_board");add("farmersdelight",Station.STOVE,"stove","skillet");
        add("ends_delight",Station.STOVE,"end_stove");
        add("dungeonsdelight",Station.MONSTER_POT,"monster_pot");add("dungeonsdelight",Station.STOVE,"dungeon_stove");
        add("goetydelight",Station.POT,"cursed_ingot_pot","black_iron_cooking_pot","apocalyptium_cooking_pot");add("goetydelight",Station.STOVE,"shade_stove","night_stove");
        add("immortalers_delight",Station.COOLER,"enchantal_cooler");add("immortalers_delight",Station.SPRING,"hot_spring_block");add("immortalers_delight",Station.TANGYUAN,"unfinished_tangyuan");
        add("immortalers_delight",Station.STOVE,"ancient_stove","exposed_ancient_stove","weathered_ancient_stove","oxidized_ancient_stove","waxed_ancient_stove","waxed_exposed_ancient_stove","waxed_weathered_ancient_stove","waxed_oxidized_ancient_stove");
        add("mynethersdelight",Station.POT,"blackstone_cooking_pot");add("mynethersdelight",Station.STOVE,"nether_stove");
        add("twilightdelight",Station.POT,"fiery_cooking_pot");add("twilightdelight",Station.STOVE,"maze_stove");
    }
    private static void add(String mod,Station station,String... paths){
        if(ModList.get().isLoaded(mod))for(String path:paths)stations.put(mod+":"+path,station);
    }
    public static boolean isWorkstation(BlockState state){return stations.containsKey(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());}
    private static boolean lit(BlockState state){return !state.hasProperty(BlockStateProperties.LIT)||state.getValue(BlockStateProperties.LIT);}
    public static Station station(ServerLevel w,BlockPos p){
        BlockState state=w.getBlockState(p);
        String id=ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        Station s=stations.get(id);
        if(s==null)return null;
        if((s==Station.POT||s==Station.MONSTER_POT||id.endsWith(":skillet"))){
            BlockState below=w.getBlockState(p.below());
            var conductor=TagKey.create(Registries.BLOCK,new ResourceLocation("farmersdelight","heat_conductors"));
            if(below.is(conductor))below=w.getBlockState(p.below(2));
            if(!lit(below)||(!below.is(HEAT)&&stations.get(ForgeRegistries.BLOCKS.getKey(below.getBlock()).toString())!=Station.STOVE))return null;
        }
        if(s==Station.STOVE&&!lit(state))return null;
        return s;
    }
    private static final Map<net.minecraft.world.item.crafting.RecipeManager,Map<ResourceLocation,Dish>> CACHE=new WeakHashMap<>();
    public static void invalidate(){CACHE.clear();}
    public static Map<ResourceLocation,Dish> all(ServerLevel level){return CACHE.computeIfAbsent(level.getRecipeManager(),key->read(level));}
    private static Map<ResourceLocation,Dish> read(ServerLevel w){
        var dishes=new LinkedHashMap<ResourceLocation,Dish>();
        for(var recipe:w.getRecipeManager().getRecipes()){
            String type=ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer()).toString();
            Station kind=switch(type){
                case "farmersdelight:cooking"->Station.POT;
                case "farmersdelight:cutting"->Station.BOARD;
                case "minecraft:campfire_cooking"->Station.STOVE;
                case "dungeonsdelight:monster_cooking"->Station.MONSTER_POT;
                case "immortalers_delight:enchantal_cooler"->Station.COOLER;
                case "immortalers_delight:hot_spring"->Station.SPRING;
                case "immortalers_delight:tangyuan"->Station.TANGYUAN;
                default->null;
            };
            if(kind==null)continue;
            ItemStack result=recipe.getResultItem(w.registryAccess());
            if(result.isEmpty())continue;
            ResourceLocation rid=recipe.getId();
            try{
                List<Ingredient> inputs=new ArrayList<>(recipe.getIngredients());
                Ingredient tool=Ingredient.EMPTY;List<Yield> yields=new ArrayList<>();
                int ticks=recipe instanceof AbstractCookingRecipe cook?cook.getCookingTime():100;
                JsonObject json=null;
                if(kind!=Station.STOVE){
                    var path=new ResourceLocation(rid.getNamespace(),"recipes/"+rid.getPath()+".json");
                    var file=w.getServer().getResourceManager().getResource(path);
                    if(file.isPresent())try(var reader=file.get().openAsReader()){json=JsonParser.parseReader(reader).getAsJsonObject();}
                }
                if(json!=null&&json.has("recipes"))json=null;
                if(json!=null){
                    if(inputs.isEmpty() && json.has("ingredients")){for(var e:json.getAsJsonArray("ingredients"))inputs.add(Ingredient.fromJson(e));}
                    if(json.has("tool"))tool=Ingredient.fromJson(json.get("tool"));
                    if(json.has("cookingtime"))ticks=json.get("cookingtime").getAsInt();
                    if(json.has("result")&&json.get("result").isJsonArray())for(var el:json.getAsJsonArray("result")){
                        var yield=el.getAsJsonObject();
                        yields.add(new Yield(ShapedRecipe.itemStackFromJson(yield),yield.has("chance")?yield.get("chance").getAsFloat():1F));
                    }
                }
                if(kind==Station.BOARD){
                    var liveTool=property(recipe,"getTool");
                    if(liveTool instanceof Ingredient t)tool=t;
                    if(property(recipe,"getRollableResults") instanceof Collection<?> rollable){
                        yields.clear();for(var roll:rollable)yields.add(new Yield(((ItemStack)property(roll,"getStack")).copy(),((Number)property(roll,"getChance")).floatValue()));
                    }
                }
                if(kind==Station.TANGYUAN && property(recipe,"getTool") instanceof Ingredient t)tool=t;
                if(property(recipe,"getCookTime") instanceof Number n)ticks=n.intValue();
                if(yields.isEmpty())yields.add(new Yield(result.copy(),1));
                // Food blocks/feasts and drinks can be edible only after placement or use.
                if(!food(result))continue;
                if(kind==Station.BOARD&&tool.isEmpty())continue;
                if(inputs.isEmpty()||inputs.stream().anyMatch(Ingredient::isEmpty))continue;
                Object nativeContainer=property(recipe,"getOutputContainer");
                if(nativeContainer==null)nativeContainer=property(recipe,"getContainer");
                if(nativeContainer instanceof ItemStack bowl){
                    if(!bowl.isEmpty())inputs.add(Ingredient.of(bowl));
                }else if(json!=null&&json.has("container"))inputs.add(Ingredient.fromJson(json.get("container")));
                dishes.put(rid,new Dish(rid,kind,List.copyOf(inputs),tool,List.copyOf(yields),Math.max(1,ticks)));
            }catch(Exception e){LOG.debug("Cannot read kitchen recipe {}",rid,e);}
        }
        return dishes;
    }
    private static Object property(Object recipe,String method)throws ReflectiveOperationException{
        try{return recipe.getClass().getMethod(method).invoke(recipe);}catch(NoSuchMethodException missing){return null;}
    }
    private static boolean food(ItemStack stack){
        if(stack.isEdible()||stack.getUseAnimation()==UseAnim.DRINK)return true;
        String path=ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
        return stack.getItem() instanceof BlockItem && (path.contains("feast")||path.contains("cake")||path.contains("pie")||path.contains("platter")||path.contains("roast")||path.contains("pizza"));
    }
}
