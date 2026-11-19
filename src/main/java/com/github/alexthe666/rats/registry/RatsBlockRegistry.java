package com.github.alexthe666.rats.registry;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.*;
import com.github.alexthe666.rats.server.items.RatsBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class RatsBlockRegistry {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RatsMod.MODID);

	public static final RegistryObject<Block> BLOCK_OF_CHEESE = register("block_of_cheese", () -> new Block(Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.SLIME_BLOCK).strength(0.6F, 0.0F)));
	public static final RegistryObject<Block> MILK_CAULDRON = register("cauldron_milk", () -> new MilkCauldronBlock(Block.Properties.copy(Blocks.CAULDRON)));
	public static final RegistryObject<Block> CHEESE_CAULDRON = register("cauldron_cheese", () -> new CheeseCauldronBlock(Block.Properties.copy(Blocks.CAULDRON), RatsBlockRegistry.BLOCK_OF_CHEESE, RatsCauldronRegistry.CHEESE));
	public static final RegistryObject<Block> BLUE_CHEESE_CAULDRON = register("cauldron_blue_cheese", () -> new CheeseCauldronBlock(Block.Properties.copy(Blocks.CAULDRON), RatsBlockRegistry.BLOCK_OF_BLUE_CHEESE, RatsCauldronRegistry.BLUE_CHEESE));
	public static final RegistryObject<Block> NETHER_CHEESE_CAULDRON = register("cauldron_nether_cheese", () -> new CheeseCauldronBlock(Block.Properties.copy(Blocks.CAULDRON), RatsBlockRegistry.BLOCK_OF_NETHER_CHEESE, RatsCauldronRegistry.NETHER_CHEESE));
	public static final RegistryObject<Block> RAT_HOLE = BLOCKS.register("rat_hole", () -> new RatHoleBlock(Block.Properties.of().sound(SoundType.WOOD).noOcclusion().dynamicShape().strength(1.3F, 0.0F)));
	public static final RegistryObject<Block> RAT_CAGE = register("rat_cage", () -> new RatCageBlock(Block.Properties.of().sound(SoundType.METAL).noOcclusion().strength(2.0F, 0.0F)));
	public static final RegistryObject<Block> RAT_CAGE_DECORATED = BLOCKS.register("rat_cage_decorated", () -> new RatCageDecoratedBlock(Block.Properties.copy(RAT_CAGE.get())));
	public static final RegistryObject<Block> RAT_CAGE_BREEDING_LANTERN = BLOCKS.register("rat_cage_breeding_lantern", () -> new RatCageBreedingLanternBlock(Block.Properties.copy(RAT_CAGE.get())));
	public static final RegistryObject<Block> RAT_CAGE_WHEEL = BLOCKS.register("rat_cage_wheel", () -> new RatCageWheelBlock(Block.Properties.copy(RAT_CAGE.get())));
	public static final RegistryObject<Block> FISH_BARREL = register("fish_barrel", () -> new Block(BlockBehaviour.Properties.of().strength(2.0F, 10.0F).sound(SoundType.WOOD)));
	public static final RegistryObject<Block> ORDER_COUNTER=register("order_counter",com.github.alexthe666.rats.server.brick.OrderCounterBlock::new);
    public static final RegistryObject<Block> RAT_CRAFTING_TABLE = register("rat_crafting_table", () -> new RatCraftingTableBlock(Block.Properties.of().sound(SoundType.SLIME_BLOCK).strength(2.0F, 0.0F)));
	public static final RegistryObject<Block> AUTO_CURDLER = register("auto_curdler", () -> new AutoCurdlerBlock(Block.Properties.of().sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().strength(2.0F, 0.0F)));
	public static final RegistryObject<Block> RAT_PRESS = BLOCKS.register("rat_press", PattyTrapBlock::new);
	public static final RegistryObject<Block> RAT_TRAP = register("rat_trap", () -> new RatTrapBlock(Block.Properties.of().sound(SoundType.WOOD).noCollission().strength(1.0F, 0.0F)));
	public static final RegistryObject<Block> RAT_TUBE_COLOR = BLOCKS.register("rat_tube", () -> new RatTubeBlock(Block.Properties.of().sound(SoundType.WOOD).strength(0.9F, 0.0F)));
	public static final RegistryObject<Block> RAT_UPGRADE_BLOCK = register("rat_upgrade_block", () -> new RatUpgradeBlock(BlockBehaviour.Properties.of().sound(SoundType.SLIME_BLOCK).strength(0.6F, 0.0F)));
	public static final RegistryObject<Block> DYE_SPONGE = register("dye_sponge", () -> new DyeSpongeBlock(Block.Properties.of().strength(0.6F, 0.0F).sound(SoundType.CROP)));
	public static final RegistryObject<Block> GARBAGE_PILE = register("garbage_pile", () -> new GarbageBlock(Block.Properties.of().sound(SoundType.GRAVEL).strength(0.7F, 1.0F).randomTicks(), 1.0F));
	public static final RegistryObject<Block> CURSED_GARBAGE = register("cursed_garbage", () -> new CursedGarbageBlock(Block.Properties.copy(GARBAGE_PILE.get())));
	public static final RegistryObject<Block> COMPRESSED_GARBAGE = register("compressed_garbage", () -> new GarbageBlock(Block.Properties.copy(GARBAGE_PILE.get()), 2.0F));
	public static final RegistryObject<Block> PURIFIED_GARBAGE = register("purified_garbage", () -> new PurifiedGarbageBlock(Block.Properties.copy(GARBAGE_PILE.get())));
	public static final RegistryObject<Block> PIED_GARBAGE = register("pied_garbage", () -> new PiedGarbageBlock(Block.Properties.copy(GARBAGE_PILE.get())));
	public static final RegistryObject<Block> MARBLED_CHEESE_RAW = register("marbled_cheese_raw", () -> new Block(Block.Properties.of().requiresCorrectToolForDrops().strength(2.0F, 10.0F).sound(SoundType.STONE)));
	public static final RegistryObject<Block> JACK_O_RATERN = register("jack_o_ratern", () -> new SetupHorizontalBlock(Block.Properties.of().sound(SoundType.WOOD).strength(1.0F, 0).lightLevel(value -> 15)));
	public static final RegistryObject<Block> BLOCK_OF_BLUE_CHEESE = register("block_of_blue_cheese", () -> new Block(Block.Properties.of().strength(0.6F, 0.0F).sound(SoundType.SLIME_BLOCK)));
	public static final RegistryObject<Block> BLOCK_OF_NETHER_CHEESE = register("block_of_nether_cheese", () -> new Block(Block.Properties.of().strength(0.6F, 0.0F).sound(SoundType.SLIME_BLOCK)));
	public static final RegistryObject<Block> PIED_WOOL = register("pied_wool", () -> new Block(Block.Properties.of().strength(1.0F, 0.0F).sound(SoundType.WOOL)));
	public static final RegistryObject<Block> UPGRADE_COMBINER = register("upgrade_combiner", () -> new UpgradeCombinerBlock(Block.Properties.of().sound(SoundType.WOOD).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 0.0F).lightLevel(value -> 4)));
	public static final RegistryObject<Block> UPGRADE_SEPARATOR = register("upgrade_separator", () -> new UpgradeSeparatorBlock(Block.Properties.of().sound(SoundType.WOOD).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 0.0F).lightLevel(value -> 4)));
	public static final RegistryObject<Block> MANHOLE = register("manhole", () -> new TrapDoorBlock(Block.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).requiresCorrectToolForDrops().strength(10.0F).sound(SoundType.ANVIL).noOcclusion(), BlockSetType.IRON));
	public static final RegistryObject<Block> TRASH_CAN = register("trash_can", () -> new TrashCanBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().strength(2.0F, 0.0F)));
	public static final RegistryObject<Block> RAT_ATTRACTOR = register("rat_attractor", () -> new RatAttractorBlock(Block.Properties.of().sound(SoundType.LANTERN).requiresCorrectToolForDrops().noOcclusion().randomTicks().strength(1.0F, 0.0F)));
	public static final RegistryObject<Block> RAT_QUARRY = register("rat_quarry", () -> new RatQuarryBlock(Block.Properties.of().sound(SoundType.SLIME_BLOCK).strength(2.0F, 0.0F)));
	public static final RegistryObject<Block> RAT_QUARRY_PLATFORM = register("rat_quarry_platform", () -> new RatQuarryPlatformBlock(Block.Properties.of().sound(SoundType.SLIME_BLOCK).noOcclusion().strength(1.0F, 0.0F)));

	public static RegistryObject<Block> register(String name, Supplier<Block> blockSupplier) {
		RegistryObject<Block> ret = BLOCKS.register(name, blockSupplier);
		RatsItemRegistry.ITEMS.register(name, () -> new RatsBlockItem(ret.get(), new Item.Properties()));
		return ret;
	}

