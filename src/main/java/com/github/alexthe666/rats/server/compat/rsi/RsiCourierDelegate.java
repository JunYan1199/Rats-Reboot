package com.github.alexthe666.rats.server.compat.rsi;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 由 GoetyBatchDelegateMixin 实现，把 rs_integration 祭坛委托的少量内部状态暴露给老鼠快递逻辑。
 * 这里不引用任何 rs_integration 类型，未安装该模组时老鼠模组照常加载。
 */
public interface RsiCourierDelegate {
	/** 本次仪式 RSI 已放上的基座记录（GoetyBatchDelegate$PlacedPedestal）。 */
	List<?> rats$filledPedestals();

	/** RSI 失败退款时会退回这里的物品（未成功放置的材料）。 */
	List<ItemStack> rats$unplacedInputs();

	/** 绕过老鼠接管，直接调用原始的 startAltarRitual。 */
	boolean rats$startRitualDirect(Object altar, ServerPlayer player, ItemStack activation, Object recipe);
}
