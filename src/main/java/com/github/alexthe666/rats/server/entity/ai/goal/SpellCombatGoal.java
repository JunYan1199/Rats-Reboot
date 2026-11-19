package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.server.entity.rat.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public final class SpellCombatGoal extends Goal {
    private final TamedRat rat;
    public SpellCombatGoal(TamedRat rat){this.rat=rat;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public boolean canUse(){return rat.getCommand()==RatCommand.SPELL_COMBAT&&rat.canMove()&&!rat.isOrderedToSit();}
    public boolean requiresUpdateEveryTick(){return true;}
    public void tick(){
        if(rat.tickCount%10!=0)return;
        LivingEntity t=rat.getTarget();
        if(t==null||!t.isAlive()||rat.distanceToSqr(t)>40*40){
            var owner=rat.getOwner();
            if(owner!=null&&rat.distanceToSqr(owner)>9*9)rat.getNavigation().moveTo(owner,1.16);
            else rat.getNavigation().stop();
            return;
        }
        if(rat.legion.isCasting()){rat.getNavigation().stop();return;}
        if(rat.distanceToSqr(t)>15*15||!rat.hasLineOfSight(t)){rat.getNavigation().moveTo(t,1.15);return;}
        if(rat.distanceToSqr(t)<4*4){
            Vec3 away=rat.position().subtract(t.position()).multiply(1,0,1).normalize().scale(4).add(rat.position());
            rat.getNavigation().moveTo(away.x,away.y,away.z,1.2);
        }else rat.getNavigation().stop();
    }
    public void stop(){rat.getNavigation().stop();}
}