public static final RegistryObject<Block> RAT_HOUSE = register("rat_house", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_WHITE = register("rat_house_white", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_ORANGE = register("rat_house_orange", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_MAGENTA = register("rat_house_magenta", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_LIGHT_BLUE = register("rat_house_light_blue", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_YELLOW = register("rat_house_yellow", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_LIME = register("rat_house_lime", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_PINK = register("rat_house_pink", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_GRAY = register("rat_house_gray", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_LIGHT_GRAY = register("rat_house_light_gray", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_CYAN = register("rat_house_cyan", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_PURPLE = register("rat_house_purple", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_BLUE = register("rat_house_blue", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_BROWN = register("rat_house_brown", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_GREEN = register("rat_house_green", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_RED = register("rat_house_red", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_HOUSE_BLACK = register("rat_house_black", () -> new RatHouseBlock(Block.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion()));
public static final RegistryObject<Block> RAT_CHEESE_FEEDER = register("rat_cheese_feeder", () -> new RatCheeseFeederBlock(Block.Properties.of().strength(1.5F).sound(SoundType.STONE).noOcclusion()));
public static Block[] ratHouseBlocks() { return new Block[]{RAT_HOUSE.get(),RAT_HOUSE_WHITE.get(),RAT_HOUSE_ORANGE.get(),RAT_HOUSE_MAGENTA.get(),RAT_HOUSE_LIGHT_BLUE.get(),RAT_HOUSE_YELLOW.get(),RAT_HOUSE_LIME.get(),RAT_HOUSE_PINK.get(),RAT_HOUSE_GRAY.get(),RAT_HOUSE_LIGHT_GRAY.get(),RAT_HOUSE_CYAN.get(),RAT_HOUSE_PURPLE.get(),RAT_HOUSE_BLUE.get(),RAT_HOUSE_BROWN.get(),RAT_HOUSE_GREEN.get(),RAT_HOUSE_RED.get(),RAT_HOUSE_BLACK.get()}; }
}
