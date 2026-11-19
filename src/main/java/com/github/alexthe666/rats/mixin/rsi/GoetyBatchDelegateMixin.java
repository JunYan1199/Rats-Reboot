package com.github.alexthe666.rats.mixin.rsi;

import com.github.alexthe666.rats.server.compat.rsi.RsiCourierDelegate;
import com.github.alexthe666.rats.server.compat.rsi.RsiCourierJobs;
import com.huanghuang.rsintegration.crafting.batch.AbstractBatchDelegate;
import com.huanghuang.rsintegration.crafting.batch.IBatchDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 让 rs_integration 的诡厄祭坛下单改由「共振快递」老鼠搬运。仅在同时安装 rs_integration 与 goety 时应用（见 RsiMixinPlugin）。
 */
@Mixin(targets = "com.huanghuang.rsintegration.mods.goety.GoetyBatchDelegate", remap = false)
public abstract class GoetyBatchDelegateMixin extends AbstractBatchDelegate implements RsiCourierDelegate {
	@Shadow
	private List<?> filledPedestals;
	@Shadow
	@Final
	private List<ItemStack> unplacedRitualInputs;
	@Shadow
	private long ritualStoppedSinceGameTime;
	@Shadow
	private long ritualIdleSinceGameTime;

	@Shadow
	private boolean startAltarRitual(Object altar, ServerPlayer player, ItemStack activation, Object recipe) {
		throw new AssertionError();
	}

	@Override
	public List<?> rats$filledPedestals() {
		return this.filledPedestals;
	}

	@Override
	public List<ItemStack> rats$unplacedInputs() {
		return this.unplacedRitualInputs;
	}

	@Override
	public boolean rats$startRitualDirect(Object altar, ServerPlayer player, ItemStack activation, Object recipe) {
		return this.startAltarRitual(altar, player, activation, recipe);
	}

	@Inject(method = "startAltarRitual", at = @At("HEAD"), cancellable = true)
	private void rats$handToCourier(Object altar, ServerPlayer player, ItemStack activation, Object recipe, CallbackInfoReturnable<Boolean> cir) {
		if (RsiCourierJobs.tryDefer(this, altar, player, activation, recipe)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "prepare", at = @At("HEAD"), cancellable = true)
	private void rats$waitForCourier(ServerPlayer player, ResourceLocation recipeId, ResourceLocation dim, BlockPos pos, CallbackInfoReturnable<IBatchDelegate.PreparationResult> cir) {
		if (RsiCourierJobs.isAltarBusy(this, dim, pos)) {
			cir.setReturnValue(IBatchDelegate.PreparationResult.retry("Rat courier is still delivering to this altar", Component.translatable("rats.rsi_courier.altar_busy")));
		}
	}

	@Inject(method = "isMachineCraftFinished", at = @At("HEAD"), cancellable = true)
	private void rats$notFinishedWhileDelivering(ServerLevel level, BlockEntity be, CallbackInfoReturnable<Boolean> cir) {
		if (RsiCourierJobs.status(this) == RsiCourierJobs.Status.PENDING) cir.setReturnValue(false);
	}

	@Inject(method = "observeMachineCraft", at = @At("HEAD"), cancellable = true)
	private void rats$observeCourier(ServerLevel level, BlockEntity be, CallbackInfoReturnable<IBatchDelegate.CraftObservation> cir) {
		RsiCourierJobs.Status status = RsiCourierJobs.status(this);
		if (status == RsiCourierJobs.Status.PENDING) {
			cir.setReturnValue(this.workingObservation());
		} else if (status == RsiCourierJobs.Status.FAILED) {
			RsiCourierJobs.forget(this);
			cir.setReturnValue(this.failObservation("Goety ritual could not be started after rat courier delivery"));
		} else if (status == RsiCourierJobs.Status.STARTED) {
			RsiCourierJobs.forget(this);
			this.ritualStoppedSinceGameTime = -1L;
			this.ritualIdleSinceGameTime = -1L;
		}
	}

	@Inject(method = {"clearMachineState", "clearMissingMachineState", "onBatchFinished"}, at = @At("HEAD"))
	private void rats$abortCourier(CallbackInfo ci) {
		RsiCourierJobs.abort(this);
	}
}
