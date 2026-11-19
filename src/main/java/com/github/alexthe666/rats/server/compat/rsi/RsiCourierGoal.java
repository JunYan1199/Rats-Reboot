package com.github.alexthe666.rats.server.compat.rsi;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** 占住移动/视线控制，搬运期间不让游荡、跟随、采集等目标抢老鼠；实际走位由 {@link RsiCourierJobs} 驱动。 */
public class RsiCourierGoal extends Goal {
	private final TamedRat rat;

	public RsiCourierGoal(TamedRat rat) {
		this.rat = rat;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return RsiCourierJobs.isWorking(this.rat);
	}

	@Override
	public boolean canContinueToUse() {
		return RsiCourierJobs.isWorking(this.rat);
	}

	@Override
	public boolean isInterruptable() {
		return false;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
