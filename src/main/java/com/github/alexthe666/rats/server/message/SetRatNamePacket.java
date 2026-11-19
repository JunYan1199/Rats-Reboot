package com.github.alexthe666.rats.server.message;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;import net.minecraft.network.FriendlyByteBuf;import net.minecraftforge.network.NetworkEvent;import java.util.function.Supplier;
public record SetRatNamePacket(int entityId,String name) {
 public static void encode(SetRatNamePacket p,FriendlyByteBuf b){b.writeInt(p.entityId);b.writeUtf(p.name,48);} public static SetRatNamePacket decode(FriendlyByteBuf b){return new SetRatNamePacket(b.readInt(),b.readUtf(48));}
 public static void handle(SetRatNamePacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{var player=c.get().getSender();if(player!=null&&player.level().getEntity(p.entityId) instanceof TamedRat rat&&rat.isOwnedBy(player)&&rat.distanceToSqr(player)<4096)rat.setRatGivenName(p.name);});c.get().setPacketHandled(true);}}
