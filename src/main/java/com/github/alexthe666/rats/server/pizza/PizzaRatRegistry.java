package com.github.alexthe666.rats.server.pizza;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 纽约披萨鼠（阶段 9）自己的注册表：鼠头头、披萨片投射物、披萨片、鼠头头刷怪蛋 */
public final class PizzaRatRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RatsMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RatsMod.MODID);

    public static final RegistryObject<EntityType<RatBoss>> RAT_BOSS = ENTITIES.register("rat_boss",
            () -> EntityType.Builder.<RatBoss>of(RatBoss::new, MobCategory.MONSTER).sized(0.98F, 0.98F).clientTrackingRange(10).build(RatsMod.MODID + ":rat_boss"));
    public static final RegistryObject<EntityType<PizzaSliceShot>> PIZZA_SLICE_SHOT = ENTITIES.register("pizza_slice_shot",
            () -> EntityType.Builder.<PizzaSliceShot>of(PizzaSliceShot::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(6).updateInterval(10).build(RatsMod.MODID + ":pizza_slice_shot"));

    public static final RegistryObject<EntityType<FoodThief>> FOOD_THIEF = ENTITIES.register("food_thief",
            () -> EntityType.Builder.<FoodThief>of(FoodThief::new, MobCategory.MISC).sized(0.49F, 0.49F).clientTrackingRange(10).build(RatsMod.MODID + ":food_thief"));
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RatsMod.MODID);
    public static final RegistryObject<net.minecraft.world.level.block.Block> RAT_HOLE = BLOCKS.register("pizza_rat_hole", RatHoleBlock::new);

    public static final RegistryObject<Item> PIZZA_SLICE = ITEMS.register("pizza_slice",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationMod(0.7F).build())));
    public static final RegistryObject<Item> RAT_BOSS_SPAWN_EGG = ITEMS.register("rat_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(RAT_BOSS, 0x4A5058, 0xC62A2A, new Item.Properties()));

    private PizzaRatRegistry() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(RAT_BOSS.get(), RatBoss.createBossAttributes().build());
            e.put(FOOD_THIEF.get(), FoodThief.createThiefAttributes().build());
        });
    }
}
