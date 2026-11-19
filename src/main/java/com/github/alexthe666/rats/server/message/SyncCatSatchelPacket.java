package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.server.catcatch.CatSatchel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 -> 客户端：同步猫是否佩戴捕鼠挎包（用于渲染挎包外观和客户端交互判断）。 */
public record SyncCatSatchelPacket(int entityId, boolean equipped) {

	public static SyncCatSatchelPacket decode(FriendlyByteBuf buf) {
		return new SyncCatSatchelPacket(buf.readVarInt(), buf.readBoolean());
	}

	public static void encode(SyncCatSatchelPacket packet, FriendlyByteBuf buf) {
		buf.writeVarInt(packet.entityId());
		buf.writeBoolean(packet.equipped());
	}

	public static class Handler {
		@SuppressWarnings("Convert2Lambda")
		public static void handle(SyncCatSatchelPacket packet, Supplier<NetworkEvent.Context> context) {
			context.get().enqueueWork(new Runnable() {
				@Override
				public void run() {
					if (Minecraft.getInstance().level == null) return;
					Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
					CatSatchel satchel = CatSatchel.get(entity);
					if (satchel != null) satchel.setEquipped(packet.equipped());
				}
			});
			context.get().setPacketHandled(true);
		}
	}
}
