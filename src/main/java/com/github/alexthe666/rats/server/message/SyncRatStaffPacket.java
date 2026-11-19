package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncRatStaffPacket(int entityId, long pos, int facing, int control, int extraData) {

	public SyncRatStaffPacket(int entityId, BlockPos pos, Direction facing, int control) {
		this(entityId, pos.asLong(), facing.ordinal(), control, 0);
	}

	public SyncRatStaffPacket(int entityId, BlockPos pos, Direction facing, int control, int extraData) {
		this(entityId, pos.asLong(), facing.ordinal(), control, extraData);
	}

	public static SyncRatStaffPacket decode(FriendlyByteBuf buf) {
		return new SyncRatStaffPacket(buf.readInt(), buf.readLong(), buf.readInt(), buf.readInt(), buf.readInt());
	}

	public static void encode(SyncRatStaffPacket packet, FriendlyByteBuf buf) {
		buf.writeInt(packet.entityId());
		buf.writeLong(packet.pos());
		buf.writeInt(packet.facing());
		buf.writeInt(packet.control());
		buf.writeInt(packet.extraData());
	}

	public static class Handler {
		public static void handle(SyncRatStaffPacket packet, Supplier<NetworkEvent.Context> context) {
			context.get().enqueueWork(() -> {
				Player player = context.get().getSender();
				if (player != null) {
					Entity e = player.level().getEntity(packet.entityId());
					if (e instanceof TamedRat rat && rat.isOwnedBy(player) && rat.distanceToSqr(player)<4096 && packet.facing()>=0 && packet.facing()<6) {
						switch (packet.control) {
case 8 -> rat.setWorkAnchorPoint(GlobalPos.of(player.level().dimension(),BlockPos.of(packet.pos())));
case 9 -> rat.setWorkAnchorPoint(null);
case 10 -> { if(player.level().getBlockEntity(BlockPos.of(packet.pos())) instanceof com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity house) house.bindRat(rat,player); }
case 11 -> { rat.getBoundHousePos().ifPresent(p -> { if(p.dimension().equals(player.level().dimension()) && player.level().getBlockEntity(p.pos()) instanceof com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity house) house.clearBinding(rat,player); }); rat.setBoundHousePos(null); }
case 12 -> {rat.setCreateWorkPos(GlobalPos.of(player.level().dimension(),BlockPos.of(packet.pos())));rat.createWorkFacing=Direction.values()[packet.facing()];}
case 13 -> rat.setCreateWorkPos(null);
case 14 -> {
    BlockPos target=BlockPos.of(packet.pos());
    if(rat.isBrick() && player.distanceToSqr(target.getCenter())<=64 && player.level().hasChunkAt(target)){
        var dest=player.level().getBlockEntity(target);Direction face=Direction.values()[packet.facing()];
        if(dest!=null&&dest.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,face).isPresent()){
            rat.kitchen.bind(target,face);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("brick.rats.bound"),true);
        }
    }
}
							case 0 -> {//deposit
								rat.setDepositPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.depositFacing = Direction.values()[packet.facing()];
							}
							case 1 -> {//pickup
								rat.setPickupPos(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
								rat.pickupFacing = Direction.values()[packet.facing()];
							}
							case 2 ->//set homepoint
								rat.setHomePoint(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
							case 3 ->//detach homepoint
								rat.setHomePoint(null);
							case 4 ->//set radius home point
								rat.setRadiusCenter(GlobalPos.of(player.level().dimension(), BlockPos.of(packet.pos())));
							case 5 ->//set radius scale
								rat.setRadius(packet.extraData());
							case 6 -> {//reset radius
								rat.setRadiusCenter(null);
								rat.setRadius(RatConfig.defaultRatRadius);
							}
							case 7 -> { //reset deposit and pickup
								rat.setPickupPos(null);
								rat.setDepositPos(null);
							}
						}
					}
				}
			});
			context.get().setPacketHandled(true);
		}
	}
}
