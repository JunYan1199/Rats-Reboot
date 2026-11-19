package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** 国王权杖界面的网络包：服务端 → 客户端「打开/刷新界面」，客户端 → 服务端「改设置」 */
public final class BrickWorkNetwork {
    /** 界面数据 */
    public record View(int rat, int job, boolean working, int mask, Optional<BlockPos> a, Optional<BlockPos> b, Optional<BlockPos> post,
                       int breedCap, int storedXp, int treasureLeft, Component name, Component status) {}
    /** 动作：0 开始/停止 1 切换子任务(value=位) 2 开始圈地 3 设驻守点 4 清除范围 5 繁殖上限 += value */
    public record Edit(int rat, int action, int value) {}

    public static void register(SimpleChannel ch, int id) {
        ch.registerMessage(id, View.class, BrickWorkNetwork::write, BrickWorkNetwork::read, BrickWorkNetwork::onView, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        ch.registerMessage(id + 1, Edit.class, (m, b) -> { b.writeVarInt(m.rat); b.writeByte(m.action); b.writeVarInt(m.value); },
                b -> new Edit(b.readVarInt(), b.readUnsignedByte(), b.readVarInt()), BrickWorkNetwork::onEdit, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static void write(View v, FriendlyByteBuf b) {
        b.writeVarInt(v.rat); b.writeByte(v.job); b.writeBoolean(v.working); b.writeVarInt(v.mask);
        b.writeOptional(v.a, FriendlyByteBuf::writeBlockPos); b.writeOptional(v.b, FriendlyByteBuf::writeBlockPos); b.writeOptional(v.post, FriendlyByteBuf::writeBlockPos);
        b.writeVarInt(v.breedCap); b.writeVarInt(v.storedXp); b.writeVarInt(v.treasureLeft); b.writeComponent(v.name); b.writeComponent(v.status);
    }
    private static View read(FriendlyByteBuf b) {
        return new View(b.readVarInt(), b.readUnsignedByte(), b.readBoolean(), b.readVarInt(),
                b.readOptional(FriendlyByteBuf::readBlockPos), b.readOptional(FriendlyByteBuf::readBlockPos), b.readOptional(FriendlyByteBuf::readBlockPos),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readComponent(), b.readComponent());
    }

    public static void open(ServerPlayer player, TamedRat rat) {
        BrickJob job = BrickJob.of(rat);
        BrickWork w = rat.work;
        BlockPos[] a = w.area(job);
        var view = new View(rat.getId(), job.ordinal(), w.working, w.mask(job), Optional.ofNullable(a == null ? null : a[0]), Optional.ofNullable(a == null ? null : a[1]),
                Optional.ofNullable(w.post), w.breedCap, w.storedXp, Math.max(0, BrickWork.TREASURE_PER_DAY - w.treasureToday), rat.getDisplayName(), w.status);
        RatsNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), view);
    }

    private static void onView(View v, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.github.alexthe666.rats.client.gui.BrickScepterScreen.show(v)));
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack scepter(ServerPlayer p) {
        for (InteractionHand h : InteractionHand.values()) if (p.getItemInHand(h).is(RatsItemRegistry.KING_SCEPTER.get())) return p.getItemInHand(h);
        return ItemStack.EMPTY;
    }

    private static void onEdit(Edit m, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (p == null || !(p.level().getEntity(m.rat) instanceof TamedRat rat) || !rat.isOwnedBy(p) || !rat.isBrick() || p.distanceTo(rat) > 32) return;
            BrickJob job = BrickJob.of(rat);
            if (job == BrickJob.NONE || job == BrickJob.CHEF) return;
            BrickWork w = rat.work;
            ItemStack stick = scepter(p);
            switch (m.action) {
                case 0 -> { if (w.working) w.stop(); else w.start(); }
                case 1 -> { String[] tasks = BrickWork.TASKS.get(job); if (tasks != null && m.value >= 0 && m.value < tasks.length) w.toggle(job, m.value); }
                case 2, 3 -> {
                    if (stick.isEmpty()) return;
                    KingScepterItem.beginSelect(stick, rat, job, m.action == 3 ? 1 : 0);
                    p.displayClientMessage(Component.translatable(m.action == 3 ? "brick.rats.scepter.pick_post" : "brick.rats.scepter.corner1"), true);
                    return;                                           // 客户端自己关界面，不回传
                }
                case 4 -> w.clearArea(job);
                case 5 -> w.breedCap = Math.max(2, Math.min(64, w.breedCap + m.value));
                default -> { return; }
            }
            if (!stick.isEmpty()) KingScepterItem.show(stick, rat, job);
            open(p, rat);
        });
        ctx.get().setPacketHandled(true);
    }
}
