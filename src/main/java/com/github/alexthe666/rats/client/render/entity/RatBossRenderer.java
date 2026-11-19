package com.github.alexthe666.rats.client.render.entity;

import com.github.alexthe666.rats.client.model.entity.RatModel;
import com.github.alexthe666.rats.client.render.entity.layer.RatEyesLayer;
import com.github.alexthe666.rats.server.pizza.RatBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.*;

/**
 * 鼠头头：老鼠模组原版模型（原版动画）放大 2 倍；头上挂反戴棒球帽、疤、金牙；嘴里叼一整张缺了一块的披萨。
 * 招式姿势在这里做：抡飞时整只转圈、冲撞前低头蓄力、冲撞时前倾、吹哨仰头、撞墙后晕乎乎地左右晃。
 */
public class RatBossRenderer extends AbstractRatRenderer<RatBoss, RatModel<RatBoss>> {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

    public RatBossRenderer(EntityRendererProvider.Context context) {
        super(context, new RatModel<>());
        this.addLayer(new RatEyesLayer<>(this));
        this.addLayer(new Gear(this));
    }

    @Override protected void scale(RatBoss rat, PoseStack stack, float partialTicks) {
        super.scale(rat, stack, partialTicks);
        stack.scale(2.0F, 2.0F, 2.0F);
    }

