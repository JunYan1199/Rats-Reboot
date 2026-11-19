package com.github.alexthe666.rats.server.brick;
import com.github.alexthe666.rats.server.entity.rat.*;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public final class BrickCookGoal extends Goal {
    private final TamedRat rat;
    public BrickCookGoal(TamedRat rat){this.rat=rat;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){return rat.isBrick()&&rat.getCommand()==RatCommand.COOK&&!rat.kitchen.idle();}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){rat.kitchen.tick();}
    @Override public void stop(){rat.getNavigation().stop();rat.setBrickCooking(false);}
}
