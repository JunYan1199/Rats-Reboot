package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PattyTrapBlockEntity extends BlockEntity {
    private ItemStack slice = ItemStack.EMPTY;

    public PattyTrapBlockEntity(BlockPos pos, BlockState state) {
        super(RatsBlockEntityRegistry.RAT_PRESS.get(), pos, state);
    }

    public boolean occupied() { return !slice.isEmpty(); }
    public void store(ItemStack item) { slice = item; setChanged(); }
    public ItemStack take() {
        ItemStack result = slice;
        store(ItemStack.EMPTY);
        return result;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (occupied()) tag.put("Slice", slice.save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        slice = ItemStack.of(tag.getCompound("Slice"));
    }
}
