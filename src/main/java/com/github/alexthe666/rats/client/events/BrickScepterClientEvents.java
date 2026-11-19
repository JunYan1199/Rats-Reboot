package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** 手持国王权杖时，用金色粒子画出布瑞克的工作区边框、驻守点，以及正在选的第一个角 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID, value = Dist.CLIENT)
public final class BrickScepterClientEvents {
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.2F), 1.0F);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.95F, 0.25F, 0.25F), 1.2F);
    private BrickScepterClientEvents() {}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        var mc = Minecraft.getInstance();
        if (e.phase != TickEvent.Phase.END || mc.player == null || mc.level == null || mc.isPaused() || mc.player.tickCount % 5 != 0) return;
        ItemStack stick = ItemStack.EMPTY;
        for (InteractionHand h : InteractionHand.values()) if (mc.player.getItemInHand(h).is(RatsItemRegistry.KING_SCEPTER.get())) stick = mc.player.getItemInHand(h);
        CompoundTag t = stick.getTag();
        if (t == null || !mc.level.dimension().location().toString().equals(t.getString("ShowDim")) && t.contains("ShowDim")) return;
        if (t.contains("ShowA") && t.contains("ShowB")) box(mc, NbtUtils.readBlockPos(t.getCompound("ShowA")), NbtUtils.readBlockPos(t.getCompound("ShowB")));
        if (t.contains("ShowPost")) ring(mc, NbtUtils.readBlockPos(t.getCompound("ShowPost")));
        if (t.contains("Select") && t.getCompound("Select").contains("First")) {
            BlockPos f = NbtUtils.readBlockPos(t.getCompound("Select").getCompound("First"));
            for (int i = 0; i < 6; i++) mc.level.addParticle(RED, f.getX() + 0.5, f.getY() + 1.1 + i * 0.25, f.getZ() + 0.5, 0, 0, 0);
        }
    }

    private static void box(Minecraft mc, BlockPos a, BlockPos b) {
        double x0 = Math.min(a.getX(), b.getX()), x1 = Math.max(a.getX(), b.getX()) + 1, z0 = Math.min(a.getZ(), b.getZ()), z1 = Math.max(a.getZ(), b.getZ()) + 1;
        double y = Math.max(a.getY(), b.getY()) + 1.05;
        if (mc.player.distanceToSqr((x0 + x1) / 2, y, (z0 + z1) / 2) > 96 * 96) return;
        for (double x = x0; x <= x1; x += 1) { mc.level.addParticle(GOLD, x, y, z0, 0, 0, 0); mc.level.addParticle(GOLD, x, y, z1, 0, 0, 0); }
        for (double z = z0; z <= z1; z += 1) { mc.level.addParticle(GOLD, x0, y, z, 0, 0, 0); mc.level.addParticle(GOLD, x1, y, z, 0, 0, 0); }
    }

    /** 驻守点：一圈半径 8 的点 + 中心一根柱 */
    private static void ring(Minecraft mc, BlockPos p) {
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            mc.level.addParticle(GOLD, p.getX() + 0.5 + Math.cos(a) * 8, p.getY() + 0.2, p.getZ() + 0.5 + Math.sin(a) * 8, 0, 0, 0);
        }
        for (int i = 0; i < 5; i++) mc.level.addParticle(RED, p.getX() + 0.5, p.getY() + 0.3 + i * 0.3, p.getZ() + 0.5, 0, 0, 0);
    }
}
