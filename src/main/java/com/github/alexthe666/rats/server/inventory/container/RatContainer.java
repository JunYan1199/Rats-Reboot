package com.github.alexthe666.rats.server.inventory.container;

import com.github.alexthe666.rats.server.entity.rat.InventoryRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.EquipmentListenerUpgrade;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class RatContainer extends SimpleContainer {

	// Slots 0 through 6 keep the IDs used by existing worlds.
	public static final int[] UPGRADES = {3, 4, 5, 7, 8, 9};
	public static final int BACKPACK = 10;
	public static final int SIZE = BACKPACK + 9;
    public static final int BRICK_OFFHAND = SIZE;
    /** 布瑞克的 4 格盔甲栏：头盔、胸甲、护腿、靴子（老鼠的胸/腿/脚装备位被升级槽占着，所以单独存，属性由 BrickWork 手动加） */
    public static final int BRICK_ARMOR = SIZE + 1;
    public static final int TOTAL = SIZE + 5;

	private final InventoryRat rat;

	/** Hand first, then backpack. Returns the accepted amount without mutating the source. */
	public int collect(ItemStack source, int limit, boolean simulate) {
		int remaining = Math.min(source.getCount(), limit);
		int offered = remaining;
		int firstSlot = source.canEquip(EquipmentSlot.HEAD, rat) ? BACKPACK : 0;
		for (int i = firstSlot; i < SIZE && remaining > 0; i = i == 0 ? BACKPACK : i + 1) {
			ItemStack held = getItem(i);
			if (!held.isEmpty() && !ItemStack.isSameItemSameTags(held, source)) continue;
			int moved = Math.min(remaining, source.getMaxStackSize() - held.getCount());
			if (moved <= 0) continue;
			if (!simulate) setItem(i, source.copyWithCount(held.getCount() + moved));
			remaining -= moved;
		}
		return offered - remaining;
	}

	public static boolean isUpgradeSlot(int index) {
		return index >= 3 && index < BACKPACK && index != 6;
	}
	
	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack removed = super.removeItem(index, count);
		if (!removed.isEmpty() && isUpgradeSlot(index)) refreshUpgrade(index);
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		ItemStack removed = super.removeItemNoUpdate(index);
		if (index < 6 && !removed.isEmpty()) rat.containerChanged(this);
		return removed;
	}

	private void refreshUpgrade(int index) {
		if (rat instanceof TamedRat tamed && tamed.isAlive() && !tamed.level().isClientSide()) {
			tamed.syncExtraUpgrade(index);
			tamed.onUpgradeChanged();
		}
	}

	public RatContainer(InventoryRat rat, int size) {
		super(size);
		this.rat = rat;
	}

	public InventoryRat getRat() {
		return this.rat;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		if (index < 3 && this.rat instanceof TamedRat tamed) {
			ItemStack oldStack = this.getItem(index);
			if (!ItemStack.isSameItem(oldStack, stack)) {
				RatUpgradeUtils.forEachUpgrade(tamed, item -> item instanceof EquipmentListenerUpgrade, (upgradeStack, upgradeSlot) ->
					((EquipmentListenerUpgrade) upgradeStack.getItem()).onItemChanged(tamed, this.getIndexedSlot(index), oldStack, stack));
			}
		}
		super.setItem(index, stack);
		if (isUpgradeSlot(index)) refreshUpgrade(index);
	}

	private EquipmentSlot getIndexedSlot(int index) {
		return switch (index) {
			case 1 -> EquipmentSlot.HEAD;
			case 2 -> EquipmentSlot.OFFHAND;
			default -> EquipmentSlot.MAINHAND;
		};
	}
}
