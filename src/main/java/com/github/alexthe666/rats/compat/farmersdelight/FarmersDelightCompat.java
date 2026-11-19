package com.github.alexthe666.rats.compat.farmersdelight;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatInventoryAccess;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import com.github.alexthe666.rats.server.misc.RebornCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;
import vectorwing.farmersdelight.common.block.SkilletBlock;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.block.entity.StoveBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

public final class FarmersDelightCompat {
    private static final int COOKING_POT_INPUT_SLOTS = 6;

    private FarmersDelightCompat() {
        throw new IllegalStateException("\u0421\u0435\u0440\u0432\u0438\u0441 \u0441\u043e\u0432\u043c\u0435\u0441\u0442\u0438\u043c\u043e\u0441\u0442\u0438 Farmer's Delight \u043d\u0435 \u0441\u043e\u0437\u0434\u0430\u0451\u0442\u0441\u044f \u043a\u0430\u043a \u043e\u0431\u044a\u0435\u043a\u0442");
    }

    public static boolean isSupportedWorkBlock(BlockEntity blockEntity) {
        try {
            return blockEntity instanceof StoveBlockEntity || blockEntity instanceof CookingPotBlockEntity || blockEntity instanceof CuttingBoardBlockEntity || blockEntity instanceof SkilletBlockEntity;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0447\u0438\u0439 \u0431\u043b\u043e\u043a Farmer's Delight {}", blockEntity, exception);
            throw exception;
        }
    }

    public static boolean tryRunChefCycle(TamedRat rat, BlockPos workPos, BlockPos pickupPos, BlockPos depositPos) {
        try {
            BlockEntity blockEntity = rat.level().getBlockEntity(workPos);
            if (blockEntity instanceof StoveBlockEntity) {
                StoveBlockEntity stove = (StoveBlockEntity)blockEntity;
                return FarmersDelightCompat.tryRunStoveCycle(rat, stove, pickupPos, depositPos);
            }
            if (blockEntity instanceof SkilletBlockEntity) {
                SkilletBlockEntity skillet = (SkilletBlockEntity)blockEntity;
                return FarmersDelightCompat.tryRunSkilletCycle(rat, skillet, pickupPos, depositPos);
            }
            if (blockEntity instanceof CookingPotBlockEntity) {
                CookingPotBlockEntity pot = (CookingPotBlockEntity)blockEntity;
                return FarmersDelightCompat.tryRunCookingPotCycle(rat, pot, pickupPos, depositPos);
            }
            if (blockEntity instanceof CuttingBoardBlockEntity) {
                CuttingBoardBlockEntity board = (CuttingBoardBlockEntity)blockEntity;
                return FarmersDelightCompat.tryRunCuttingBoardCycle(rat, board, pickupPos, depositPos);
            }
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442 \u0441 Farmer's Delight: \u0431\u043b\u043e\u043a {} \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0430\u043d", rat.getUUID(), workPos);
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u044b\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u0446\u0438\u043a\u043b Farmer's Delight \u0443 \u0431\u043b\u043e\u043a\u0430 {}", rat.getUUID(), workPos, exception);
            throw exception;
        }
    }

