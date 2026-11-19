package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 布瑞克的职业：「帽定职业」——头部栏位戴哪顶职业帽就是哪个职业（国王权杖再给职业派子任务）。
 * 每个职业对应模型里的一顶帽子部件（mesh 名）。
 */
public enum BrickJob {
    NONE(null, "hat"),
    CHEF(RatsItemRegistry.BRICK_CHEF_HAT, "hat"),
    GUARD(RatsItemRegistry.BRICK_HAT_GUARD, "hat_guard"),
    SENTRY(RatsItemRegistry.BRICK_HAT_SENTRY, "hat_sentry"),
    TREASURE(RatsItemRegistry.BRICK_HAT_EXPLORER, "hat_explorer"),
    FARMER(RatsItemRegistry.BRICK_HAT_STRAW, "hat_straw"),
    RANCHER(RatsItemRegistry.BRICK_HAT_COWBOY, "hat_cowboy");

    private final net.minecraftforge.registries.RegistryObject<Item> hat;
    public final String mesh;

    BrickJob(net.minecraftforge.registries.RegistryObject<Item> hat, String mesh) { this.hat = hat; this.mesh = mesh; }

    public static BrickJob of(TamedRat rat) {
        if (!rat.isBrick()) return NONE;
        ItemStack head = rat.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return NONE;
        for (BrickJob job : values()) if (job.hat != null && head.is(job.hat.get())) return job;
        return NONE;
    }

    /** 同步给客户端的职业动作（TamedRat#getBrickAction），客户端按它选动画 */
    public static final class Action {
        public static final int NONE = 0, HOE = 1, SHEAR = 2, MILK = 3, DIG = 4, PRESENT = 5, BLOCK = 6;
        private Action() {}
    }
}
