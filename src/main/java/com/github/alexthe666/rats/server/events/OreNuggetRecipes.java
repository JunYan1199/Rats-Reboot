package com.github.alexthe666.rats.server.events;

import com.github.alexthe666.rats.compat.create.CreateCompat;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "rats")
public class OreNuggetRecipes {
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        install(event.getServer());
    }

    @SubscribeEvent
    public static void synced(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) install(event.getPlayerList().getServer());
    }

    public static void install(MinecraftServer server) {
        var manager = server.getRecipeManager();
        Map<ResourceLocation, Recipe<?>> all = new LinkedHashMap<>();
        for (var recipe : manager.getRecipes()) {
            var id = recipe.getId();
            if (id.getNamespace().equals("rats") && id.getPath().startsWith("ore_nugget_smelting/")) continue;
            all.put(id, recipe);
        }
        for (var recipe : java.util.List.copyOf(all.values())) {
            if (recipe.getType() != RecipeType.SMELTING && recipe.getType() != RecipeType.BLASTING) continue;
            var result = recipe.getResultItem(server.registryAccess());
            if (result.isEmpty()) continue;
            addNuggetRecipe(all, result);
            if (result.getCount() > 1) addNuggetRecipe(all, result.copyWithCount(1));
        }
        manager.replaceRecipes(all.values());
        CreateCompat.invalidateRecipeCaches();
    }

    private static void addNuggetRecipe(Map<ResourceLocation, Recipe<?>> recipes, ItemStack result) {
        var item = BuiltInRegistries.ITEM.getKey(result.getItem());
        String suffix = result.getCount() == 1 ? "" : "/" + result.getCount();
        var id = new ResourceLocation("rats", "ore_nugget_smelting/" + item.getNamespace() + "/" + item.getPath() + suffix);
        var input = StrictNBTIngredient.of(OreRatNuggetItem.saveResourceToNugget(result));
        recipes.put(id, new SmeltingRecipe(id, "rats_ore_nuggets", CookingBookCategory.MISC, input, result.copy(), 0.7F, 200));
    }
}
