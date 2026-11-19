package com.github.alexthe666.rats.server.legion;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.*;

public final class LegionBaton extends Item {
    public LegionBaton(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    private static String role(ItemStack s){
        String value=s.hasTag()?s.getTag().getString("LegionRole"):"";
        return RatLegion.ROLES.contains(value)?value:"attack";
    }
    private static UUID squad(ItemStack s){
        var tag=s.getOrCreateTag();
        if(!tag.hasUUID("LegionSquad")) tag.putUUID("LegionSquad",UUID.randomUUID());
        return tag.getUUID("LegionSquad");
    }
    public InteractionResult assign(Player owner,TamedRat rat,ItemStack baton){
        if(!owner.level().isClientSide){
            if(!rat.isTame()||!owner.getUUID().equals(rat.getOwnerUUID())){owner.displayClientMessage(Component.translatable("legion.rats.owner"),true);return InteractionResult.SUCCESS;}
            String role=role(baton);
            rat.legion.assign(rat,role.equals("free")?null:squad(baton),role);
            owner.displayClientMessage(Component.translatable("legion.rats.assign",rat.getDisplayName(),Component.translatable("legion.rats."+role),rat.legion.countEquipped(rat)),true);
        }
        return InteractionResult.sidedSuccess(owner.level().isClientSide);
    }
    private int order(ServerLevel level,Player player,ItemStack baton,LivingEntity target){
        UUID id=squad(baton);int count=0;
        for(TamedRat rat:level.getEntitiesOfClass(TamedRat.class,player.getBoundingBox().inflate(48),r->r.isTame()&&player.getUUID().equals(r.getOwnerUUID())&&id.equals(r.legion.group))){
            rat.legion.mark(target,level.getGameTime());count++;
        }
        player.displayClientMessage(Component.translatable("legion.rats.focus",count,target.getDisplayName()),true);
        return count;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player p,LivingEntity target,InteractionHand hand){
        if(target instanceof TamedRat rat)return assign(p,rat,stack);
        if(!RatLegion.hostile(target,p))return InteractionResult.PASS;
        if(p.level() instanceof ServerLevel world)order(world,p,stack,target);
        return InteractionResult.sidedSuccess(p.level().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(level instanceof ServerLevel world){
            if(player.isShiftKeyDown()){
                int index=RatLegion.ROLES.indexOf(role(stack));
                String next=RatLegion.ROLES.get((index+1)%RatLegion.ROLES.size());
                stack.getOrCreateTag().putString("LegionRole",next);
                player.displayClientMessage(Component.translatable("legion.rats.mode",Component.translatable("legion.rats."+next)),true);
            }else{
                Vec3 start=player.getEyePosition(),delta=player.getViewVector(1).scale(48);
                Vec3 end=level.clip(new ClipContext(start,start.add(delta),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation();
                var hit=ProjectileUtil.getEntityHitResult(player,start,end,player.getBoundingBox().expandTowards(delta).inflate(1),e->e instanceof LivingEntity l&&RatLegion.hostile(l,player),start.distanceToSqr(end));
                if(hit!=null)order(world,player,stack,(LivingEntity)hit.getEntity());
                else player.displayClientMessage(Component.translatable("legion.rats.aim"),true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("legion.rats.mode",Component.translatable("legion.rats."+role(stack))));
        lines.add(Component.translatable("legion.rats.hint"));
    }
}
