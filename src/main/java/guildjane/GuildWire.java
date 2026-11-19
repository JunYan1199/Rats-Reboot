package guildjane;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

class GuildWire {
    static final SimpleChannel NET=NetworkRegistry.newSimpleChannel(new ResourceLocation("guildjane","talk"),()->"3","3"::equals,"3"::equals);
    record Page(UUID token,String title,String line,String voice,String layout,List<String> labels) {
        void encode(FriendlyByteBuf b){b.writeUUID(token);b.writeUtf(title);b.writeUtf(line);b.writeUtf(voice);b.writeUtf(layout);b.writeCollection(labels,(buf,s)->buf.writeUtf(s));}
        static Page read(FriendlyByteBuf b){return new Page(b.readUUID(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readList(buf->buf.readUtf()));}
    }
    record Pick(UUID token,int index){}
    static void register(){
        NET.messageBuilder(Page.class,0,NetworkDirection.PLAY_TO_CLIENT)
          .encoder(Page::encode).decoder(Page::read).consumerMainThread((msg,ctx)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->JaneScreen.show(msg))).add();
        NET.messageBuilder(Pick.class,1,NetworkDirection.PLAY_TO_SERVER)
          .encoder((m,b)->{b.writeUUID(m.token);b.writeVarInt(m.index);})
          .decoder(b->new Pick(b.readUUID(),b.readVarInt())).consumerMainThread((msg,ctx)->Conversation.choose(ctx.get().getSender(),msg)).add();
    }
    static void send(ServerPlayer p,Page page){NET.send(PacketDistributor.PLAYER.with(()->p),page);}
}
