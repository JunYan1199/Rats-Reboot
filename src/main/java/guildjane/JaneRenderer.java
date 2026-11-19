package guildjane;
import java.io.*;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=GuildJane.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class JaneRenderer extends EntityRenderer<GuildJane.Receptionist> {
    record Part(ResourceLocation texture,float[] vertices){}
    final List<Part> standard=load("standard"),nocturne=load("nocturne");
    final JaneRig dayRig=new JaneRig("standard"),nightRig=new JaneRig("nocturne");
    static List<Part> load(String name){
        var path=new ResourceLocation(GuildJane.ID,"meshes/"+name+".bin");
        try(var in=new DataInputStream(new BufferedInputStream(Minecraft.getInstance().getResourceManager().open(path)))){
            var parts=new ArrayList<Part>();int count=in.readInt();
            for(int i=0;i<count;i++){
                var texture=new ResourceLocation(GuildJane.ID,"textures/entity/"+in.readUTF()+".png");
                float[] vertices=new float[in.readInt()*8];
                for(int j=0;j<vertices.length;j++)vertices[j]=in.readFloat();
                parts.add(new Part(texture,vertices));
            }
            return parts;
        }catch(IOException ex){throw new UncheckedIOException(ex);}
    }
    public JaneRenderer(EntityRendererProvider.Context ctx){super(ctx);shadowRadius=.26f;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(GuildJane.NPC.get(),JaneRenderer::new);}
    @Override public ResourceLocation getTextureLocation(GuildJane.Receptionist e){return standard.get(0).texture();}
    @Override public void render(GuildJane.Receptionist e,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){
        float bodyYaw=net.minecraft.util.Mth.rotLerp(partial,e.yRotO,e.getYRot());
        ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-bodyYaw));
        if(e.flourish>0)ps.mulPose(Axis.YP.rotationDegrees(360f*(22-e.flourish+partial)/22f));
        var mat=ps.last();
        boolean night=e.getEntityData().get(GuildJane.Receptionist.NIGHT);JaneRig rig=night?nightRig:dayRig;
        rig.animate(e,partial,bodyYaw);float[] a=rig.palette;int partIndex=0;
        for(var part:night?nocturne:standard){
            var out=buffers.getBuffer(RenderType.entityCutoutNoCull(part.texture));float[] v=part.vertices;
            int[] ids=rig.cornerWeights[partIndex++];
            for(int i=0;i<v.length;i+=24){
                for(int k=0;k<4;k++){int j=i+Math.min(k,2)*8;
                    int s=ids[j/8];float x=-v[j],y=v[j+1],z=v[j+2],nx=-v[j+5],ny=v[j+6],nz=v[j+7];
                    out.vertex(mat.pose(),a[s]*x+a[s+4]*y+a[s+8]*z+a[s+12],a[s+1]*x+a[s+5]*y+a[s+9]*z+a[s+13],a[s+2]*x+a[s+6]*y+a[s+10]*z+a[s+14]).color(255,255,255,255).uv(v[j+3],v[j+4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                       .normal(mat.normal(),a[s]*nx+a[s+4]*ny+a[s+8]*nz,a[s+1]*nx+a[s+5]*ny+a[s+9]*nz,a[s+2]*nx+a[s+6]*ny+a[s+10]*nz).endVertex();
                }
            }
        }
        ps.popPose();super.render(e,yaw,partial,ps,buffers,light);
    }
}
