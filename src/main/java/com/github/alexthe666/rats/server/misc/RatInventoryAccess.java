package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.create.CreateCompat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.github.alexthe666.rats.server.misc.RebornCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public final class RatInventoryAccess {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction[][] ORDERED_SIDES = RatInventoryAccess.createOrderedSides();

    private RatInventoryAccess() {
        throw new IllegalStateException("\u0421\u0435\u0440\u0432\u0438\u0441 \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f\u043c \u043d\u0435 \u0441\u043e\u0437\u0434\u0430\u0451\u0442\u0441\u044f \u043a\u0430\u043a \u043e\u0431\u044a\u0435\u043a\u0442");
    }

    public static boolean canInsert(Level level, BlockPos pos, Direction preferredSide, ItemStack stack, TamedRat rat, String action) {
        try {
            if (stack.isEmpty()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043f\u0440\u043e\u0432\u0435\u0440\u044f\u0435\u0442 \u0432\u0441\u0442\u0430\u0432\u043a\u0443 \u0432 {}: \u0441\u0442\u0435\u043a \u043f\u0443\u0441\u0442\u043e\u0439, \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435 {}", rat.getUUID(), pos, action);
                return false;
            }
            for (Direction side : RatInventoryAccess.orderedSides(preferredSide)) {
                IItemHandler handler = RebornCapabilities.items(level, pos, side);
                if (handler == null) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0443 \u0431\u043b\u043e\u043a\u0430 {} \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}", rat.getUUID(), pos, side, action);
                    continue;
                }
                ItemStack simulated = ItemHandlerHelper.insertItem((IItemHandler)handler, (ItemStack)stack.copy(), (boolean)true);
                if (simulated.getCount() < stack.getCount()) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0431\u043b\u043e\u043a {} \u043f\u0440\u0438\u043c\u0435\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}", rat.getUUID(), pos, side, action);
                    return true;
                }
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0431\u043b\u043e\u043a {} \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u0432\u043e \u0432\u0441\u0442\u0430\u0432\u043a\u0435 {} \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}", rat.getUUID(), pos, stack, side, action);
            }
            if (RatInventoryAccess.canUseCreateProxy(level, pos, rat, action)) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0431\u043b\u043e\u043a Create {} \u0431\u0443\u0434\u0435\u0442 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d \u043a\u0430\u043a \u0440\u0443\u0447\u043d\u0430\u044f \u0442\u043e\u0447\u043a\u0430 \u0432\u0441\u0442\u0430\u0432\u043a\u0438 \u0434\u043b\u044f {}", rat.getUUID(), pos, action);
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0432\u0441\u0442\u0430\u0432\u043a\u0443 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0432 \u0431\u043b\u043e\u043a {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }

    public static ItemStack insert(Level level, BlockPos pos, Direction preferredSide, ItemStack stack, TamedRat rat, String action) {
        try {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack remaining = stack.copy();
            for (Direction side : RatInventoryAccess.orderedSides(preferredSide)) {
                IItemHandler handler = RebornCapabilities.items(level, pos, side);
                if (handler == null) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u043d\u0435\u043b\u044c\u0437\u044f \u0432\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0432 {} \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}: \u043d\u0435\u0442 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430", rat.getUUID(), pos, side, action);
                    continue;
                }
                ItemStack before = remaining.copy();
                remaining = ItemHandlerHelper.insertItem((IItemHandler)handler, (ItemStack)remaining, (boolean)false);
                int moved = before.getCount() - remaining.getCount();
                if (moved > 0) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u043b\u043e\u0436\u0438\u043b\u0430 {} \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0432 {} \u0447\u0435\u0440\u0435\u0437 \u0441\u0442\u043e\u0440\u043e\u043d\u0443 {} \u0434\u043b\u044f {}", rat.getUUID(), moved, pos, side, action);
                }
                if (!remaining.isEmpty()) continue;
                return ItemStack.EMPTY;
            }
            ItemStack createRemaining = RatInventoryAccess.insertWithCreateProxy(level, pos, remaining, rat, action);
            if (createRemaining.getCount() < remaining.getCount()) {
                return createRemaining;
            }
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u043d\u043e\u0441\u0442\u044c\u044e \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0432 {} \u0434\u043b\u044f {}, \u043e\u0441\u0442\u0430\u0442\u043e\u043a {}", rat.getUUID(), pos, action, remaining);
            return remaining;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0432 \u0431\u043b\u043e\u043a {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }

    public static boolean canExtract(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, String action) {
        try {
            for (Direction side : RatInventoryAccess.orderedSides(preferredSide)) {
                IItemHandler handler = RebornCapabilities.items(level, pos, side);
                if (handler == null) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0443 \u0431\u043b\u043e\u043a\u0430 {} \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}", rat.getUUID(), pos, side, action);
                    continue;
                }
                int slot = RatUtils.getItemSlotFromItemHandler(rat, handler, level.getRandom());
                if (slot >= 0) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0431\u043b\u043e\u043a {} \u043e\u0442\u0434\u0430\u0441\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0438\u0437 \u0441\u043b\u043e\u0442\u0430 {} \u0434\u043b\u044f {}", rat.getUUID(), pos, side, slot, action);
                    return true;
                }
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0432 \u0431\u043b\u043e\u043a\u0435 {} \u043d\u0435\u0442 \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0438\u0445 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}", rat.getUUID(), pos, side, action);
            }
            if (RatInventoryAccess.canUseCreateProxy(level, pos, rat, action)) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0431\u043b\u043e\u043a Create {} \u0431\u0443\u0434\u0435\u0442 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d \u043a\u0430\u043a \u0440\u0443\u0447\u043d\u0430\u044f \u0442\u043e\u0447\u043a\u0430 \u0437\u0430\u0431\u043e\u0440\u0430 \u0434\u043b\u044f {}", rat.getUUID(), pos, action);
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0438\u0437\u0432\u043b\u0435\u0447\u0435\u043d\u0438\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0438\u0437 \u0431\u043b\u043e\u043a\u0430 {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }

    public static ItemStack extract(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, int amount, String action) {
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("\u041a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0438\u0437\u0432\u043b\u0435\u0447\u0435\u043d\u0438\u044f \u0434\u043e\u043b\u0436\u043d\u043e \u0431\u044b\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u043c: " + amount);
            }
            for (Direction side : RatInventoryAccess.orderedSides(preferredSide)) {
                IItemHandler handler = RebornCapabilities.items(level, pos, side);
                if (handler == null) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u043d\u0435\u043b\u044c\u0437\u044f \u0437\u0430\u0431\u0440\u0430\u0442\u044c \u0438\u0437 {} \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}: \u043d\u0435\u0442 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430", rat.getUUID(), pos, side, action);
                    continue;
                }
                int slot = RatUtils.getItemSlotFromItemHandler(rat, handler, level.getRandom());
                if (slot < 0) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u043d\u0435\u0442 \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0435\u0433\u043e \u0441\u043b\u043e\u0442\u0430 \u0432 {} \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u0434\u043b\u044f {}", rat.getUUID(), pos, side, action);
                    continue;
                }
                ItemStack simulated = handler.extractItem(slot, amount, true);
                if (simulated.isEmpty()) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {}: \u0441\u043b\u043e\u0442 {} \u0432 {} \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {} \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u0432 \u0438\u0437\u0432\u043b\u0435\u0447\u0435\u043d\u0438\u0438 \u0434\u043b\u044f {}", rat.getUUID(), slot, pos, side, action);
                    continue;
                }
                ItemStack extracted = handler.extractItem(slot, amount, false);
                if (extracted.isEmpty()) continue;
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u0431\u0440\u0430\u043b\u0430 {} \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u0438\u0437 {} \u0447\u0435\u0440\u0435\u0437 \u0441\u0442\u043e\u0440\u043e\u043d\u0443 {} \u0434\u043b\u044f {}", rat.getUUID(), extracted.getCount(), pos, side, action);
                return extracted;
            }
            ItemStack createExtracted = RatInventoryAccess.extractWithCreateProxy(level, pos, rat, amount, action);
            if (!createExtracted.isEmpty()) {
                return createExtracted;
            }
            return ItemStack.EMPTY;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0438\u0437\u0432\u043b\u0435\u0447\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0438\u0437 \u0431\u043b\u043e\u043a\u0430 {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }

    private static Direction[] orderedSides(Direction preferredSide) {
        try {
            return ORDERED_SIDES[preferredSide.ordinal()];
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0431\u0440\u0430\u0442\u044c \u043f\u043e\u0440\u044f\u0434\u043e\u043a \u0441\u0442\u043e\u0440\u043e\u043d \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f, \u043f\u0440\u0435\u0434\u043f\u043e\u0447\u0438\u0442\u0430\u0435\u043c\u0430\u044f \u0441\u0442\u043e\u0440\u043e\u043d\u0430 {}", preferredSide, exception);
            throw exception;
        }
    }

    private static Direction[][] createOrderedSides() {
        try {
            Direction[][] ordered = new Direction[DIRECTIONS.length][DIRECTIONS.length];
            for (Direction preferredSide : DIRECTIONS) {
                Direction[] sides = ordered[preferredSide.ordinal()];
                sides[0] = preferredSide;
                int index = 1;
                for (Direction direction : DIRECTIONS) {
                    if (direction == preferredSide) continue;
                    sides[index++] = direction;
                }
            }
            return ordered;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043a\u0435\u0448 \u043f\u043e\u0440\u044f\u0434\u043a\u0430 \u0441\u0442\u043e\u0440\u043e\u043d \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f", (Throwable)exception);
            throw exception;
        }
    }

    private static boolean canUseCreateProxy(Level level, BlockPos pos, TamedRat rat, String action) {
        try {
            boolean canUse;
            boolean bl = canUse = rat.hasWorkerUpgrade() && CreateCompat.canWorkerUse(level.getBlockState(pos));
            if (!canUse) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442 Create-\u043f\u0440\u043e\u043a\u0441\u0438 \u0434\u043b\u044f {}: \u043d\u0435\u0442 \u0443\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u044f \u0440\u0430\u0431\u043e\u0442\u044f\u0433\u0438 \u0438\u043b\u0438 \u0431\u043b\u043e\u043a \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0430\u043d", rat.getUUID(), action);
            }
            return canUse;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c Create-\u043f\u0440\u043e\u043a\u0441\u0438 \u0431\u043b\u043e\u043a\u0430 {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }

    private static ItemStack insertWithCreateProxy(Level level, BlockPos pos, ItemStack stack, TamedRat rat, String action) {
        try {
            if (!RatInventoryAccess.canUseCreateProxy(level, pos, rat, action)) {
                return stack;
            }
            rat.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            boolean worked = CreateCompat.workBlock(rat, pos);
            ItemStack remaining = rat.getMainHandItem().copy();
            if (worked && remaining.getCount() < stack.getCount()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u043b\u043e\u0436\u0438\u043b\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0432 Create-\u0431\u043b\u043e\u043a {} \u0447\u0435\u0440\u0435\u0437 \u0440\u0443\u0447\u043d\u043e\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435 \u0434\u043b\u044f {}", rat.getUUID(), pos, action);
                return remaining;
            }
            return stack;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0447\u0435\u0440\u0435\u0437 Create-\u043f\u0440\u043e\u043a\u0441\u0438 {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }

    private static ItemStack extractWithCreateProxy(Level level, BlockPos pos, TamedRat rat, int amount, String action) {
        try {
            if (!RatInventoryAccess.canUseCreateProxy(level, pos, rat, action)) {
                return ItemStack.EMPTY;
            }
            rat.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            boolean worked = CreateCompat.workBlock(rat, pos);
            ItemStack extracted = rat.getMainHandItem().copy();
            if (worked && !extracted.isEmpty()) {
                if (extracted.getCount() > amount) {
                    ItemStack limited = extracted.copyWithCount(amount);
                    ItemStack remainder = extracted.copyWithCount(extracted.getCount() - amount);
                    rat.spawnAtLocation(remainder, 0.0f);
                    return limited;
                }
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u0431\u0440\u0430\u043b\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0438\u0437 Create-\u0431\u043b\u043e\u043a\u0430 {} \u0447\u0435\u0440\u0435\u0437 \u0440\u0443\u0447\u043d\u043e\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435 \u0434\u043b\u044f {}", rat.getUUID(), pos, action);
                return extracted;
            }
            return ItemStack.EMPTY;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u0431\u0440\u0430\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0447\u0435\u0440\u0435\u0437 Create-\u043f\u0440\u043e\u043a\u0441\u0438 {} \u0434\u043b\u044f {}", rat.getUUID(), pos, action, exception);
            throw exception;
        }
    }
}

