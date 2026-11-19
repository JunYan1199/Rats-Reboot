package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.server.entity.rat.RatProgression.Gene;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class RatTrainingItem extends Item {
    private final int levels;
    private final Gene gene;

    public RatTrainingItem(int levels) {
        super(new Properties());
        this.levels = levels;
        this.gene = null;
    }

    public RatTrainingItem(Gene gene) {
        super(new Properties());
        this.levels = 0;
        this.gene = gene;
    }

    public InteractionResult feed(TamedRat rat, Player player, InteractionHand hand) {
        if (!rat.isOwnedBy(player) || !rat.isAlive() || rat.getRespawnCountdown() > 0)
            return InteractionResult.FAIL;
        if (rat.level().isClientSide()) return InteractionResult.SUCCESS;
        boolean applied;
        Component message;
        if (gene == null) {
            applied = rat.eatLevelCandy(levels);
            message = applied ? Component.translatable("rats.training.leveled", rat.getDisplayName(), rat.getRatLevel())
                              : Component.translatable("rats.training.level_max");
        } else {
            float value = rat.improveGene(gene);
            applied = value > 0;
            message = applied ? Component.translatable("rats.training.improved", getDescription(), String.format(Locale.ROOT, "%.2f", value))
                              : Component.translatable("rats.training.gene_max");
        }
        if (applied) {
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            rat.playSound(SoundEvents.GENERIC_EAT, 0.6F, 1.3F);
        }
        player.displayClientMessage(message, true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add((gene == null ? Component.translatable("rats.training.candy_tip", levels)
                                : Component.translatable("rats.training.gene_tip")).withStyle(ChatFormatting.GRAY));
    }
}
