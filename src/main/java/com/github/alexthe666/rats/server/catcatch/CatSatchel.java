package com.github.alexthe666.rats.server.catcatch;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.items.RatSliceItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 猫咪捕鼠挎包：挂在原版猫身上的能力数据（是否佩戴、9 格鼠片库存、绑定的存货容器）。
 * 随实体一起存档（ForgeCaps），退出重进不丢失。
 */
public class CatSatchel implements INBTSerializable<CompoundTag> {
	public static final ResourceLocation ID = new ResourceLocation(RatsMod.MODID, "cat_satchel");
	public static final Capability<CatSatchel> CAP = CapabilityManager.get(new CapabilityToken<>() {
	});
	public static final int SIZE = 9;

	private boolean equipped;
	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	@Nullable
	private GlobalPos depot;

	/** 运行时状态（不存档）：存货失败后的等待截止时间。 */
	public long depotRetryAt;
	/** 运行时状态：上一次存货失败（容器满/不可达/被拆），猫回主人身边等待。 */
	public boolean depotBlocked;

	@Nullable
	public static CatSatchel get(@Nullable Entity entity) {
		if (!(entity instanceof Cat)) return null;
		return entity.getCapability(CAP).resolve().orElse(null);
	}

	@Nullable
	public static CatSatchel equipped(@Nullable Entity entity) {
		CatSatchel s = get(entity);
		return s != null && s.isEquipped() ? s : null;
	}

	public boolean isEquipped() {
		return this.equipped;
	}

	public void setEquipped(boolean equipped) {
		this.equipped = equipped;
	}

	public NonNullList<ItemStack> items() {
		return this.items;
	}

	public int count() {
		int n = 0;
		for (ItemStack s : this.items) if (!s.isEmpty()) n++;
		return n;
	}

	public boolean isFull() {
		return this.count() >= SIZE;
	}

	public boolean isEmpty() {
		return this.count() == 0;
	}

	/** 放入一张鼠片（每格只放一张）。没有空位时返回 false，调用方必须先检查，不能丢失鼠片。 */
	public boolean insert(ItemStack slice) {
		if (slice.isEmpty() || !(slice.getItem() instanceof RatSliceItem)) return false;
		for (int i = 0; i < SIZE; i++) {
			if (this.items.get(i).isEmpty()) {
				this.items.set(i, slice.copyWithCount(1));
				return true;
			}
		}
		return false;
	}

	/** 取出全部内容并清空（卸下挎包时使用）。 */
	public List<ItemStack> takeAll() {
		List<ItemStack> out = new ArrayList<>();
		for (int i = 0; i < SIZE; i++) {
			if (!this.items.get(i).isEmpty()) out.add(this.items.get(i));
			this.items.set(i, ItemStack.EMPTY);
		}
		return out;
	}

	@Nullable
	public GlobalPos getDepot() {
		return this.depot;
	}

	public void setDepot(@Nullable GlobalPos depot) {
		this.depot = depot;
		this.depotBlocked = false;
		this.depotRetryAt = 0;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("Equipped", this.equipped);
		ListTag list = new ListTag();
		for (int i = 0; i < SIZE; i++) {
			ItemStack s = this.items.get(i);
			if (s.isEmpty()) continue;
			CompoundTag e = new CompoundTag();
			e.putByte("Slot", (byte) i);
			s.save(e);
			list.add(e);
		}
		tag.put("Items", list);
		if (this.depot != null) {
			tag.putString("DepotDim", this.depot.dimension().location().toString());
			tag.putLong("DepotPos", this.depot.pos().asLong());
		}
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		this.equipped = tag.getBoolean("Equipped");
		for (int i = 0; i < SIZE; i++) this.items.set(i, ItemStack.EMPTY);
		ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag e = list.getCompound(i);
			int slot = e.getByte("Slot") & 255;
			if (slot < SIZE) this.items.set(slot, ItemStack.of(e));
		}
		this.depot = null;
		if (tag.contains("DepotDim", Tag.TAG_STRING)) {
			ResourceLocation dim = ResourceLocation.tryParse(tag.getString("DepotDim"));
			if (dim != null) {
				this.depot = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(tag.getLong("DepotPos")));
			}
		}
	}

	public static class Provider implements ICapabilitySerializable<CompoundTag> {
		private final CatSatchel data = new CatSatchel();
		private final LazyOptional<CatSatchel> opt = LazyOptional.of(() -> this.data);

		@Override
		public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
			return CAP.orEmpty(cap, this.opt);
		}

		@Override
		public CompoundTag serializeNBT() {
			return this.data.serializeNBT();
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			this.data.deserializeNBT(nbt);
		}
	}
}
