package com.github.alexthe666.rats.server.entity.ai.goal;
import com.github.alexthe666.rats.server.entity.rat.Mastery;

import com.github.alexthe666.rats.server.entity.rat.RatCommand;

import com.github.alexthe666.rats.server.inventory.container.RatContainer;

import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.entity.AutoCurdlerBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.UpdateCurdlerFluidPacket;
import com.github.alexthe666.rats.server.message.UpdateRatFluidPacket;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.List;

public class RatDepositGoal extends Goal implements RatWorkGoal {
	private final TamedRat rat;
	private final DepositType type;
	private BlockPos targetBlock = null;
	private long nextDeposit = -1;

	public RatDepositGoal(TamedRat entity, DepositType type) {
		super();
		this.rat = entity;
		this.type = type;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
if (!this.rat.canWorkBySchedule() || !this.rat.canWorkFromHunger()) return false;

		if (!this.rat.canMove()) return false;
		RatCommand command = rat.getCommand();
        if(command==RatCommand.COOK||command==RatCommand.SPELL_COMBAT)return false;
		if (!command.workCommand && !(type == DepositType.INVENTORY && (command == RatCommand.WANDER || command == RatCommand.FOLLOW))) return false;
		if (this.rat.getTarget() != null) return false;
		if (this.rat.getDepositPos().isEmpty() || !this.rat.getDepositPos().get().dimension().equals(this.rat.level().dimension()) || RatUtils.isBlockProtected(this.rat.level(), this.rat.getDepositPos().get().pos(), this.rat))
			return false;

		BlockEntity te = this.rat.level().getBlockEntity(this.rat.getDepositPos().get().pos());
		if (te == null) return false;

		if (this.type == DepositType.INVENTORY) {
			if (!hasCargo()) {
				nextDeposit = -1;
				return false;
			}
			long now = rat.level().getGameTime();
			if (nextDeposit < 0) nextDeposit = now + 200;
			if (now < nextDeposit) return false;
			if (te.getCapability(ForgeCapabilities.ITEM_HANDLER, this.rat.depositFacing).resolve().isEmpty()) {
				return false;
			}

		} else if (this.type == DepositType.ENERGY) {
			if (this.rat.getRFTransferRate() <= 0 || this.rat.getHeldRF() <= 0) {
				return false;
			}
			if (te.getCapability(ForgeCapabilities.ENERGY, this.rat.depositFacing).resolve().isEmpty()) {
				return false;
			}
		} else if (this.type == DepositType.FLUID) {
			if (this.rat.transportingFluid.isEmpty() || this.rat.transportingFluid.getAmount() == 0) {
				return false;
			}
			if (te.getCapability(ForgeCapabilities.FLUID_HANDLER, this.rat.depositFacing).resolve().isEmpty()) {
				return false;
			}
		}
		this.resetTarget();
		return targetBlock != null;
	}

	private boolean hasCargo() {
		for (int i = 0; i < RatContainer.SIZE; i = i == 0 ? RatContainer.BACKPACK : i + 1) {
			ItemStack stack = rat.getInventory().getItem(i);
			if (!stack.isEmpty() && rat.shouldDepositItem(stack)) return true;
		}
		return false;
	}

	private void resetTarget() {
		this.targetBlock = this.rat.getDepositPos().get().pos();
	}

	@Override
	public boolean canContinueToUse() {
if (!this.rat.canWorkBySchedule() || !this.rat.canWorkFromHunger()) return false;

		if (type == DepositType.INVENTORY) {
			return targetBlock != null && rat.getDepositPos().filter(pos -> pos.dimension().equals(rat.level().dimension())
					&& pos.pos().equals(targetBlock)).isPresent() && hasCargo();
		}
		return targetBlock != null && (type == DepositType.ENERGY ? rat.getHeldRF() > 0 : !rat.transportingFluid.isEmpty());
	}

	@Override
	public void start() {
		this.rat.isCurrentlyWorking = true;
	}

	@Override
	public void stop() {
		this.rat.isCurrentlyWorking = false;
	}

	private Vec3 getMovePos() {
		return Vec3.atBottomCenterOf(this.targetBlock.relative(this.rat.depositFacing));
	}

	@Override
	public void tick() {
		BlockEntity te = this.rat.level().getBlockEntity(this.targetBlock);
		if (this.targetBlock != null && te != null) {
			this.rat.getNavigation().moveTo(this.getMovePos().x(), this.getMovePos().y(), this.getMovePos().z(), this.rat.getHungerAdjustedWorkSpeed(1.25D));
			double distance = Math.sqrt(this.rat.distanceToSqr(this.getMovePos().x(), this.getMovePos().y(), this.getMovePos().z()));
			if (distance < 4.5D * this.rat.getRatDistanceModifier() && distance > 2.5D * this.rat.getRatDistanceModifier() && te instanceof Container container) {
				this.toggleChest(container, true);
			}
			if (distance <= 2.0D * this.rat.getRatDistanceModifier()) {
				if (te instanceof Container container) {
					this.toggleChest(container, false);
				}

				this.executeTask(te);
				this.targetBlock = null;
				this.stop();
			}
		}
	}

