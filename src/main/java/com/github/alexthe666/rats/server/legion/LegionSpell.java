package com.github.alexthe666.rats.server.legion;
import net.minecraft.world.entity.LivingEntity;

interface LegionSpell {
    String id();
    default boolean isBook(){return false;}
    int duration();
    int cooldown();
    boolean available();
    boolean begin(LivingEntity target);
    void tick(int elapsed);
    void finish(boolean cancelled);
}
