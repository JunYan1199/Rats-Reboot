package com.github.alexthe666.rats.server.catcatch;

import com.github.alexthe666.rats.registry.RatsMenuRegistry;
import com.github.alexthe666.rats.server.items.RatSliceItem;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 挎包界面：一排 9 格（原版箱子界面外观），每格只能放一张鼠片。 */
public class CatSatchelMenu extends ChestMenu {

	public CatSatchelMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(CatSatchel.SIZE));
	}

	public CatSatchelMenu(int id, Inventory inventory, Container container) {
		super(RatsMenuRegistry.CAT_SATCHEL_CONTAINER.get(), id, inventory, container, 1);
		for (int i = 0; i < CatSatchel.SIZE; i++) {
			Slot old = this.slots.get(i);
			Slot slot = new Slot(container, i, old.x, old.y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.getItem() instanceof RatSliceItem;
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			};
			slot.index = i;
			this.slots.set(i, slot);
		}
	}

	/** 直接读写猫身上的挎包数据。 */
	public static class SatchelContainer implements Container {
		private final Cat cat;
		private final CatSatchel satchel;

		public SatchelContainer(Cat cat, CatSatchel satchel) {
			this.cat = cat;
			this.satchel = satchel;
		}

		@Override
		public int getContainerSize() {
			return CatSatchel.SIZE;
		}

		@Override
		public boolean isEmpty() {
			return this.satchel.isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return this.satchel.items().get(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			return ContainerHelper.removeItem(this.satchel.items(), slot, amount);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return ContainerHelper.takeItem(this.satchel.items(), slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			this.satchel.items().set(slot, stack);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return stack.getItem() instanceof RatSliceItem;
		}

		@Override
		public void setChanged() {
			// 取出鼠片腾出空位：清除“回主人等待”的状态，让猫恢复捕鼠
			if (!this.satchel.isFull()) {
				this.satchel.depotBlocked = false;
				this.satchel.depotRetryAt = 0;
			}
		}

		@Override
		public boolean stillValid(Player player) {
			return this.cat.isAlive() && this.satchel.isEquipped() && player.distanceToSqr(this.cat) <= 64.0D;
		}

		@Override
		public void clearContent() {
			for (int i = 0; i < CatSatchel.SIZE; i++) this.satchel.items().set(i, ItemStack.EMPTY);
		}
	}
}