	public void toggleChest(Container te, boolean open) {
		if (te instanceof ChestBlockEntity chest) {
			if (open) {
				this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, 1);
			} else {
				this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, 0);
			}
			this.rat.level().gameEvent(this.rat, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, chest.getBlockPos());
		}
	}

	private void executeTask(BlockEntity entity) {
		if (this.type == DepositType.INVENTORY) {
			LazyOptional<IItemHandler> handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, this.rat.depositFacing);
			if (handler.resolve().isPresent()) {
				IItemHandler resolvedHandler = handler.resolve().get();
				nextDeposit = rat.level().getGameTime() + 200;
				for (int i = 0; i < RatContainer.SIZE; i = i == 0 ? RatContainer.BACKPACK : i + 1) {
					ItemStack cargo = rat.getInventory().getItem(i);
					if (cargo.isEmpty() || !rat.shouldDepositItem(cargo)) continue;
					ItemStack rest = ItemHandlerHelper.insertItemStacked(resolvedHandler, cargo.copy(), false);
					if (rest.getCount() < cargo.getCount()){rat.getInventory().setItem(i, rest);Mastery.deposited(rat);}
				}
			}
		} else if (this.type == DepositType.ENERGY) {
			LazyOptional<IEnergyStorage> handler = entity.getCapability(ForgeCapabilities.ENERGY, this.rat.depositFacing);
			if (handler.resolve().isPresent()) {
				IEnergyStorage storage = handler.resolve().get();
				int howMuchWeHave = this.rat.getHeldRF();
				int inputtedEnergy = 0;
				try {
					if (storage.canReceive() && storage.receiveEnergy(howMuchWeHave, true) > 0) {
						inputtedEnergy = storage.receiveEnergy(howMuchWeHave, false);
					}
				} catch (Exception e) {
					//container is empty
				}
				if (inputtedEnergy > 0) {
					this.rat.setHeldRF(Math.max(0, this.rat.getHeldRF() - inputtedEnergy));
				}
			}
		} else if (this.type == DepositType.FLUID) {
			FluidStack copiedFluid = this.rat.transportingFluid.copy();
			LazyOptional<IFluidHandler> handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, this.rat.depositFacing);
			if (handler.resolve().isPresent()) {
				IFluidHandler fluidHandler = handler.resolve().get();
				if (!this.rat.transportingFluid.isEmpty()) {
					int minusAmount = 0;
					try {
						if (fluidHandler.getTanks() > 0) {
							FluidStack firstTank = fluidHandler.getFluidInTank(0);
							if (fluidHandler.getTanks() > 1) {
								for (int i = 0; i < fluidHandler.getTanks(); i++) {
									FluidStack otherTank = fluidHandler.getFluidInTank(i);
									if (copiedFluid != null && copiedFluid.isFluidEqual(otherTank)) {
										firstTank = otherTank;
									}
								}
							}
							if (firstTank.isEmpty() || (copiedFluid == null || copiedFluid.isFluidEqual(firstTank))) {
								if (fluidHandler.fill(copiedFluid, IFluidHandler.FluidAction.SIMULATE) != 0) {
									minusAmount = fluidHandler.fill(copiedFluid, IFluidHandler.FluidAction.EXECUTE);
								}
							}
						}
					} catch (Exception e) {
						//container is empty
					}

					if (minusAmount > 0) {
						int total = copiedFluid.getAmount() - minusAmount;
						if (total <= 0) {
							this.rat.transportingFluid = FluidStack.EMPTY;
						} else {
							this.rat.transportingFluid.setAmount(total);
						}
						if (!this.rat.level().isClientSide()) {
							RatsNetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
							if (this.rat.level().getBlockEntity(this.targetBlock) instanceof AutoCurdlerBlockEntity curdler) {
								RatsNetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new UpdateCurdlerFluidPacket(this.targetBlock.asLong(), curdler.getTank().getFluid()));
							}
						}
						SoundEvent sound = this.rat.transportingFluid.isEmpty() ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL;
						this.rat.playSound(sound, 1, 1);
					}
				}
			}
		}
	}

	@Override
	public TaskType getRatTaskType() {
		return TaskType.DEPOSIT;
	}

	public enum DepositType {
		INVENTORY,
		FLUID,
		ENERGY
	}
}
