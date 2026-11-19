package com.github.alexthe666.rats.client.model.entity;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.events.SingingRatMusic;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Rigid textured quads and local bone poses exported from the approved singing-rat model. */
public class SingingRatModel<T extends AbstractRat> extends EntityModel<T> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(RatsMod.MODID, "textures/entity/rat/singing.png");
    private static final ResourceLocation GEOMETRY = new ResourceLocation(RatsMod.MODID, "rat_models/singing_rat.json");
    private static Rig rig;
    private double seconds;
    private boolean baby;

    public static void reload(ResourceManager resources) {
        try (var reader = resources.openAsReader(GEOMETRY)) {
            Rig loaded = new Gson().fromJson(reader, Rig.class);
            for (Part part : loaded.parts) {
                if (part.parent >= 0) loaded.parts[part.parent].children.add(part);
                if ("jaw".equals(part.name)) {
                    for (int i = 0; i < loaded.frames; i++) {
                        float height = part.translationKeys[i * 3 + 1];
                        if (height > part.translationKeys[part.closed * 3 + 1]) part.closed = i;
                        if (height < part.translationKeys[part.open * 3 + 1]) part.open = i;
                    }
                }
            }
            rig = loaded;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load the singing rat", e);
        }
    }

    @Override
    public void setupAnim(T rat, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        seconds = SingingRatMusic.playbackTicks(rat, age - rat.tickCount) / 20.0;
        baby = rat.isBaby();
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (rig == null) return;
        double time = seconds % rig.duration;
        float pose = (float)((time * 132.0 / 240.0) % 1.0) * (rig.frames - 1);
        float lip = (float)(time / rig.duration * rig.mouth.length);
        int sample = (int)lip % rig.mouth.length;
        float mouth = Mth.lerp(lip - (int)lip, rig.mouth[sample], rig.mouth[(sample + 1) % rig.mouth.length]);
        stack.pushPose();
        stack.translate(0, 1.501, 0);
        float scale = baby ? .19F : .325F;
        stack.scale(scale, -scale, -scale);
        rig.parts[0].draw(stack, buffer, (int)pose, Math.min((int)pose + 1, rig.frames - 1), pose - (int)pose, mouth, light, overlay, red, green, blue, alpha);
        stack.popPose();
    }

    private static final class Rig {
        Part[] parts;
        int frames;
        double duration;
        float[] mouth;
    }

    private static final class Part {
        String name;
        int parent, closed, open;
        float[] translation, rotation, translationKeys, rotationKeys, vertices;
        final List<Part> children = new ArrayList<>();
        final Quaternionf orientation = new Quaternionf();

        private float component(float[] base, float[] keys, int axis, int a, int b, float mix) {
            if (keys == null) return base[axis];
            return Mth.lerp(mix, keys[a * base.length + axis], keys[b * base.length + axis]);
        }

        void draw(PoseStack stack, VertexConsumer buffer, int a, int b, float mix, float mouth,
                  int light, int overlay, float red, float green, float blue, float alpha) {
            boolean isJaw = closed != open;
            int first = isJaw ? closed : a, second = isJaw ? open : b;
            float weight = isJaw ? mouth : mix;
            stack.pushPose();
            stack.translate(component(translation, translationKeys, 0, first, second, weight),
                    component(translation, translationKeys, 1, first, second, weight), component(translation, translationKeys, 2, first, second, weight));
            orientation.set(component(rotation, rotationKeys, 0, first, second, weight), component(rotation, rotationKeys, 1, first, second, weight),
                    component(rotation, rotationKeys, 2, first, second, weight), component(rotation, rotationKeys, 3, first, second, weight)).normalize();
            stack.mulPose(orientation);
            if (vertices != null) {
                var pose = stack.last();
                for (int i = 0; i < vertices.length; i += 8) {
                    buffer.vertex(pose.pose(), vertices[i], vertices[i+1], vertices[i+2]).color(red, green, blue, alpha)
                            .uv(vertices[i+3], vertices[i+4]).overlayCoords(overlay).uv2(light)
                            .normal(pose.normal(), vertices[i+5], vertices[i+6], vertices[i+7]).endVertex();
                }
            }
            for (Part child : children) child.draw(stack, buffer, a, b, mix, mouth, light, overlay, red, green, blue, alpha);
            stack.popPose();
        }
    }
}