    @Override protected void setupRotations(RatBoss rat, PoseStack stack, float bob, float yaw, float partial) {
        super.setupRotations(rat, stack, bob, yaw, partial);
        float t = rat.skillTicks() + partial;
        switch (rat.skill()) {
            case RatBoss.SPIN -> { if (t > 6) stack.mulPose(Axis.YP.rotationDegrees((t - 6) * 30F)); else stack.mulPose(Axis.YP.rotationDegrees(-t * 4)); }
            case RatBoss.CHARGE_WINDUP -> { stack.mulPose(Axis.XP.rotationDegrees(12)); stack.translate(Math.sin(t * 2.5) * 0.03, 0, 0); }
            case RatBoss.CHARGING -> stack.mulPose(Axis.XP.rotationDegrees(8));
            case RatBoss.WHISTLE -> { stack.translate(0, 0, 0.25); stack.mulPose(Axis.XP.rotationDegrees(-28)); }
            case RatBoss.THROW -> stack.mulPose(Axis.YP.rotationDegrees((float) Math.sin(t * 0.45) * 25));
            case RatBoss.STUNNED -> stack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t * 0.5) * 12));
            default -> {}
        }
    }

    // ---------------------------------------------------------------- 帽子、疤、金牙、整张披萨（纯色方块，顶点色）
    record Cell(float x, float y, float z, int rgb, int faces) {}
    private static final int CRUST = 0xC98A3E, CRUST_D = 0xA56A28, CHEESE = 0xF4C64A, CHEESE_D = 0xE0A830, SAUCE = 0xC8452C, PEP = 0xA8282C, PEP_D = 0x7E1C20, BASIL = 0x4E8A34;
    private static final int R = 15;
    private static List<Cell> pizza;

    /** 与预览里的整张披萨同一算法（同一随机种子）：半径 15 格、缺一块、8 道切痕，只保留露在外面的面 */
    private static List<Cell> pizza() {
        if (pizza != null) return pizza;
        Map<Long, Integer> grid = new HashMap<>();
        Random r = new Random(11);
        List<double[]> pep = new ArrayList<>();
        for (int k = 0; k < 9; k++) { double a = k * 2.3998, d = R * 0.32 + (k % 3) * R * 0.17; pep.add(new double[]{Math.cos(a) * d, Math.sin(a) * d}); }
        double missFrom = Math.toRadians(150), missTo = Math.toRadians(195);
        for (int i = -R; i <= R; i++) for (int j = -R; j <= R; j++) {
            double cx = i + 0.5, cz = j + 0.5, d = Math.hypot(cx, cz);
            if (d > R) continue;
            double a = Math.atan2(cz, cx);
            if (a >= missFrom && a <= missTo && d > 1.2) continue;
            boolean crust = d > R - 2.2;
            grid.put(key(i, -1, j), CRUST_D);
            int top;
            if (crust) top = r.nextInt(4) == 0 ? CRUST_D : CRUST;
            else {
                top = r.nextInt(6) == 0 ? CHEESE_D : CHEESE;
                for (double[] p : pep) if (Math.hypot(cx - p[0], cz - p[1]) < 1.6) top = cx > p[0] ? PEP_D : PEP;
                if (r.nextInt(45) == 0) top = BASIL;
                double sector = Math.abs(((a / (Math.PI / 4)) % 1 + 1) % 1 - 0.5);
                if (sector > 0.47 && d > 2) top = SAUCE;
            }
            grid.put(key(i, -2, j), top);
            if (crust) grid.put(key(i, -3, j), r.nextInt(3) == 0 ? CRUST_D : CRUST);
        }
        List<Cell> out = new ArrayList<>();
        int[][] dirs = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
        for (var e : grid.entrySet()) {
            int x = (int) (e.getKey() >> 40) - 1000, y = (int) ((e.getKey() >> 20) & 0xFFFFF) - 1000, z = (int) (e.getKey() & 0xFFFFF) - 1000;
            int faces = 0;
            for (int f = 0; f < 6; f++) if (!grid.containsKey(key(x + dirs[f][0], y + dirs[f][1], z + dirs[f][2]))) faces |= 1 << f;
            if (faces != 0) out.add(new Cell(x, y, z, e.getValue(), faces));
        }
        return pizza = out;
    }
    private static long key(int x, int y, int z) { return ((long) (x + 1000) << 40) | ((long) (y + 1000) << 20) | (z + 1000); }

    static final class Gear extends RenderLayer<RatBoss, RatModel<RatBoss>> {
        Gear(RatBossRenderer r) { super(r); }

        @Override public void render(PoseStack p, MultiBufferSource buffers, int light, RatBoss rat, float limb, float speed, float partial, float age, float yaw, float pitch) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            int overlay = LivingEntityRenderer.getOverlayCoords(rat, 0);
            p.pushPose();
            getParentModel().translateToHead(p);
            p.scale(1 / 16F, 1 / 16F, 1 / 16F);                     // 换成模型像素（Y 向下，-Z 朝前）
            int RED = 0xC62A2A, RED_D = 0x8E1C1C, WHITE_C = 0xF2F2EE, GOLD = 0xE8B83A, SCAR = 0x3A2A2A;
            box(p, vc, light, overlay, -2.2F, -2.3F, -2.4F, 2.2F, -1.45F, 0.5F, RED);       // 帽顶
            box(p, vc, light, overlay, -1.7F, -2.7F, -1.9F, 1.7F, -2.3F, 0.2F, RED);
            box(p, vc, light, overlay, -1.9F, -1.55F, 0.5F, 1.9F, -1.3F, 3.0F, RED_D);      // 帽檐朝后
            box(p, vc, light, overlay, -0.4F, -3.0F, -1.5F, 0.4F, -2.65F, -0.7F, RED_D);    // 顶扣
            box(p, vc, light, overlay, -0.9F, -2.3F, -2.55F, 0.9F, -1.6F, -2.4F, WHITE_C);  // 正面白色奶酪标
            box(p, vc, light, overlay, -0.3F, -2.1F, -2.61F, 0.3F, -1.8F, -2.55F, GOLD);
            box(p, vc, light, overlay, 1.45F, -1.55F, -2.55F, 2.65F, -1.35F, -2.45F, SCAR); // 左眼上一道疤
            box(p, vc, light, overlay, 1.9F, -1.1F, -2.6F, 2.1F, -0.2F, -2.45F, SCAR);
            // 嘴：snout 在 (0, 0.5, -2.5)；金牙 + 叼住的披萨
            p.translate(0, 0.5F, -2.5F);
            box(p, vc, light, overlay, 0.3F, 0.9F, -2.1F, 0.9F, 1.5F, -1.6F, GOLD);
            p.translate(0, 1.6F, -1.4F);                                // 嘴边
            p.mulPose(Axis.XP.rotation(-0.19F));                        // 抵消原版低头角度，让披萨基本平放
            p.mulPose(Axis.YP.rotationDegrees(-35));
            p.mulPose(Axis.ZP.rotationDegrees(-4));
            p.scale(0.5F, 0.5F, 0.5F);                                  // 披萨每格半个模型像素（同预览）
            p.translate(-R + 1.0F, 0, 0);
            for (Cell c : pizza()) cube(p, vc, light, overlay, c.x, c.y, c.z, c.x + 1, c.y + 1, c.z + 1, c.rgb, c.faces);
            p.popPose();
        }

        private static void box(PoseStack p, VertexConsumer vc, int light, int overlay, float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
            cube(p, vc, light, overlay, x0, y0, z0, x1, y1, z1, rgb, 63);
        }

        /** faces 位：0 上(-y) 1 下(+y) 2 前(-z) 3 后(+z) 4 左(-x) 5 右(+x) */
        private static void cube(PoseStack p, VertexConsumer vc, int light, int overlay, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int faces) {
            Matrix4f m = p.last().pose(); Matrix3f n = p.last().normal();
            float r = ((rgb >> 16) & 255) / 255F, g = ((rgb >> 8) & 255) / 255F, b = (rgb & 255) / 255F;
            if ((faces & 1) != 0) quad(vc, m, n, light, overlay, r, g, b, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
            if ((faces & 2) != 0) quad(vc, m, n, light, overlay, r, g, b, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
            if ((faces & 4) != 0) quad(vc, m, n, light, overlay, r, g, b, 0, 0, -1, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
            if ((faces & 8) != 0) quad(vc, m, n, light, overlay, r, g, b, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            if ((faces & 16) != 0) quad(vc, m, n, light, overlay, r, g, b, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
            if ((faces & 32) != 0) quad(vc, m, n, light, overlay, r, g, b, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        }

        private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int overlay, float r, float g, float b, float nx, float ny, float nz, float... v) {
            for (int i = 0; i < 12; i += 3)
                vc.vertex(m, v[i], v[i + 1], v[i + 2]).color(r, g, b, 1).uv(0.5F, 0.5F).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