    private static boolean tryRunStoveCycle(TamedRat rat, StoveBlockEntity stove, BlockPos pickupPos, BlockPos depositPos) {
        try {
            FdFilter filter = FdFilter.fromRat(rat);
            if (FarmersDelightCompat.tryCollectFinishedStoveItem(rat, stove, depositPos, filter)) {
                return true;
            }
            BlockState state = rat.level().getBlockState(stove.getBlockPos());
            if (!state.hasProperty((Property)AbstractStoveBlock.LIT) || !((Boolean)state.getValue((Property)AbstractStoveBlock.LIT)).booleanValue()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043a\u043b\u0430\u0434\u0451\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043d\u0430 \u043f\u043b\u0438\u0442\u0443 FD {}: \u043f\u043b\u0438\u0442\u0430 \u043d\u0435 \u043d\u0430\u0433\u0440\u0435\u0442\u0430", rat.getUUID(), stove.getBlockPos());
                return false;
            }
            if (stove.isFull()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043a\u043b\u0430\u0434\u0451\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043d\u0430 \u043f\u043b\u0438\u0442\u0443 FD {}: \u0432\u0441\u0435 \u043c\u0435\u0441\u0442\u0430 \u0437\u0430\u043d\u044f\u0442\u044b", rat.getUUID(), stove.getBlockPos());
                return false;
            }
            Optional<InventoryView> pickup = FarmersDelightCompat.resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "\u0437\u0430\u0431\u043e\u0440 \u0434\u043b\u044f \u043f\u043b\u0438\u0442\u044b FD");
            if (pickup.isEmpty()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u0441\u0443\u043d\u0434\u0443\u043a \u0437\u0430\u0431\u043e\u0440\u0430 \u0434\u043b\u044f \u043f\u043b\u0438\u0442\u044b FD {}", rat.getUUID(), pickupPos);
                return false;
            }
            for (int slot = 0; slot < pickup.get().handler().getSlots(); ++slot) {
                ItemStack output;
                Optional recipe;
                ItemStack candidate = pickup.get().handler().getStackInSlot(slot);
                if (candidate.isEmpty() || filter.isBlacklisted(candidate) || (recipe = stove.getCookingRecipe(candidate)).isEmpty() || (output = ((AbstractCookingRecipe)((Recipe)recipe.get())).getResultItem(rat.level().registryAccess()).copy()).isEmpty() || !filter.allowsProcessing(candidate, output)) continue;
                if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, output, rat, "\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u043a\u043b\u0430\u0434\u0430 \u0434\u043b\u044f \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430 \u043f\u043b\u0438\u0442\u044b FD")) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043a\u043b\u0430\u0434\u0451\u0442 {} \u043d\u0430 \u043f\u043b\u0438\u0442\u0443 FD {}: \u0441\u043a\u043b\u0430\u0434 {} \u043d\u0435 \u043f\u0440\u0438\u043c\u0435\u0442 {}", rat.getUUID(), candidate, stove.getBlockPos(), depositPos, output);
                    return false;
                }
                ItemStack extracted = FarmersDelightCompat.extractExact(pickup.get(), slot, 1, stack -> ItemStack.isSameItemSameTags((ItemStack)stack, (ItemStack)candidate), rat, "\u0437\u0430\u0431\u043e\u0440 \u0441\u044b\u0440\u044c\u044f \u0434\u043b\u044f \u043f\u043b\u0438\u0442\u044b FD");
                if (extracted.isEmpty()) {
                    return false;
                }
                ItemStack oneItem = extracted.copyWithCount(1);
                boolean placed = stove.placeFood((Entity)rat, oneItem, ((AbstractCookingRecipe)recipe.get()).getCookingTime());
                if (!placed) {
                    FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, extracted, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u0441\u044b\u0440\u044c\u044f \u043f\u043e\u0441\u043b\u0435 \u043e\u0442\u043a\u0430\u0437\u0430 \u043f\u043b\u0438\u0442\u044b FD");
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c {} \u043d\u0430 \u043f\u043b\u0438\u0442\u0443 FD {}", rat.getUUID(), extracted, stove.getBlockPos());
                    return false;
                }
                FarmersDelightCompat.markChanged((BlockEntity)stove);
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u043b\u043e\u0436\u0438\u043b\u0430 {} \u043d\u0430 \u043f\u043b\u0438\u0442\u0443 FD {}, \u043e\u0436\u0438\u0434\u0430\u0435\u043c\u044b\u0439 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 {}", rat.getUUID(), extracted, stove.getBlockPos(), output);
                return true;
            }
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0435\u0435 \u0441\u044b\u0440\u044c\u0451 \u0434\u043b\u044f \u043f\u043b\u0438\u0442\u044b FD \u0432 {}", rat.getUUID(), pickupPos);
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u043f\u043b\u0438\u0442\u0443 FD {}", rat.getUUID(), stove.getBlockPos(), exception);
            throw exception;
        }
    }

    private static boolean tryCollectFinishedStoveItem(TamedRat rat, StoveBlockEntity stove, BlockPos depositPos, FdFilter filter) {
        try {
            ItemStackHandler items = stove.getItems();
            for (int slot = 0; slot < items.getSlots(); ++slot) {
                ItemStack stack = items.getStackInSlot(slot);
                if (stack.isEmpty() || stove.getCookingRecipe(stack).isPresent()) continue;
                if (filter.isBlacklisted(stack)) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 {} \u0441 \u043f\u043b\u0438\u0442\u044b FD {}: \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 \u0432 \u0447\u0451\u0440\u043d\u043e\u043c \u0441\u043f\u0438\u0441\u043a\u0435", rat.getUUID(), stack, stove.getBlockPos());
                    continue;
                }
                if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, stack.copyWithCount(1), rat, "\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u043a\u043b\u0430\u0434\u0430 \u0434\u043b\u044f \u0433\u043e\u0442\u043e\u0432\u043e\u0439 \u043f\u043b\u0438\u0442\u044b FD")) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 {} \u0441 \u043f\u043b\u0438\u0442\u044b FD {}: \u0441\u043a\u043b\u0430\u0434 {} \u043f\u043e\u043b\u043e\u043d", rat.getUUID(), stack, stove.getBlockPos(), depositPos);
                    return false;
                }
                ItemStack extracted = items.extractItem(slot, 1, false);
                ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, extracted.copy(), rat, "\u0441\u0434\u0430\u0447\u0430 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430 \u043f\u043b\u0438\u0442\u044b FD");
                if (!remaining.isEmpty()) {
                    ItemStack rollback = ItemHandlerHelper.insertItem((IItemHandler)items, (ItemStack)extracted, (boolean)false);
                    if (!rollback.isEmpty()) {
                        throw new IllegalStateException("\u0421\u043a\u043b\u0430\u0434 \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u043f\u043e\u0441\u043b\u0435 \u0441\u0438\u043c\u0443\u043b\u044f\u0446\u0438\u0438, \u0430 \u0432\u0435\u0440\u043d\u0443\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 \u043f\u043b\u0438\u0442\u044b \u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c: " + String.valueOf(rollback));
                    }
                    return false;
                }
                FarmersDelightCompat.markChanged((BlockEntity)stove);
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0441\u043d\u044f\u043b\u0430 {} \u0441 \u043f\u043b\u0438\u0442\u044b FD {} \u0438 \u043e\u0442\u043d\u0435\u0441\u043b\u0430 \u0432 {}", rat.getUUID(), extracted, stove.getBlockPos(), depositPos);
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0441\u043d\u044f\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 \u0441 \u043f\u043b\u0438\u0442\u044b FD {}", rat.getUUID(), stove.getBlockPos(), exception);
            throw exception;
        }
    }

    private static boolean tryRunSkilletCycle(TamedRat rat, SkilletBlockEntity skillet, BlockPos pickupPos, BlockPos depositPos) {
        try {
            FdFilter filter = FdFilter.fromRat(rat);
            if (FarmersDelightCompat.tryCollectDroppedSkilletResult(rat, skillet, depositPos, filter)) {
                return true;
            }
            BlockState state = rat.level().getBlockState(skillet.getBlockPos());
            if (state.hasProperty((Property)SkilletBlock.WATERLOGGED) && ((Boolean)state.getValue((Property)SkilletBlock.WATERLOGGED)).booleanValue()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043a\u043b\u0430\u0434\u0451\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043d\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0443 FD {}: \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0430 \u0437\u0430\u043b\u0438\u0442\u0430 \u0432\u043e\u0434\u043e\u0439", rat.getUUID(), skillet.getBlockPos());
                return false;
            }
            if (!skillet.isHeated()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043a\u043b\u0430\u0434\u0451\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043d\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0443 FD {}: \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0430 \u043d\u0435 \u043d\u0430\u0433\u0440\u0435\u0442\u0430", rat.getUUID(), skillet.getBlockPos());
                return false;
            }
            if (skillet.hasStoredStack()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0436\u0434\u0451\u0442 \u0433\u043e\u0442\u043e\u0432\u043a\u0443 \u043d\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0435 FD {}: \u0432\u043d\u0443\u0442\u0440\u0438 \u0443\u0436\u0435 \u0435\u0441\u0442\u044c {}", rat.getUUID(), skillet.getBlockPos(), skillet.getStoredStack());
                return false;
            }
            Optional<InventoryView> pickup = FarmersDelightCompat.resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "\u0437\u0430\u0431\u043e\u0440 \u0434\u043b\u044f \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD");
            if (pickup.isEmpty()) {
                return false;
            }
            for (int slot = 0; slot < pickup.get().handler().getSlots(); ++slot) {
                ItemStack output;
                ItemStack candidate = pickup.get().handler().getStackInSlot(slot);
                if (candidate.isEmpty() || filter.isBlacklisted(candidate) || (output = FarmersDelightCompat.getCampfireCookingResult(rat.level(), candidate)).isEmpty() || !filter.allowsProcessing(candidate, output)) continue;
                if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, output, rat, "\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u043a\u043b\u0430\u0434\u0430 \u0434\u043b\u044f \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD")) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043a\u043b\u0430\u0434\u0451\u0442 {} \u043d\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0443 FD {}: \u0441\u043a\u043b\u0430\u0434 {} \u043d\u0435 \u043f\u0440\u0438\u043c\u0435\u0442 {}", rat.getUUID(), candidate, skillet.getBlockPos(), depositPos, output);
                    return false;
                }
                ItemStack extracted = FarmersDelightCompat.extractExact(pickup.get(), slot, 1, stack -> ItemStack.isSameItemSameTags((ItemStack)stack, (ItemStack)candidate), rat, "\u0437\u0430\u0431\u043e\u0440 \u0441\u044b\u0440\u044c\u044f \u0434\u043b\u044f \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD");
                if (extracted.isEmpty()) {
                    return false;
                }
                ItemStack oneItem = extracted.copyWithCount(1);
                ItemStack remaining = skillet.addItemToCook(oneItem, null);
                if (!remaining.isEmpty()) {
                    FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, extracted, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u0441\u044b\u0440\u044c\u044f \u043f\u043e\u0441\u043b\u0435 \u043e\u0442\u043a\u0430\u0437\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD");
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c {} \u043d\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0443 FD {}", rat.getUUID(), extracted, skillet.getBlockPos());
                    return false;
                }
                FarmersDelightCompat.markChanged((BlockEntity)skillet);
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u043b\u043e\u0436\u0438\u043b\u0430 {} \u043d\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0443 FD {}, \u043e\u0436\u0438\u0434\u0430\u0435\u043c\u044b\u0439 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 {}", rat.getUUID(), extracted, skillet.getBlockPos(), output);
                return true;
            }
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0435\u0435 \u0441\u044b\u0440\u044c\u0451 \u0434\u043b\u044f \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD \u0432 {}", rat.getUUID(), pickupPos);
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u0443 FD {}", rat.getUUID(), skillet.getBlockPos(), exception);
            throw exception;
        }
    }

    private static boolean tryCollectDroppedSkilletResult(TamedRat rat, SkilletBlockEntity skillet, BlockPos depositPos, FdFilter filter) {
        try {
            AABB searchBox = new AABB(skillet.getBlockPos()).inflate(1.0, 0.75, 1.0);
            List<ItemEntity> items = rat.level().getEntitiesOfClass(ItemEntity.class, searchBox, entity -> entity.isAlive() && !entity.getItem().isEmpty());
            for (ItemEntity entity2 : items) {
                ItemStack stack = entity2.getItem();
                if (!FarmersDelightCompat.isCampfireCookingResult(rat.level(), stack) || !filter.allowsCollectedResult(stack)) continue;
                ItemStack single = stack.copyWithCount(1);
                if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, single, rat, "\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u043a\u043b\u0430\u0434\u0430 \u0434\u043b\u044f \u0432\u044b\u043f\u0430\u0432\u0448\u0435\u0439 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD")) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0437\u0430\u0431\u0438\u0440\u0430\u0435\u0442 {} \u0443 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD {}: \u0441\u043a\u043b\u0430\u0434 {} \u043d\u0435 \u043f\u0440\u0438\u043c\u0435\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442", rat.getUUID(), single, skillet.getBlockPos(), depositPos);
                    return false;
                }
                ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, single.copy(), rat, "\u0441\u0434\u0430\u0447\u0430 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD");
                if (!remaining.isEmpty()) {
                    throw new IllegalStateException("\u0421\u043a\u043b\u0430\u0434 \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u043f\u043e\u0441\u043b\u0435 \u0441\u0438\u043c\u0443\u043b\u044f\u0446\u0438\u0438 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD, \u043e\u0441\u0442\u0430\u0442\u043e\u043a: " + String.valueOf(remaining));
                }
                stack.shrink(1);
                if (stack.isEmpty()) {
                    entity2.discard();
                } else {
                    entity2.setItem(stack);
                }
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u0431\u0440\u0430\u043b\u0430 {} \u0443 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD {} \u0438 \u043e\u0442\u043d\u0435\u0441\u043b\u0430 \u0432 {}", rat.getUUID(), single, skillet.getBlockPos(), depositPos);
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u0431\u0440\u0430\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 \u0441\u043a\u043e\u0432\u043e\u0440\u043e\u0434\u044b FD {}", rat.getUUID(), skillet.getBlockPos(), exception);
            throw exception;
        }
    }

    private static ItemStack getCampfireCookingResult(Level level, ItemStack input) {
        try {
            Optional recipe = level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(input.copyWithCount(1)), level);
            if (recipe.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return ((CampfireCookingRecipe)((Recipe)recipe.get())).getResultItem(level.registryAccess()).copy();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0439\u0442\u0438 \u0440\u0435\u0446\u0435\u043f\u0442 \u043a\u043e\u0441\u0442\u0440\u0430 FD \u0434\u043b\u044f {}", input, exception);
            throw exception;
        }
    }

    private static boolean isCampfireCookingResult(Level level, ItemStack stack) {
        try {
            List<? extends Recipe<?>> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CAMPFIRE_COOKING);
            for (Recipe recipe : recipes) {
                ItemStack output = ((CampfireCookingRecipe)recipe).getResultItem(level.registryAccess());
                if (!ItemStack.isSameItemSameTags((ItemStack)output, (ItemStack)stack)) continue;
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 \u043a\u043e\u0441\u0442\u0440\u0430 FD {}", stack, exception);
            throw exception;
        }
    }

    private static boolean tryRunCookingPotCycle(TamedRat rat, CookingPotBlockEntity pot, BlockPos pickupPos, BlockPos depositPos) {
        try {
            FdFilter filter = FdFilter.fromRat(rat);
            if (FarmersDelightCompat.tryServeCookingPotMeal(rat, pot, pickupPos, depositPos, filter)) {
                return true;
            }
            if (!pot.isHeated()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0430\u0435\u0442 \u043a\u043e\u0442\u0451\u043b FD {}: \u043a\u043e\u0442\u0451\u043b \u043d\u0435 \u043d\u0430\u0433\u0440\u0435\u0442", rat.getUUID(), pot.getBlockPos());
                return false;
            }
            if (!FarmersDelightCompat.isCookingPotInputClear(pot)) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0436\u0434\u0451\u0442 \u043a\u043e\u0442\u0451\u043b FD {}: \u0432\u043d\u0443\u0442\u0440\u0438 \u0443\u0436\u0435 \u0435\u0441\u0442\u044c \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b \u0438\u043b\u0438 \u0431\u043b\u044e\u0434\u043e", rat.getUUID(), pot.getBlockPos());
                return false;
            }
            if (!filter.hasWhitelist()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0433\u043e\u0442\u043e\u0432\u0438\u0442 \u0432 \u043a\u043e\u0442\u043b\u0435 FD {}: \u0434\u043b\u044f \u043a\u043e\u0442\u043b\u0430 \u043d\u0443\u0436\u043d\u0430 \u0446\u0435\u043b\u044c-\u0440\u0435\u0446\u0435\u043f\u0442 \u0432 \u0431\u0435\u043b\u043e\u043c \u0441\u043f\u0438\u0441\u043a\u0435", rat.getUUID(), pot.getBlockPos());
                return false;
            }
            Optional<InventoryView> pickup = FarmersDelightCompat.resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "\u0437\u0430\u0431\u043e\u0440 \u0434\u043b\u044f \u043a\u043e\u0442\u043b\u0430 FD");
            if (pickup.isEmpty()) {
                return false;
            }
            Optional<CookingPotPlan> plan = FarmersDelightCompat.findCookingPotPlan(rat, pot, pickup.get(), depositPos, filter);
            if (plan.isEmpty()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u043f\u043e\u043b\u043d\u044b\u0439 \u043d\u0430\u0431\u043e\u0440 \u0434\u043b\u044f \u0446\u0435\u043b\u0435\u0432\u043e\u0433\u043e \u0440\u0435\u0446\u0435\u043f\u0442\u0430 \u043a\u043e\u0442\u043b\u0430 FD \u0443 {}", rat.getUUID(), pickupPos);
                return false;
            }
            List<ItemStack> extracted = FarmersDelightCompat.extractPlans(pickup.get(), plan.get().ingredients(), rat, "\u0437\u0430\u0431\u043e\u0440 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u043e\u0432 \u0434\u043b\u044f \u043a\u043e\u0442\u043b\u0430 FD");
            if (extracted.size() != plan.get().ingredients().size()) {
                throw new IllegalStateException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u0437\u0432\u043b\u0435\u0447\u044c \u043f\u043e\u043b\u043d\u044b\u0439 \u043d\u0430\u0431\u043e\u0440 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u043e\u0432 \u043a\u043e\u0442\u043b\u0430 FD \u0434\u043b\u044f " + plan.get().recipeId());
            }
            try {
                FarmersDelightCompat.insertCookingPotIngredients(pot, extracted, rat);
            }
            catch (RuntimeException exception) {
                for (ItemStack stack : extracted) {
                    FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, stack, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u043e\u0432 \u043a\u043e\u0442\u043b\u0430 FD");
                }
                throw exception;
            }
            FarmersDelightCompat.markChanged((BlockEntity)pot);
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u043b\u043e\u0436\u0438\u043b\u0430 \u0440\u0435\u0446\u0435\u043f\u0442 \u043a\u043e\u0442\u043b\u0430 FD {} \u0432 {}", rat.getUUID(), plan.get().recipeId(), pot.getBlockPos());
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u043a\u043e\u0442\u0451\u043b FD {}", rat.getUUID(), pot.getBlockPos(), exception);
            throw exception;
        }
    }

    private static boolean tryServeCookingPotMeal(TamedRat rat, CookingPotBlockEntity pot, BlockPos pickupPos, BlockPos depositPos, FdFilter filter) {
        try {
            ItemStack meal = pot.getMeal();
            if (meal.isEmpty()) {
                return false;
            }
            if (filter.isBlacklisted(meal)) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 \u0431\u043b\u044e\u0434\u043e {} \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {}: \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 \u0432 \u0447\u0451\u0440\u043d\u043e\u043c \u0441\u043f\u0438\u0441\u043a\u0435", rat.getUUID(), meal, pot.getBlockPos());
                return false;
            }
            ItemStack container = pot.getContainer().copyWithCount(1);
            if (container.isEmpty()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 \u0431\u043b\u044e\u0434\u043e {} \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {}: \u0443 \u0431\u043b\u044e\u0434\u0430 \u043d\u0435\u0442 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u0430 \u0434\u043b\u044f \u043f\u043e\u0434\u0430\u0447\u0438", rat.getUUID(), meal, pot.getBlockPos());
                return false;
            }
            if (filter.isBlacklisted(container)) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 \u0431\u043b\u044e\u0434\u043e \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {}: \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 {} \u0432 \u0447\u0451\u0440\u043d\u043e\u043c \u0441\u043f\u0438\u0441\u043a\u0435", rat.getUUID(), pot.getBlockPos(), container);
                return false;
            }
            if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, meal.copyWithCount(1), rat, "\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u043a\u043b\u0430\u0434\u0430 \u0434\u043b\u044f \u0431\u043b\u044e\u0434\u0430 \u043a\u043e\u0442\u043b\u0430 FD")) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 \u0431\u043b\u044e\u0434\u043e {} \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {}: \u0441\u043a\u043b\u0430\u0434 {} \u043d\u0435 \u043f\u0440\u0438\u043c\u0435\u0442 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442", rat.getUUID(), meal, pot.getBlockPos(), depositPos);
                return false;
            }
            Optional<InventoryView> pickup = FarmersDelightCompat.resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "\u0437\u0430\u0431\u043e\u0440 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u0430 \u0434\u043b\u044f \u043a\u043e\u0442\u043b\u0430 FD");
            if (pickup.isEmpty()) {
                return false;
            }
            Optional<InventoryPlan> containerPlan = FarmersDelightCompat.reserveSingle(pickup.get(), stack -> ItemStack.isSameItemSameTags((ItemStack)stack, (ItemStack)container), filter::allowsContainer);
            if (containerPlan.isEmpty()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 \u0431\u043b\u044e\u0434\u043e {} \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {}: \u043d\u0435\u0442 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u0430 {}", rat.getUUID(), meal, pot.getBlockPos(), container);
                return false;
            }
            ItemStack extractedContainer = FarmersDelightCompat.extractPlan(pickup.get(), containerPlan.get(), rat, "\u0437\u0430\u0431\u043e\u0440 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u0430 \u0434\u043b\u044f \u043a\u043e\u0442\u043b\u0430 FD");
            ItemStack served = pot.useHeldItemOnMeal(extractedContainer);
            if (served.isEmpty()) {
                FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, container, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u0430 \u043f\u043e\u0441\u043b\u0435 \u043e\u0442\u043a\u0430\u0437\u0430 \u043a\u043e\u0442\u043b\u0430 FD");
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0441\u043d\u044f\u0442\u044c \u043f\u043e\u0440\u0446\u0438\u044e \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {}", rat.getUUID(), pot.getBlockPos());
                return false;
            }
            ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, served.copy(), rat, "\u0441\u0434\u0430\u0447\u0430 \u0431\u043b\u044e\u0434\u0430 \u043a\u043e\u0442\u043b\u0430 FD");
            if (!remaining.isEmpty()) {
                throw new IllegalStateException("\u0421\u043a\u043b\u0430\u0434 \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u043f\u043e\u0441\u043b\u0435 \u0441\u0438\u043c\u0443\u043b\u044f\u0446\u0438\u0438 \u043f\u043e\u0434\u0430\u0447\u0438 \u043a\u043e\u0442\u043b\u0430 FD, \u043e\u0441\u0442\u0430\u0442\u043e\u043a: " + String.valueOf(remaining));
            }
            FarmersDelightCompat.markChanged((BlockEntity)pot);
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0441\u043d\u044f\u043b\u0430 {} \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {} \u0438 \u043e\u0442\u043d\u0435\u0441\u043b\u0430 \u0432 {}", rat.getUUID(), served, pot.getBlockPos(), depositPos);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0441\u043d\u044f\u0442\u044c \u0431\u043b\u044e\u0434\u043e \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 FD {}", rat.getUUID(), pot.getBlockPos(), exception);
            throw exception;
        }
    }

    private static Optional<CookingPotPlan> findCookingPotPlan(TamedRat rat, CookingPotBlockEntity pot, InventoryView pickup, BlockPos depositPos, FdFilter filter) {
        try {
            List<? extends Recipe<?>> recipes = rat.level().getRecipeManager().getAllRecipesFor((RecipeType)ModRecipeTypes.COOKING.get());
            for (Recipe holder : recipes) {
                CookingPotRecipe recipe = (CookingPotRecipe)holder;
                ItemStack result = recipe.getResultItem(rat.level().registryAccess()).copy();
                if (result.isEmpty() || !filter.isWhitelisted(result) || filter.isBlacklisted(result)) continue;
                ItemStack container = recipe.getOutputContainer().copyWithCount(1);
                if (!container.isEmpty() && !filter.allowsContainer(container)) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u0440\u043e\u043f\u0443\u0441\u043a\u0430\u0435\u0442 \u0440\u0435\u0446\u0435\u043f\u0442 \u043a\u043e\u0442\u043b\u0430 FD {}: \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 {} \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d", rat.getUUID(), holder.getId(), container);
                    continue;
                }
                List<InventoryPlan> ingredients = FarmersDelightCompat.reserveIngredients(pickup, (NonNullList<Ingredient>)recipe.getIngredients(), stack -> filter.allowsIngredientForTargetRecipe((ItemStack)stack));
                if (ingredients.size() != recipe.getIngredients().size()) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043e\u0431\u0440\u0430\u043b\u0430 \u043f\u043e\u043b\u043d\u044b\u0439 \u043d\u0430\u0431\u043e\u0440 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u043e\u0432 \u0434\u043b\u044f \u0440\u0435\u0446\u0435\u043f\u0442\u0430 \u043a\u043e\u0442\u043b\u0430 FD {}", rat.getUUID(), holder.getId());
                    continue;
                }
                if (!container.isEmpty()) {
                    if (FarmersDelightCompat.reserveSingle(pickup, stack -> ItemStack.isSameItemSameTags((ItemStack)stack, (ItemStack)container), filter::allowsContainer).isEmpty()) {
                        RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 {} \u0434\u043b\u044f \u0440\u0435\u0446\u0435\u043f\u0442\u0430 \u043a\u043e\u0442\u043b\u0430 FD {}", rat.getUUID(), container, holder.getId());
                        continue;
                    }
                }
                if (!FarmersDelightCompat.matchesCookingPotRecipe(recipe, ingredients, rat.level())) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0441\u043e\u0431\u0440\u0430\u043b\u0430 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b, \u043d\u043e \u0440\u0435\u0446\u0435\u043f\u0442 \u043a\u043e\u0442\u043b\u0430 FD {} \u043d\u0435 \u0441\u043e\u0432\u043f\u0430\u043b \u043f\u0440\u0438 \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0435", rat.getUUID(), holder.getId());
                    continue;
                }
                if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, result, rat, "\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u043a\u043b\u0430\u0434\u0430 \u0434\u043b\u044f \u0440\u0435\u0446\u0435\u043f\u0442\u0430 \u043a\u043e\u0442\u043b\u0430 FD")) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0433\u043e\u0442\u043e\u0432\u0438\u0442 \u0440\u0435\u0446\u0435\u043f\u0442 \u043a\u043e\u0442\u043b\u0430 FD {}: \u0441\u043a\u043b\u0430\u0434 {} \u043d\u0435 \u043f\u0440\u0438\u043c\u0435\u0442 {}", rat.getUUID(), holder.getId(), depositPos, result);
                    continue;
                }
                return Optional.of(new CookingPotPlan(holder.getId().toString(), ingredients));
            }
            return Optional.empty();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u0434\u043e\u0431\u0440\u0430\u0442\u044c \u0440\u0435\u0446\u0435\u043f\u0442 \u043a\u043e\u0442\u043b\u0430 FD {}", rat.getUUID(), pot.getBlockPos(), exception);
            throw exception;
        }
    }

    private static boolean tryRunCuttingBoardCycle(TamedRat rat, CuttingBoardBlockEntity board, BlockPos pickupPos, BlockPos depositPos) {
        try {
            ItemStack tool;
            if (!board.isEmpty()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442 \u0441 \u0434\u043e\u0441\u043a\u043e\u0439 FD {}: \u0434\u043e\u0441\u043a\u0430 \u0437\u0430\u043d\u044f\u0442\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u043c {}", rat.getUUID(), board.getBlockPos(), board.getStoredItem());
                return false;
            }
            FdFilter filter = FdFilter.fromRat(rat);
            Optional<InventoryView> pickup = FarmersDelightCompat.resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "\u0437\u0430\u0431\u043e\u0440 \u0434\u043b\u044f \u0434\u043e\u0441\u043a\u0438 FD");
            if (pickup.isEmpty()) {
                return false;
            }
            Optional<CuttingBoardPlan> plan = FarmersDelightCompat.findCuttingBoardPlan(rat, pickup.get(), depositPos, filter);
            if (plan.isEmpty()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u043f\u043e\u043b\u043d\u044b\u0439 \u043d\u0430\u0431\u043e\u0440 \u0434\u043b\u044f \u0434\u043e\u0441\u043a\u0438 FD \u0432 {}", rat.getUUID(), pickupPos);
                return false;
            }
            ItemStack ingredient = FarmersDelightCompat.extractPlan(pickup.get(), plan.get().ingredient(), rat, "\u0437\u0430\u0431\u043e\u0440 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u0430 \u0434\u043b\u044f \u0434\u043e\u0441\u043a\u0438 FD");
            try {
                tool = FarmersDelightCompat.extractPlan(pickup.get(), plan.get().tool(), rat, "\u0437\u0430\u0431\u043e\u0440 \u043d\u043e\u0436\u0430 \u0434\u043b\u044f \u0434\u043e\u0441\u043a\u0438 FD");
            }
            catch (RuntimeException exception) {
                FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, ingredient, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u0430 \u043f\u043e\u0441\u043b\u0435 \u043e\u0442\u043a\u0430\u0437\u0430 \u043d\u043e\u0436\u0430 FD");
                throw exception;
            }
            boolean ingredientConsumed = false;
            try {
                ItemStack leftover = board.addItem(ingredient.copy());
                if (!leftover.isEmpty()) {
                    throw new IllegalStateException("\u0414\u043e\u0441\u043a\u0430 FD \u043e\u0442\u043a\u0430\u0437\u0430\u043b\u0430 \u043f\u043e\u0441\u043b\u0435 \u0441\u0438\u043c\u0443\u043b\u044f\u0446\u0438\u0438, \u043e\u0441\u0442\u0430\u0442\u043e\u043a: " + String.valueOf(leftover));
                }
                ItemStack stored = board.removeItem();
                if (stored.isEmpty()) {
                    throw new IllegalStateException("\u0414\u043e\u0441\u043a\u0430 FD \u043d\u0435 \u0432\u0435\u0440\u043d\u0443\u043b\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0434\u043b\u044f \u043d\u0430\u0440\u0435\u0437\u043a\u0438");
                }
                ingredientConsumed = true;
                FarmersDelightCompat.damageToolOrBreak(rat, tool);
                for (ItemStack output : plan.get().outputs()) {
                    ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, output.copy(), rat, "\u0441\u0434\u0430\u0447\u0430 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430 \u0434\u043e\u0441\u043a\u0438 FD");
                    if (remaining.isEmpty()) continue;
                    throw new IllegalStateException("\u0421\u043a\u043b\u0430\u0434 \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u043f\u043e\u0441\u043b\u0435 \u0441\u0438\u043c\u0443\u043b\u044f\u0446\u0438\u0438 \u0434\u043e\u0441\u043a\u0438 FD, \u043e\u0441\u0442\u0430\u0442\u043e\u043a: " + String.valueOf(remaining));
                }
                if (!tool.isEmpty()) {
                    FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, tool, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u043d\u043e\u0436\u0430 \u043f\u043e\u0441\u043b\u0435 \u0434\u043e\u0441\u043a\u0438 FD");
                }
                FarmersDelightCompat.markChanged((BlockEntity)board);
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0430\u0440\u0435\u0437\u0430\u043b\u0430 {} \u043d\u0430 \u0434\u043e\u0441\u043a\u0435 FD {} \u043f\u043e \u0440\u0435\u0446\u0435\u043f\u0442\u0443 {}", rat.getUUID(), stored, board.getBlockPos(), plan.get().recipeId());
                return true;
            }
            catch (RuntimeException exception) {
                if (!ingredientConsumed) {
                    FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, ingredient, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u0430 \u043f\u043e\u0441\u043b\u0435 \u043e\u0448\u0438\u0431\u043a\u0438 \u0434\u043e\u0441\u043a\u0438 FD");
                }
                if (!tool.isEmpty()) {
                    FarmersDelightCompat.returnToPickupOrThrow(rat, pickupPos, tool, "\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u043d\u043e\u0436\u0430 \u043f\u043e\u0441\u043b\u0435 \u043e\u0448\u0438\u0431\u043a\u0438 \u0434\u043e\u0441\u043a\u0438 FD");
                }
                throw exception;
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u0434\u043e\u0441\u043a\u0443 FD {}", rat.getUUID(), board.getBlockPos(), exception);
            throw exception;
        }
    }

    private static Optional<CuttingBoardPlan> findCuttingBoardPlan(TamedRat rat, InventoryView pickup, BlockPos depositPos, FdFilter filter) {
        try {
            List<? extends Recipe<?>> recipes = rat.level().getRecipeManager().getAllRecipesFor((RecipeType)ModRecipeTypes.CUTTING.get());
            for (Recipe holder : recipes) {
                boolean outputTargeted;
                Optional<InventoryPlan> ingredient;
                CuttingBoardRecipe recipe = (CuttingBoardRecipe)holder;
                List<ItemStack> outputs = FarmersDelightCompat.getCuttingBoardOutputs(recipe);
                if (outputs.isEmpty()) continue;
                if (outputs.stream().anyMatch(filter::isBlacklisted) || (ingredient = FarmersDelightCompat.reserveSingle(pickup, stack -> ((Ingredient)recipe.getIngredients().get(0)).test(stack), arg_0 -> FarmersDelightCompat.lambda$findCuttingBoardPlan$7(filter, outputs.stream().anyMatch(filter::isWhitelisted), arg_0))).isEmpty()) continue;
                Optional<InventoryPlan> tool = FarmersDelightCompat.reserveSingle(pickup, stack -> recipe.getTool().test(stack), stack -> filter.allowsTool((ItemStack)stack) && !FarmersDelightCompat.samePlanItem((InventoryPlan)ingredient.get(), stack));
                if (tool.isEmpty()) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u043d\u043e\u0436 \u0438\u043b\u0438 \u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442 \u0434\u043b\u044f \u0440\u0435\u0446\u0435\u043f\u0442\u0430 \u0434\u043e\u0441\u043a\u0438 FD {}", rat.getUUID(), holder.getId());
                    continue;
                }
                if (!FarmersDelightCompat.matchesCuttingBoardRecipe(recipe, ingredient.get().stack(), tool.get().stack(), rat.level())) continue;
                if (!FarmersDelightCompat.canInsertAll(rat, depositPos, outputs, "\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u043a\u043b\u0430\u0434\u0430 \u0434\u043b\u044f \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430 \u0434\u043e\u0441\u043a\u0438 FD")) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0440\u0435\u0436\u0435\u0442 \u0440\u0435\u0446\u0435\u043f\u0442 \u0434\u043e\u0441\u043a\u0438 FD {}: \u0441\u043a\u043b\u0430\u0434 {} \u043d\u0435 \u043f\u0440\u0438\u043c\u0435\u0442 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442", rat.getUUID(), holder.getId(), depositPos);
                    continue;
                }
                return Optional.of(new CuttingBoardPlan(holder.getId().toString(), ingredient.get(), tool.get(), outputs));
            }
            return Optional.empty();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u0434\u043e\u0431\u0440\u0430\u0442\u044c \u0440\u0435\u0446\u0435\u043f\u0442 \u0434\u043e\u0441\u043a\u0438 FD", rat.getUUID(), exception);
            throw exception;
        }
    }

    private static boolean isCookingPotInputClear(CookingPotBlockEntity pot) {
        try {
            ItemStackHandler inventory = pot.getInventory();
            if (!pot.getMeal().isEmpty()) {
                return false;
            }
            for (int slot = 0; slot < 6; ++slot) {
                if (inventory.getStackInSlot(slot).isEmpty()) continue;
                return false;
            }
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0437\u0430\u043d\u044f\u0442\u043e\u0441\u0442\u044c \u043a\u043e\u0442\u043b\u0430 FD {}", pot.getBlockPos(), exception);
            throw exception;
        }
    }

    private static void insertCookingPotIngredients(CookingPotBlockEntity pot, List<ItemStack> ingredients, TamedRat rat) {
        try {
            ItemStackHandler inventory = pot.getInventory();
            for (int index = 0; index < ingredients.size(); ++index) {
                ItemStack remaining = inventory.insertItem(index, ingredients.get(index).copy(), false);
                if (remaining.isEmpty()) continue;
                throw new IllegalStateException("\u041a\u043e\u0442\u0451\u043b FD \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u0443 " + String.valueOf(ingredients.get(index)) + ", \u043e\u0441\u0442\u0430\u0442\u043e\u043a: " + String.valueOf(remaining));
            }
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u043b\u0430 {} \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u043e\u0432 \u0432 \u043a\u043e\u0442\u0451\u043b FD {}", rat.getUUID(), ingredients.size(), pot.getBlockPos());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b \u0432 \u043a\u043e\u0442\u0451\u043b FD {}", rat.getUUID(), pot.getBlockPos(), exception);
            throw exception;
        }
    }

    private static boolean matchesCookingPotRecipe(CookingPotRecipe recipe, List<InventoryPlan> ingredients, Level level) {
        try {
            ItemStackHandler handler = new ItemStackHandler(6);
            for (int index = 0; index < ingredients.size(); ++index) {
                handler.setStackInSlot(index, ingredients.get(index).stack().copy());
            }
            return recipe.matches(new RecipeWrapper(handler), level);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0435\u0446\u0435\u043f\u0442 \u043a\u043e\u0442\u043b\u0430 FD", (Throwable)exception);
            throw exception;
        }
    }

