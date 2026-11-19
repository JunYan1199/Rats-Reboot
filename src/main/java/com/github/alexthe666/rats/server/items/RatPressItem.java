package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.server.entity.rat.DiggingRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class RatPressItem extends BlockItem {
    public RatPressItem() {
        super(com.github.alexthe666.rats.registry.RatsBlockRegistry.RAT_PRESS.get(), new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        return player != null && player.isSecondaryUseActive() ? super.useOn(context) : InteractionResult.PASS;
    }

    public InteractionResult capture(Player player, DiggingRat rat) {
        if (rat instanceof TamedRat pet && !player.getUUID().equals(pet.getOwnerUUID())) {
            return fail(player, "owner");
        }
        if (!rat.isAlive() || rat.isRemoved() || rat instanceof TamedRat pet && pet.getRespawnCountdown() > 0)
            return InteractionResult.FAIL;
        if (!(rat.level() instanceof ServerLevel world)) return InteractionResult.SUCCESS;

        // Use an empty slot explicitly. Creative inventory insertion can silently void overflow.
        int slot = player.getInventory().getFreeSlot();
        if (slot < 0) return fail(player, "full");
        ItemStack patty = RatSliceItem.pressed(rat);
        player.getInventory().setItem(slot, patty);
        rat.stopRiding();
        rat.ejectPassengers();
        rat.discard();
        world.sendParticles(ParticleTypes.POOF, rat.getX(), rat.getY() + .2, rat.getZ(), 6, .15, .1, .15, .01);
        world.playSound(null, rat.blockPosition(), com.github.alexthe666.rats.registry.RatsSoundRegistry.RAT_TRAP_CLOSE.get(), SoundSource.PLAYERS, .8f, 1.2f);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult fail(Player player, String reason) {
        if (!player.level().isClientSide) player.displayClientMessage(Component.translatable("item.rats.rat_press." + reason), true);
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flags) {
        lines.add(Component.translatable("item.rats.rat_press.use"));
    }
}
