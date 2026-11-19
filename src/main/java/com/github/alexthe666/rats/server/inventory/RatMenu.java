package com.github.alexthe666.rats.server.inventory;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsMenuRegistry;
import com.github.alexthe666.rats.registry.RatsUpgradeConflictRegistry;
import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import com.github.alexthe666.rats.server.items.upgrades.BaseRatUpgradeItem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

public class RatMenu extends AbstractContainerMenu {
	private final Container ratInventory;
    private final int ratSlots;
	private static final ResourceLocation EMPTY_HELMET_SLOT = new ResourceLocation(RatsMod.MODID, "item/empty_rat_helmet_slot");
	private static final ResourceLocation EMPTY_BANNER_SLOT = new ResourceLocation(RatsMod.MODID, "item/empty_rat_banner_slot");
	private static final ResourceLocation EMPTY_ITEM_SLOT = new ResourceLocation(RatsMod.MODID, "item/empty_rat_item_slot");
	private static final ResourceLocation EMPTY_UPGRADE_SLOT = new ResourceLocation(RatsMod.MODID, "item/empty_rat_upgrade_slot");
    public static final EquipmentSlot[] BRICK_ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** 按 EquipmentSlot#getIndex（脚0 腿1 胸2 头3）取原版空盔甲栏图标 */
    private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};

	public RatMenu(int id, Container ratInventory, Inventory playerInventory) {
		super(RatsMenuRegistry.RAT_CONTAINER.get(), id);
		this.ratInventory = ratInventory;
		ratInventory.startOpen(playerInventory.player);
		this.addSlot(new Slot(ratInventory, 0, 108, 58) {
			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ITEM_SLOT);
			}
		});
		this.addSlot(new Slot(ratInventory, 1, 108, 22) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return super.mayPlace(stack) && !stack.isEmpty() && stack.canEquip(EquipmentSlot.HEAD, playerInventory.player);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}

			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_HELMET_SLOT);
			}
		});
		this.addSlot(new Slot(ratInventory, 2, 108, 40) {
			public boolean mayPlace(ItemStack stack) {
				return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem() instanceof BannerItem;
			}

			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_BANNER_SLOT);
			}
		});

		for (int i = 0; i < 3; i++) addUpgradeSlot(ratInventory, i, 8);
		this.addSlot(new Slot(ratInventory, 6, 132, 58) {
			@Override public boolean mayPlace(ItemStack stack) {
				return com.github.alexthe666.rats.server.entity.rat.TamedRat.isCheeseReserveItem(stack);
			}
			@Override public int getMaxStackSize() { return 16; }
		});
		for (int i = 3; i < RatContainer.UPGRADES.length; i++) addUpgradeSlot(ratInventory, i, 28);
		for (int i = 0; i < 9; i++) this.addSlot(new Slot(ratInventory, RatContainer.BACKPACK + i, 39 + i * 18, 91));
		if (ratInventory instanceof RatContainer c && c.getRat() instanceof com.github.alexthe666.rats.server.entity.rat.TamedRat r && r.isBrick()) {
            addSlot(new Slot(ratInventory,RatContainer.BRICK_OFFHAND,132,23));
            // 布瑞克盔甲栏：头盔、胸甲、护腿、靴子（只加属性，模型不显示）
            for (int i = 0; i < BRICK_ARMOR_SLOTS.length; i++) {
                EquipmentSlot type = BRICK_ARMOR_SLOTS[i];
                addSlot(new Slot(ratInventory, RatContainer.BRICK_ARMOR + i, 156 + i * 18, 76) {
                    @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof net.minecraft.world.item.ArmorItem a && a.getEquipmentSlot() == type; }
                    @Override public int getMaxStackSize() { return 1; }
                    @Override public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() { return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_ICONS[type.getIndex()]); }
                });
            }
        }
        ratSlots=slots.size();
        for (int j = 0; j < 3; ++j) {
			for (int k1 = 0; k1 < 9; ++k1) {
				this.addSlot(new Slot(playerInventory, k1 + j * 9 + 9, 39 + k1 * 18, 123 + j * 18));
			}
		}

		for (int k = 0; k < 9; ++k) {
			this.addSlot(new Slot(playerInventory, k, 39 + k * 18, 181));
		}
	}

	public RatMenu(int i, Inventory inventory) {
		this(i, new SimpleContainer(RatContainer.SIZE), inventory);
	}

	private void addUpgradeSlot(Container inventory, int position, int x) {
		addSlot(new Slot(inventory, RatContainer.UPGRADES[position], x, 22 + (position % 3) * 18) {
			@Override public int getMaxStackSize() { return 1; }
			@Override public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof BaseRatUpgradeItem && canCoexistUpgrade(inventory, stack, position);
			}
			@Override public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_UPGRADE_SLOT);
			}
		});
	}

	public static boolean canCoexistUpgrade(Container inventory, ItemStack upgrade, int ourSlot) {
		for (int i = 0; i < RatContainer.UPGRADES.length; i++) {
			if (i != ourSlot && RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(RatContainer.UPGRADES[i]))) return false;
		}
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		if(ratInventory instanceof RatContainer c && c.getRat() instanceof com.github.alexthe666.rats.server.entity.rat.TamedRat r)
            return r.isAlive() && r.isOwnedBy(player) && player.distanceToSqr(r)<64 && (ratSlots>RatContainer.SIZE)==r.isBrick();
        return this.ratInventory.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < ratSlots) {
				if (!this.moveItemStackTo(itemstack1, ratSlots, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (ratSlots > RatContainer.BRICK_ARMOR && itemstack1.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                    && !(itemstack1.getItem() instanceof com.github.alexthe666.rats.server.items.HatItem)
                    && !this.getSlot(RatContainer.BRICK_ARMOR + java.util.List.of(BRICK_ARMOR_SLOTS).indexOf(armor.getEquipmentSlot())).hasItem()) {
                // 布瑞克：普通盔甲 Shift 点击进对应盔甲栏（职业帽等帽子仍进头部栏）
                int target = RatContainer.BRICK_ARMOR + java.util.List.of(BRICK_ARMOR_SLOTS).indexOf(armor.getEquipmentSlot());
                if (!this.moveItemStackTo(itemstack1, target, target + 1, false)) return ItemStack.EMPTY;
			} else if (this.getSlot(1).mayPlace(itemstack1)) {
				Slot head = this.getSlot(1);
				if (head.hasItem()) {
					if (itemstack1.getCount() != 1 || !head.mayPickup(player)) return ItemStack.EMPTY;
					ItemStack previous = head.getItem();
					if (ItemStack.matches(previous, itemstack1)) return ItemStack.EMPTY;
					head.set(itemstack1.copy());
					slot.set(previous);
					head.onTake(player, previous);
					return ItemStack.EMPTY; // One exchange. A quick-move retry would swap the hats back.
				}
				if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.getSlot(2).mayPlace(itemstack1) && !this.getSlot(2).hasItem()) {
				if (!this.moveItemStackTo(itemstack1, 2, 3, false)) {
					return ItemStack.EMPTY;
				}
			} else {
				boolean moved = false;
				for (int i : RatContainer.UPGRADES) {
					if (!getSlot(i).hasItem() && getSlot(i).mayPlace(itemstack1)) {
						moved = moveItemStackTo(itemstack1, i, i + 1, false);
						break;
					}
				}
				if (!moved && getSlot(6).mayPlace(itemstack1)) {
					Slot reserve = getSlot(6);
					int before = itemstack1.getCount();
					reserve.safeInsert(itemstack1);
					moved = itemstack1.getCount() < before;
				}
				if (!moved && !moveItemStackTo(itemstack1, RatContainer.BACKPACK, RatContainer.SIZE, false)
						&& !moveItemStackTo(itemstack1, 0, 1, false)) return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return itemstack;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.ratInventory.stopOpen(player);
		if (this.ratInventory instanceof RatContainer container) {
			container.getRat().closeInventory();
		}
	}
}