private static boolean matchesCuttingBoardRecipe(CuttingBoardRecipe recipe, ItemStack item, ItemStack tool, Level level) { return recipe.getIngredients().get(0).test(item) && recipe.getTool().test(tool); }

    private static List<ItemStack> getCuttingBoardOutputs(CuttingBoardRecipe recipe) {
        try {
            ArrayList<ItemStack> outputs = new ArrayList<ItemStack>();
            for (ItemStack stack : recipe.getResults()) {
                if (stack.isEmpty()) continue;
                outputs.add(stack.copy());
            }
            return outputs;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u044b \u0440\u0435\u0446\u0435\u043f\u0442\u0430 \u0434\u043e\u0441\u043a\u0438 FD", (Throwable)exception);
            throw exception;
        }
    }

    private static List<InventoryPlan> reserveIngredients(InventoryView inventory, NonNullList<Ingredient> ingredients, Predicate<ItemStack> allowed) {
        try {
            List<VirtualSlot> virtualSlots = FarmersDelightCompat.createVirtualSlots(inventory);
            ArrayList<InventoryPlan> plans = new ArrayList<InventoryPlan>();
            for (Ingredient ingredient : ingredients) {
                Optional<InventoryPlan> plan = FarmersDelightCompat.reserveFromVirtualSlots(inventory, virtualSlots, stack -> ingredient.test(stack), allowed);
                if (plan.isEmpty()) {
                    return List.of();
                }
                plans.add(plan.get());
            }
            return plans;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0440\u0435\u0437\u0435\u0440\u0432\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b FD", (Throwable)exception);
            throw exception;
        }
    }

    private static Optional<InventoryPlan> reserveSingle(InventoryView inventory, Predicate<ItemStack> matcher, Predicate<ItemStack> allowed) {
        try {
            return FarmersDelightCompat.reserveFromVirtualSlots(inventory, FarmersDelightCompat.createVirtualSlots(inventory), matcher, allowed);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0440\u0435\u0437\u0435\u0440\u0432\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 FD", (Throwable)exception);
            throw exception;
        }
    }

    private static Optional<InventoryPlan> reserveFromVirtualSlots(InventoryView inventory, List<VirtualSlot> virtualSlots, Predicate<ItemStack> matcher, Predicate<ItemStack> allowed) {
        try {
            for (VirtualSlot virtualSlot : virtualSlots) {
                ItemStack available = virtualSlot.stack();
                if (available.isEmpty() || !matcher.test(available) || !allowed.test(available)) continue;
                ItemStack planned = available.copyWithCount(1);
                available.shrink(1);
                return Optional.of(new InventoryPlan(inventory.side(), virtualSlot.slot(), planned));
            }
            return Optional.empty();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0431\u0440\u0430\u0442\u044c \u0432\u0438\u0440\u0442\u0443\u0430\u043b\u044c\u043d\u044b\u0439 \u0441\u043b\u043e\u0442 FD", (Throwable)exception);
            throw exception;
        }
    }

    private static List<VirtualSlot> createVirtualSlots(InventoryView inventory) {
        try {
            ArrayList<VirtualSlot> slots = new ArrayList<VirtualSlot>();
            for (int slot = 0; slot < inventory.handler().getSlots(); ++slot) {
                ItemStack stack = inventory.handler().getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                slots.add(new VirtualSlot(slot, stack.copy()));
            }
            return slots;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0441\u043d\u0438\u043c\u043e\u043a \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f FD \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {}", inventory.side(), exception);
            throw exception;
        }
    }

    private static List<ItemStack> extractPlans(InventoryView inventory, List<InventoryPlan> plans, TamedRat rat, String action) {
        try {
            ArrayList<ItemStack> extracted = new ArrayList<ItemStack>();
            for (InventoryPlan plan : plans) {
                extracted.add(FarmersDelightCompat.extractPlan(inventory, plan, rat, action));
            }
            return extracted;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0438\u0437\u0432\u043b\u0435\u0447\u044c \u043d\u0430\u0431\u043e\u0440 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 FD \u0434\u043b\u044f {}", rat.getUUID(), action, exception);
            throw exception;
        }
    }

    private static ItemStack extractPlan(InventoryView inventory, InventoryPlan plan, TamedRat rat, String action) {
        try {
            return FarmersDelightCompat.extractExact(inventory, plan.slot(), plan.stack().getCount(), stack -> ItemStack.isSameItemSameTags((ItemStack)stack, (ItemStack)plan.stack()), rat, action);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0438\u0437\u0432\u043b\u0435\u0447\u044c \u043f\u043b\u0430\u043d {} \u0434\u043b\u044f {}", rat.getUUID(), plan, action, exception);
            throw exception;
        }
    }

    private static ItemStack extractExact(InventoryView inventory, int slot, int amount, Predicate<ItemStack> matcher, TamedRat rat, String action) {
        try {
            ItemStack current = inventory.handler().getStackInSlot(slot);
            if (current.isEmpty() || !matcher.test(current)) {
                throw new IllegalStateException("\u0421\u043b\u043e\u0442 " + slot + " \u0438\u0437\u043c\u0435\u043d\u0438\u043b\u0441\u044f \u043f\u0435\u0440\u0435\u0434 \u0438\u0437\u0432\u043b\u0435\u0447\u0435\u043d\u0438\u0435\u043c FD \u0434\u043b\u044f " + action + ": " + String.valueOf(current));
            }
            ItemStack simulated = inventory.handler().extractItem(slot, amount, true);
            if (simulated.getCount() != amount || !matcher.test(simulated)) {
                throw new IllegalStateException("\u0421\u043b\u043e\u0442 " + slot + " \u043d\u0435 \u043e\u0442\u0434\u0430\u0451\u0442 \u043d\u0443\u0436\u043d\u044b\u0439 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 FD \u0434\u043b\u044f " + action + ": " + String.valueOf(simulated));
            }
            ItemStack extracted = inventory.handler().extractItem(slot, amount, false);
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u0431\u0440\u0430\u043b\u0430 {} \u0438\u0437 \u0441\u043b\u043e\u0442\u0430 {} \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}", rat.getUUID(), extracted, slot, inventory.side(), action);
            return extracted;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0438\u0437\u0432\u043b\u0435\u0447\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 FD \u0438\u0437 \u0441\u043b\u043e\u0442\u0430 {} \u0434\u043b\u044f {}", rat.getUUID(), slot, action, exception);
            throw exception;
        }
    }

    private static Optional<InventoryView> resolveInventory(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, String action) {
        try {
            for (Direction side : FarmersDelightCompat.orderedSides(preferredSide)) {
                IItemHandler handler = RebornCapabilities.items(level, pos, side);
                if (handler == null) continue;
                return Optional.of(new InventoryView(handler, side));
            }
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action);
            return Optional.empty();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043d\u0430\u0439\u0442\u0438 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }

    private static Direction[] orderedSides(Direction preferredSide) {
        try {
            Direction[] directions = Direction.values();
            Direction[] ordered = new Direction[directions.length];
            ordered[0] = preferredSide;
            int index = 1;
            for (Direction direction : directions) {
                if (direction == preferredSide) continue;
                ordered[index++] = direction;
            }
            return ordered;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0431\u0440\u0430\u0442\u044c \u043f\u043e\u0440\u044f\u0434\u043e\u043a \u0441\u0442\u043e\u0440\u043e\u043d FD \u0434\u043b\u044f {}", preferredSide, exception);
            throw exception;
        }
    }

    private static boolean canInsertAll(TamedRat rat, BlockPos depositPos, List<ItemStack> stacks, String action) {
        try {
            for (ItemStack stack : stacks) {
                if (RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, stack, rat, action)) continue;
                return false;
            }
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0432\u0441\u0442\u0430\u0432\u043a\u0443 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432 FD \u0432 {}", rat.getUUID(), depositPos, exception);
            throw exception;
        }
    }

    private static void returnToPickupOrThrow(TamedRat rat, BlockPos pickupPos, ItemStack stack, String action) {
        try {
            if (stack.isEmpty()) {
                return;
            }
            ItemStack remaining = RatInventoryAccess.insert(rat.level(), pickupPos, rat.pickupFacing, stack.copy(), rat, action);
            if (!remaining.isEmpty()) {
                throw new IllegalStateException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u0435\u0440\u043d\u0443\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 FD \u0432 \u0441\u0443\u043d\u0434\u0443\u043a \u0437\u0430\u0431\u043e\u0440\u0430, \u043e\u0441\u0442\u0430\u0442\u043e\u043a: " + String.valueOf(remaining));
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u0435\u0440\u043d\u0443\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 {} \u0432 {}", rat.getUUID(), stack, pickupPos, exception);
            throw exception;
        }
    }

    private static void damageToolOrBreak(TamedRat rat, ItemStack tool) {
        try {
            if (tool.isDamageableItem()) {
                ItemStack previousHand = rat.getMainHandItem().copy();
                rat.setItemInHand(InteractionHand.MAIN_HAND, tool);
                tool.hurtAndBreak(1, rat, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                rat.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u0442\u0440\u0430\u0442\u0438\u0442\u044c \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c \u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u0430 FD {}", rat.getUUID(), tool, exception);
            throw exception;
        }
    }

    private static boolean samePlanItem(InventoryPlan plan, ItemStack stack) {
        try {
            return ItemStack.isSameItemSameTags((ItemStack)plan.stack(), (ItemStack)stack);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u0440\u0430\u0432\u043d\u0438\u0442\u044c \u043f\u043b\u0430\u043d FD {} \u0441\u043e \u0441\u0442\u0435\u043a\u043e\u043c {}", plan, stack, exception);
            throw exception;
        }
    }

    private static void markChanged(BlockEntity blockEntity) {
        try {
            blockEntity.setChanged();
            if (blockEntity.getLevel() != null) {
                BlockState state = blockEntity.getBlockState();
                blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043c\u0435\u0442\u0438\u0442\u044c \u0431\u043b\u043e\u043a FD {} \u0438\u0437\u043c\u0435\u043d\u0451\u043d\u043d\u044b\u043c", blockEntity.getBlockPos(), exception);
            throw exception;
        }
    }

    private static /* synthetic */ boolean lambda$findCuttingBoardPlan$7(FdFilter filter, boolean outputTargeted, ItemStack stack) {
        return filter.allowsIngredient(stack, outputTargeted);
    }

    private record FdFilter(boolean whitelistPresent, Set<String> whitelistIds, Set<String> blacklistIds) {
        private static FdFilter fromRat(TamedRat rat) {
            try {
                Set<String> whitelist = FdFilter.readFilterIds(rat, (Item)RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
                Set<String> blacklist = FdFilter.readFilterIds(rat, (Item)RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get());
                return new FdFilter(RatUpgradeUtils.hasUpgrade(rat, (Item)RatsItemRegistry.RAT_UPGRADE_WHITELIST.get()), whitelist, blacklist);
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u0441\u043f\u0438\u0441\u043a\u0438 FD", rat.getUUID(), exception);
                throw exception;
            }
        }

        private boolean hasWhitelist() {
            try {
                return this.whitelistPresent;
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043d\u0430\u043b\u0438\u0447\u0438\u0435 whitelist FD", (Throwable)exception);
                throw exception;
            }
        }

        private boolean isWhitelisted(ItemStack stack) {
            try {
                return this.whitelistIds.contains(FdFilter.getItemId(stack));
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c whitelist FD \u0434\u043b\u044f {}", stack, exception);
                throw exception;
            }
        }

        private boolean isBlacklisted(ItemStack stack) {
            try {
                return this.blacklistIds.contains(FdFilter.getItemId(stack));
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c blacklist FD \u0434\u043b\u044f {}", stack, exception);
                throw exception;
            }
        }

        private boolean allowsProcessing(ItemStack input, ItemStack output) {
            try {
                if (this.isBlacklisted(input) || this.isBlacklisted(output)) {
                    return false;
                }
                if (!this.whitelistPresent) {
                    return true;
                }
                return this.isWhitelisted(output) || this.isWhitelisted(input);
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0443 FD \u0434\u043b\u044f \u0432\u0445\u043e\u0434\u0430 {} \u0438 \u0432\u044b\u0445\u043e\u0434\u0430 {}", input, output, exception);
                throw exception;
            }
        }

        private boolean allowsIngredient(ItemStack stack, boolean outputTargeted) {
            try {
                if (this.isBlacklisted(stack)) {
                    return false;
                }
                return !this.whitelistPresent || outputTargeted || this.isWhitelisted(stack);
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442 FD {}", stack, exception);
                throw exception;
            }
        }

        private boolean allowsIngredientForTargetRecipe(ItemStack stack) {
            try {
                return !this.isBlacklisted(stack);
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442 \u0446\u0435\u043b\u0435\u0432\u043e\u0433\u043e \u0440\u0435\u0446\u0435\u043f\u0442\u0430 FD {}", stack, exception);
                throw exception;
            }
        }

        private boolean allowsContainer(ItemStack stack) {
            try {
                return !this.isBlacklisted(stack);
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 FD {}", stack, exception);
                throw exception;
            }
        }

        private boolean allowsCollectedResult(ItemStack stack) {
            try {
                if (this.isBlacklisted(stack)) {
                    return false;
                }
                return !this.whitelistPresent || this.isWhitelisted(stack);
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0433\u043e\u0442\u043e\u0432\u044b\u0439 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 FD {}", stack, exception);
                throw exception;
            }
        }

        private boolean allowsTool(ItemStack stack) {
            try {
                return !this.isBlacklisted(stack);
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442 FD {}", stack, exception);
                throw exception;
            }
        }

        private static Set<String> readFilterIds(TamedRat rat, Item upgradeItem) {
            try {
                ItemStack upgrade = RatUpgradeUtils.getUpgrade(rat, upgradeItem);
                if (upgrade.isEmpty()) {
                    return Set.of();
                }
                CompoundTag tag = upgrade.getOrCreateTag().copy();
                if (!tag.contains("Items", 9)) {
                    return Set.of();
                }
                ListTag list = tag.getList("Items", 10);
                HashSet<String> ids = new HashSet<String>();
                for (int index = 0; index < list.size(); ++index) {
                    String id = list.getCompound(index).getString("id");
                    if (id.isBlank()) continue;
                    ids.add(new ResourceLocation((String)id).toString());
                }
                return ids;
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a \u0443\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u044f {}", rat.getUUID(), upgradeItem, exception);
                throw exception;
            }
        }

        private static String getItemId(ItemStack stack) {
            try {
                return Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0438\u0434\u0435\u043d\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u0440 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430 FD {}", stack, exception);
                throw exception;
            }
        }
    }

    private record InventoryView(IItemHandler handler, Direction side) {
    }

    private record CookingPotPlan(String recipeId, List<InventoryPlan> ingredients) {
    }

    private record InventoryPlan(Direction side, int slot, ItemStack stack) {
    }

    private record CuttingBoardPlan(String recipeId, InventoryPlan ingredient, InventoryPlan tool, List<ItemStack> outputs) {
    }

    private record VirtualSlot(int slot, ItemStack stack) {
    }
}

