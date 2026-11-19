package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.server.entity.rat.RatEvolution;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;
import java.util.Optional;
import java.util.function.Supplier;

public record EvolutionEffectPacket(Kind kind, RatEvolution form, Vec3 from, Vec3 to, int tint, float radius, boolean charged, long seed) {
    public enum Kind { ARC, BURST, WARD, STREAM }

    public static void register(int id) {
        RatsNetworkHandler.CHANNEL.registerMessage(id, EvolutionEffectPacket.class, EvolutionEffectPacket::write,
                EvolutionEffectPacket::read, EvolutionEffectPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public void send(ServerLevel world) {
        var target = new PacketDistributor.TargetPoint(to.x, to.y, to.z, 48, world.dimension());
        RatsNetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(() -> target), this);
    }
    private static void write(EvolutionEffectPacket fx, FriendlyByteBuf buf) {
        buf.writeEnum(fx.kind); buf.writeEnum(fx.form); writePosition(buf,fx.from); writePosition(buf,fx.to);
        buf.writeInt(fx.tint); buf.writeFloat(fx.radius); buf.writeBoolean(fx.charged); buf.writeLong(fx.seed);
    }
    private static void writePosition(FriendlyByteBuf b, Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    private static Vec3 readPosition(FriendlyByteBuf b) { return new Vec3(b.readDouble(),b.readDouble(),b.readDouble()); }
    private static EvolutionEffectPacket read(FriendlyByteBuf buf) {
        return new EvolutionEffectPacket(buf.readEnum(Kind.class),buf.readEnum(RatEvolution.class),readPosition(buf),readPosition(buf),
                buf.readInt(),buf.readFloat(),buf.readBoolean(),buf.readLong());
    }
    private static void handle(EvolutionEffectPacket fx, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> com.github.alexthe666.rats.client.events.EvolutionEffectRenderer.add(fx));
        context.get().setPacketHandled(true);
    }
}

