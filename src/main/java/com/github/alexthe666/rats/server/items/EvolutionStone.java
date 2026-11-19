package com.github.alexthe666.rats.server.items;
import com.github.alexthe666.rats.server.entity.rat.RatEvolution;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

public final class EvolutionStone extends Item {
    private final RatEvolution form;
    public EvolutionStone(RatEvolution form){super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));this.form=form;}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tips,TooltipFlag flag){
        tips.add(Component.translatable("evolution.rats.stone_hint",RatEvolution.MIN_LEVEL,form.title()));
        tips.add(form.bonus());
        tips.add(form.mastery(1,0));tips.add(form.mastery(2,0));
    }
}