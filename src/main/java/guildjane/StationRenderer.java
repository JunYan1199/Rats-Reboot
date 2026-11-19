package guildjane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=GuildJane.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class StationRenderer implements BlockEntityRenderer<PowerStation.Unit>{
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(PowerStation.TYPE.get(),ctx->new StationRenderer());}
    private Level world;private LivingEntity rat;
    private void lettering(PoseStack pose,MultiBufferSource buf,String text,double x,double y,double z,double width,double height){
        var font=Minecraft.getInstance().font;float scale=(float)Math.min(width/(16*font.width(text)),height/(16*9));
        pose.pushPose();pose.translate(x/16,y/16,z/16);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));pose.scale(scale,-scale,scale);
        font.drawInBatch(text,-font.width(text)/2f,-4.5f,0xff8dfff3,false,pose.last().pose(),buf,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,15728880);
        pose.popPose();
    }
    public void render(PowerStation.Unit u,float partial,PoseStack pose,MultiBufferSource buf,int light,int overlay){
        int load=u.getBlockState().getValue(PowerStation.LOAD);
        String label=new String[]{"待机","群友","皮卡丘"}[load];
        lettering(pose,buf,label,16,26.1,1.35,10,2.2);
        lettering(pose,buf,label,29,11.55,4.72,4.1,1.7);
        lettering(pose,buf,label,5.0,4.95,-.10,5.7,2.2);
        if(load!=2)return;
        if(world!=u.getLevel()){world=u.getLevel();rat=null;}
        if(rat==null){var r=RatsEntityRegistry.TAMED_RAT.get().create(world);var data=r.saveWithoutId(new net.minecraft.nbt.CompoundTag());data.putString("RatEvolution","spark");r.readAdditionalSaveData(data);rat=r;}
        var mob=rat;if(mob==null)return;
        mob.yBodyRot=mob.yBodyRotO=mob.yHeadRot=mob.yHeadRotO=168;mob.setYRot(168);mob.yRotO=168;
        pose.pushPose();pose.translate(1.04,.32,.86);float scale=1.18f;pose.scale(scale,scale,scale);
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mob).render(mob,168,partial,pose,buf,light);pose.popPose();
    }
}
