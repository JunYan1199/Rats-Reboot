package com.github.alexthe666.rats.server.events;
import com.github.alexthe666.rats.RatsMod;import com.github.alexthe666.rats.registry.*;import com.github.alexthe666.rats.server.entity.rat.TamedRat;import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import net.minecraft.core.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.Item;import net.minecraft.world.level.block.entity.BlockEntity;import net.minecraft.network.chat.Component;import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="rats") public class RebornEvents { @SubscribeEvent     public static void respawnRatAtHouse(LivingDeathEvent event) {
        try {
            TamedRat rat;
            LivingEntity livingEntity = event.getEntity();
            if (!(livingEntity instanceof TamedRat) || (rat = (TamedRat)livingEntity).level().isClientSide() || RatUpgradeUtils.hasUpgrade(rat, (Item)RatsItemRegistry.RAT_UPGRADE_ANGEL.get())) {
                return;
            }
            if (rat.getBoundHousePos().isEmpty() || !rat.getBoundHousePos().get().dimension().equals(rat.level().dimension())) {
                return;
            }
            BlockPos housePos = rat.getBoundHousePos().get().pos();
            BlockEntity blockEntity = rat.level().getBlockEntity(housePos);
            if (!(blockEntity instanceof RatHouseBlockEntity)) {
                RatsMod.LOGGER.warn("\u041a\u0440\u044b\u0441\u0430 {} \u0443\u043c\u0435\u0440\u043b\u0430 \u0441 \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u043e\u0439 \u043a {}, \u043d\u043e \u0434\u043e\u043c\u0438\u043a\u0430 \u0442\u0430\u043c \u043d\u0435\u0442", rat.getUUID(), housePos);
                return;
            }
            RatHouseBlockEntity house = (RatHouseBlockEntity)blockEntity;
            if (!house.beginRespawn(rat, 1200)) {
                return;
            }
            event.setCanceled(true);
            rat.playSound((SoundEvent)RatsSoundRegistry.RAT_DIE.get());
            LivingEntity livingEntity2 = rat.getOwner();
            if (livingEntity2 instanceof Player) {
                Player player = (Player)livingEntity2;
                player.displayClientMessage((Component)Component.translatable((String)"block.rats.rat_house.respawn_started", (Object[])new Object[]{rat.getName()}), true);
            }
            rat.discard();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043a\u0440\u044b\u0441\u0443 \u043d\u0430 \u0440\u0435\u0441\u043f\u0430\u0432\u043d \u0443 \u0434\u043e\u043c\u0438\u043a\u0430", (Throwable)exception);
            throw exception;
        }
    } 
@SubscribeEvent public static void protectTrees(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){if(event.getEntity() instanceof net.minecraft.world.entity.player.Player && (event.getPlacedBlock().is(net.minecraft.tags.BlockTags.LOGS)||event.getPlacedBlock().is(net.minecraft.tags.BlockTags.LEAVES)) && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level){com.github.alexthe666.rats.server.world.RatProtectedTreeBlocksData.get(level).protect(event.getPos());}}
@SubscribeEvent public static void clearTreeProtection(net.minecraftforge.event.level.BlockEvent.BreakEvent event){if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)com.github.alexthe666.rats.server.world.RatProtectedTreeBlocksData.get(level).unprotect(event.getPos());}

}
