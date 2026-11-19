package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.server.brick.BrickJob;
import com.github.alexthe666.rats.server.brick.BrickWork;
import com.github.alexthe666.rats.server.brick.BrickWorkNetwork;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 国王权杖：给布瑞克派当前职业（由帽子决定）的子任务、圈地、设驻守点、调参数 */
public final class BrickScepterScreen extends Screen {
    private static final int W = 252, H = 196;
    private BrickWorkNetwork.View v;

    private BrickScepterScreen(BrickWorkNetwork.View v) { super(Component.translatable("brick.rats.scepter.title")); this.v = v; }

    public static void show(BrickWorkNetwork.View v) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof BrickScepterScreen s && s.v.rat() == v.rat()) { s.v = v; s.rebuildWidgets(); }
        else mc.setScreen(new BrickScepterScreen(v));
    }

    private BrickJob job() { return BrickJob.values()[v.job()]; }
    private void send(int action, int value) { RatsNetworkHandler.CHANNEL.sendToServer(new BrickWorkNetwork.Edit(v.rat(), action, value)); }

    @Override protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        BrickJob job = job();
        String[] tasks = BrickWork.TASKS.getOrDefault(job, new String[0]);
        String jobKey = job.name().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < tasks.length; i++) {
            final int bit = i;
            boolean on = (v.mask() & (1 << i)) != 0;
            Component label = Component.literal(on ? "✔ " : "✘ ").append(Component.translatable("brick.rats.task." + jobKey + "." + tasks[i]));
            addRenderableWidget(Button.builder(label, b -> send(1, bit)).bounds(x + 10 + (i % 2) * 118, y + 46 + (i / 2) * 22, 114, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("brick.rats.task." + jobKey + "." + tasks[i] + ".desc"))).build());
        }
        int row = y + 46 + ((tasks.length + 1) / 2) * 22 + 6;
        if (job == BrickJob.SENTRY) {
            addRenderableWidget(Button.builder(Component.translatable("brick.rats.scepter.set_post"), b -> { send(3, 0); onClose(); }).bounds(x + 10, row, 114, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("brick.rats.scepter.clear"), b -> send(4, 0)).bounds(x + 128, row, 114, 20).build());
            row += 22;
        } else if (job == BrickJob.GUARD || job == BrickJob.FARMER || job == BrickJob.RANCHER) {
            addRenderableWidget(Button.builder(Component.translatable("brick.rats.scepter.select_area"), b -> { send(2, 0); onClose(); }).bounds(x + 10, row, 114, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("brick.rats.scepter.clear"), b -> send(4, 0)).bounds(x + 128, row, 114, 20).build());
            row += 22;
        }
        if (job == BrickJob.RANCHER) {
            addRenderableWidget(Button.builder(Component.literal("-"), b -> send(5, hasShiftDown() ? -8 : -1)).bounds(x + 128, row, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> send(5, hasShiftDown() ? 8 : 1)).bounds(x + 222, row, 20, 20).build());
            row += 22;
        }
        addRenderableWidget(Button.builder(Component.translatable(v.working() ? "brick.rats.scepter.stop" : "brick.rats.scepter.start"), b -> send(0, 0))
                .bounds(x + 10, y + H - 30, 154, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x + 170, y + H - 30, 72, 20).build());
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = (width - W) / 2, y = (height - H) / 2;
        g.fill(x, y, x + W, y + H, 0xF0202230);
        g.fill(x + 1, y + 1, x + W - 1, y + 2, 0xFFE0B040);
        BrickJob job = job();
        g.drawString(font, Component.translatable("brick.rats.scepter.header", v.name()), x + 10, y + 8, 0xFFE8C860);
        g.drawString(font, Component.translatable("brick.rats.scepter.job", Component.translatable("brick.rats.job." + job.name().toLowerCase(java.util.Locale.ROOT))), x + 10, y + 21, 0xFFFFFFFF);
        g.drawString(font, Component.translatable(v.working() ? "brick.rats.scepter.state_on" : "brick.rats.scepter.state_off"), x + 150, y + 21, v.working() ? 0xFF7CE07C : 0xFFB0B0B0);
        g.drawString(font, Component.translatable("brick.rats.scepter.tasks"), x + 10, y + 34, 0xFFB8BCD0);
        String[] tasks = BrickWork.TASKS.getOrDefault(job, new String[0]);
        int row = y + 46 + ((tasks.length + 1) / 2) * 22 + 6;
        if (job == BrickJob.SENTRY) row += 22;
        else if (job == BrickJob.GUARD || job == BrickJob.FARMER || job == BrickJob.RANCHER) row += 22;
        if (job == BrickJob.RANCHER) {
            g.drawString(font, Component.translatable("brick.rats.scepter.breed_cap"), x + 10, row + 6, 0xFFFFFFFF);
            g.drawCenteredString(font, String.valueOf(v.breedCap()), x + 185, row + 6, 0xFFFFE080);
            row += 22;
        }
        // 范围 / 驻守点 / 统计
        Component info;
        if (job == BrickJob.SENTRY) info = v.post().map(p -> Component.translatable("brick.rats.scepter.post_at", p.getX(), p.getY(), p.getZ(), v.storedXp()))
                .orElse(Component.translatable("brick.rats.scepter.no_post"));
        else if (job == BrickJob.TREASURE) info = Component.translatable("brick.rats.scepter.treasure_left", v.treasureLeft(), BrickWork.TREASURE_PER_DAY);
        else info = v.a().isPresent() && v.b().isPresent()
                ? Component.translatable("brick.rats.scepter.area_at", Math.abs(v.b().get().getX() - v.a().get().getX()) + 1, Math.abs(v.b().get().getZ() - v.a().get().getZ()) + 1)
                : Component.translatable("brick.rats.scepter.no_area");
        g.drawString(font, info, x + 10, row + 4, 0xFFC8D0E8);
        if (!v.status().getString().isEmpty()) g.drawString(font, Component.translatable("brick.rats.scepter.status", v.status()), x + 10, row + 16, 0xFF9AA0B8);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }
}
