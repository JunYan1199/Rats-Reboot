package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsItemTags;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.RatCheeseFeederBlock;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

public class RatCheeseFeederBlockEntity
extends BaseContainerBlockEntity
implements WorldlyContainer {
    private static final int SLOT_COUNT = 9;
    private static final int[] SLOTS = IntStream.range(0, 9).toArray();
    private final IItemHandler itemHandler = new SidedInvWrapper((WorldlyContainer)this, Direction.UP);
    private NonNullList<ItemStack> inventory = NonNullList.withSize((int)9, ItemStack.EMPTY);

    public RatCheeseFeederBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)RatsBlockEntityRegistry.RAT_CHEESE_FEEDER.get(), pos, state);
    }

    public static boolean isAllowedCheese(ItemStack stack) {
        try {
            return !stack.isEmpty() && (stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is((Item)RatsItemRegistry.CREATIVE_CHEESE.get()));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u044b\u0440\u043d\u044b\u0439 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 {}", stack, exception);
            throw exception;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RatCheeseFeederBlockEntity feeder) {
        try {
            if (!level.isClientSide() && ((Boolean)state.getValue((Property)RatCheeseFeederBlock.HAS_CHEESE)).booleanValue() != feeder.hasCheese()) {
                feeder.updateCheeseState();
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0441\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", pos, exception);
            throw exception;
        }
    }

    public int addCheeseFrom(ItemStack source) {
        try {
            int slot;
            if (!RatCheeseFeederBlockEntity.isAllowedCheese(source)) {
                throw new IllegalArgumentException("\u041a\u043e\u0440\u043c\u0443\u0448\u043a\u0430 \u043f\u0440\u0438\u043d\u0438\u043c\u0430\u0435\u0442 \u0442\u043e\u043b\u044c\u043a\u043e \u0441\u044b\u0440: " + String.valueOf(source));
            }
            int moved = 0;
            for (slot = 0; slot < this.getContainerSize() && !source.isEmpty(); ++slot) {
                ItemStack existing = this.getItem(slot);
                if (existing.isEmpty() || !ItemStack.isSameItemSameTags((ItemStack)existing, (ItemStack)source) || existing.getCount() >= existing.getMaxStackSize()) continue;
                int amount = Math.min(source.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(amount);
                source.shrink(amount);
                moved += amount;
                this.setChanged();
            }
            for (slot = 0; slot < this.getContainerSize() && !source.isEmpty(); ++slot) {
                if (!this.getItem(slot).isEmpty()) continue;
                int amount = Math.min(source.getCount(), source.getMaxStackSize());
                ItemStack inserted = source.split(amount);
                this.setItem(slot, inserted);
                moved += amount;
            }
            return moved;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0441\u044b\u0440 \u0432 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public ItemStack takeCheeseForRat(int amount) {
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("\u041a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0441\u044b\u0440\u0430 \u0434\u043b\u044f \u0432\u044b\u0434\u0430\u0447\u0438 \u0434\u043e\u043b\u0436\u043d\u043e \u0431\u044b\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u043c: " + amount);
            }
            for (int slot = 0; slot < this.getContainerSize(); ++slot) {
                ItemStack stack = this.getItem(slot);
                if (stack.isEmpty() || !RatCheeseFeederBlockEntity.isAllowedCheese(stack)) continue;
                ItemStack removed = this.removeItem(slot, Math.min(amount, stack.getCount()));
                if (removed.isEmpty()) {
                    throw new IllegalStateException("\u041a\u043e\u0440\u043c\u0443\u0448\u043a\u0430 \u043f\u043e\u043a\u0430\u0437\u0430\u043b\u0430 \u0441\u044b\u0440, \u043d\u043e \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u044b\u0434\u0430\u0442\u044c \u0435\u0433\u043e \u0438\u0437 \u0441\u043b\u043e\u0442\u0430 " + slot);
                }
                return removed;
            }
            return ItemStack.EMPTY;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0434\u0430\u0442\u044c \u0441\u044b\u0440 \u043a\u0440\u044b\u0441\u0435 \u0438\u0437 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public ItemStack takeOneCheeseForRat() {
        try {
            return this.takeCheeseForRat(1);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0434\u0430\u0442\u044c \u043e\u0434\u0438\u043d \u0441\u044b\u0440 \u043a\u0440\u044b\u0441\u0435 \u0438\u0437 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public boolean hasCheese() {
        try {
            for (ItemStack stack : this.inventory) {
                if (stack.isEmpty()) continue;
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043d\u0430\u043b\u0438\u0447\u0438\u0435 \u0441\u044b\u0440\u0430 \u0432 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0435 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        try {
            return this.itemHandler;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c item capability \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public int getContainerSize() {
        return this.inventory.size();
    }

    public boolean isEmpty() {
        return !this.hasCheese();
    }

    public ItemStack getItem(int index) {
        return (ItemStack)this.inventory.get(index);
    }

    public ItemStack removeItem(int index, int count) {
        try {
            ItemStack removed = ContainerHelper.removeItem(this.inventory, (int)index, (int)count);
            if (!removed.isEmpty()) {
                this.setChanged();
            }
            return removed;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0431\u0440\u0430\u0442\u044c \u0441\u044b\u0440 \u0438\u0437 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public ItemStack removeItemNoUpdate(int index) {
        try {
            return ContainerHelper.takeItem(this.inventory, (int)index);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0442\u0438\u0445\u043e \u0437\u0430\u0431\u0440\u0430\u0442\u044c \u0441\u044b\u0440 \u0438\u0437 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public void setItem(int index, ItemStack stack) {
        try {
            if (!stack.isEmpty() && !RatCheeseFeederBlockEntity.isAllowedCheese(stack)) {
                throw new IllegalArgumentException("\u041a\u043e\u0440\u043c\u0443\u0448\u043a\u0430 \u043f\u0440\u0438\u043d\u0438\u043c\u0430\u0435\u0442 \u0442\u043e\u043b\u044c\u043a\u043e \u0441\u044b\u0440: " + String.valueOf(stack));
            }
            this.inventory.set(index, stack);
            if (stack.getCount() > this.getMaxStackSize()) {
                stack.setCount(this.getMaxStackSize());
            }
            this.setChanged();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0432 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public boolean canPlaceItem(int index, ItemStack stack) {
        return RatCheeseFeederBlockEntity.isAllowedCheese(stack);
    }

    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        try {
            boolean allowed = this.canPlaceItem(index, stack);
            if (!allowed) {
                RatsMod.LOGGER.debug("\u041a\u043e\u0440\u043c\u0443\u0448\u043a\u0430 {} \u043e\u0442\u043a\u0430\u0437\u0430\u043b\u0430 \u0430\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0437\u0430\u0446\u0438\u0438 \u0441\u043e \u0441\u0442\u043e\u0440\u043e\u043d\u044b {}: \u043f\u0440\u0435\u0434\u043c\u0435\u0442 {} \u043d\u0435 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u0441\u044b\u0440\u043e\u043c", this.getBlockPos(), direction, stack);
            }
            return allowed;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0432\u0441\u0442\u0430\u0432\u043a\u0443 \u0430\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0437\u0430\u0446\u0438\u0435\u0439 \u0432 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    public boolean stillValid(Player player) {
        return this.getLevel() != null && this.getLevel().getBlockEntity(this.getBlockPos()) == this && player.distanceToSqr((double)this.getBlockPos().getX() + 0.5, (double)this.getBlockPos().getY() + 0.5, (double)this.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    public void clearContent() {
        try {
            this.inventory.clear();
            this.setChanged();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    protected void setItems(NonNullList<ItemStack> items) {
        this.inventory = items;
    }

    public void load(CompoundTag compound) {
        try {
            super.load(compound);
            this.inventory = NonNullList.withSize((int)this.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems((CompoundTag)compound, this.inventory);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    protected void saveAdditional(CompoundTag compound) {
        try {
            super.saveAdditional(compound);
            ContainerHelper.saveAllItems((CompoundTag)compound, this.inventory);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    protected Component getDefaultName() {
        return Component.translatable((String)"block.rats.rat_cheese_feeder");
    }

    protected AbstractContainerMenu createMenu(int id, Inventory player) {
        return this.filteredMenu(id,player);
    }

    @Nullable
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return this.filteredMenu(id,playerInventory);
    }

    public void setChanged() {
        super.setChanged();
        try {
            this.updateCheeseState();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c \u0441\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435 \u0441\u044b\u0440\u0430 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    private void updateCheeseState() {
        try {
            Level level = this.getLevel();
            if (level == null || level.isClientSide()) {
                return;
            }
            BlockState state = this.getBlockState();
            if (!state.hasProperty((Property)RatCheeseFeederBlock.HAS_CHEESE)) {
                throw new IllegalStateException("\u0423 \u0431\u043b\u043e\u043a\u0430 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 \u043d\u0435\u0442 \u0441\u0432\u043e\u0439\u0441\u0442\u0432\u0430 has_cheese");
            }
            boolean hasCheese = this.hasCheese();
            if ((Boolean)state.getValue((Property)RatCheeseFeederBlock.HAS_CHEESE) != hasCheese) {
                level.setBlock(this.getBlockPos(), (BlockState)state.setValue((Property)RatCheeseFeederBlock.HAS_CHEESE, (Comparable)Boolean.valueOf(hasCheese)), 3);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u043c\u043e\u0434\u0435\u043b\u044c \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.getBlockPos(), exception);
            throw exception;
        }
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this, BlockEntity::getUpdateTag);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(net, packet);
        this.handleUpdateTag(packet.getTag());
    }

    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

private net.minecraftforge.common.util.LazyOptional<IItemHandler> forgeItems = net.minecraftforge.common.util.LazyOptional.of(() -> this.itemHandler);
@Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) { if(!isRemoved() && cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) return forgeItems.cast(); return super.getCapability(cap,side); }
@Override public void invalidateCaps(){super.invalidateCaps();forgeItems.invalidate();}
@Override public void reviveCaps(){super.reviveCaps();forgeItems=net.minecraftforge.common.util.LazyOptional.of(() -> this.itemHandler);}


private AbstractContainerMenu filteredMenu(int id, Inventory player){
 ChestMenu menu=new ChestMenu(MenuType.GENERIC_9x1,id,player,this,1);
 for(int i=0;i<9;i++){var slot=new net.minecraft.world.inventory.Slot(this,i,8+i*18,18){@Override public boolean mayPlace(ItemStack stack){return RatCheeseFeederBlockEntity.isAllowedCheese(stack);}};slot.index=i;menu.slots.set(i,slot);}
 return menu;
}

}
