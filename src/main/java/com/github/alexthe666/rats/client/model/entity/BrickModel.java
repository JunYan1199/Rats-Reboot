package com.github.alexthe666.rats.client.model.entity;

import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.brick.BrickJob;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import org.joml.Quaternionf;
import java.io.*;
import java.util.*;

/**
 * 布瑞克模型：mesh.bin（体素外表面面片）+ clips.bin（逐帧部件变换）。
 * clips.bin v2（首个 int = -2）按名字存全部动画段，并记录每只手持物的握点/缩放；旧格式（4 段）仍可读。
 */
public final class BrickModel extends EntityModel<TamedRat> {
    public static final ResourceLocation TEXTURE=new ResourceLocation("rats","textures/entity/brick.png");
    private record Face(byte nx,byte ny,byte nz,float[] vertices){}
    private record Transform(float x,float y,float z,Quaternionf rotation){}
    /** 手持规则：def = 预览里的物品定义名（空 = 用原版第三人称手持）；pivot 为贴图握点像素，scale 为每像素大小 */
    public record Held(String def,float px,float py,float scale){ boolean custom(){return scale>0&&!def.equals("spatula")&&!def.equals("converter");} }
    private record Clip(float duration,Held right,Held left,Transform[][] frames){}
    private static final Held NO_HELD=new Held("",8,8,0);
    private static final String[] LEGACY={"待机","走路","做饭·翻炒","展示·转化装置"};
    private final Map<String,Face[]> meshes=new LinkedHashMap<>();
    private final Map<String,Integer> tracks=new HashMap<>();
    private final Map<String,Clip> clips=new HashMap<>();
    private Clip clip;
    private Transform[] from,to;
    private float blend;
    private String hat;
    private boolean spatula,converter;

    public BrickModel(ResourceManager manager){
        try(var mesh=stream(manager,"mesh");var anim=stream(manager,"clips")){
            int parts=mesh.readInt();
            for(int i=0;i<parts;i++){
                String name=mesh.readUTF();Face[] faces=new Face[mesh.readInt()];
                for(int n=0;n<faces.length;n++){
                    byte x=mesh.readByte(),y=mesh.readByte(),z=mesh.readByte();float[] verts=new float[20];
                    for(int j=0;j<20;j++)verts[j]=mesh.readFloat();
                    faces[n]=new Face(x,y,z,verts);
                }
                meshes.put(name,faces);
            }
            int head=anim.readInt();boolean v2=head==-2;
            int count=v2?anim.readInt():head;
            for(int i=1;i<=count;i++)tracks.put(anim.readUTF(),i);
            int clipCount=v2?anim.readInt():LEGACY.length;
            for(int i=0;i<clipCount;i++){
                String name=v2?anim.readUTF():LEGACY[i];
                float dur=anim.readFloat();
                Held r=NO_HELD,l=NO_HELD;
                if(v2){r=new Held(anim.readUTF(),anim.readFloat(),anim.readFloat(),anim.readFloat());l=new Held(anim.readUTF(),anim.readFloat(),anim.readFloat(),anim.readFloat());}
                Transform[][] frames=new Transform[anim.readInt()][count+1];
                for(var frame:frames)for(int j=0;j<frame.length;j++)
                    frame[j]=new Transform(anim.readFloat(),anim.readFloat(),anim.readFloat(),new Quaternionf(anim.readFloat(),anim.readFloat(),anim.readFloat(),anim.readFloat()));
                clips.put(name,new Clip(dur,r,l,frames));
            }
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static DataInputStream stream(ResourceManager resources,String name) throws IOException {
        return new DataInputStream(new BufferedInputStream(resources.open(new ResourceLocation("rats","brick/"+name+".bin"))));
    }

    private static boolean melee(ItemStack s){return s.getItem() instanceof SwordItem||s.getItem() instanceof AxeItem||s.getItem() instanceof TridentItem;}

    /** 按状态挑动画段 */
    private String pick(TamedRat rat,float speed){
        ItemStack main=rat.getMainHandItem();
        BrickJob job=BrickJob.of(rat);
        if(rat.isBrickCooking())return "做饭·翻炒";
        switch(rat.getBrickAction()){
            case BrickJob.Action.HOE:return "农夫·锄地";
            case BrickJob.Action.SHEAR:return "牧场主·剪毛";
            case BrickJob.Action.MILK:return "牧场主·挤奶";
            case BrickJob.Action.DIG:return "寻宝·挖宝";
            case BrickJob.Action.PRESENT:return "寻宝·献宝";
            case BrickJob.Action.BLOCK:return "剑盾·格挡刺击";
            default:break;
        }
        if(rat.attackAnim>0&&melee(main))return "持剑·挥砍";
        if(main.getItem() instanceof ProjectileWeaponItem&&rat.isAggressive())return "持弓·拉弓射击";
        if(speed>0.025F)return "走路";
        if(main.is(RatsItemRegistry.BRICK_CONVERTER.get()))return "展示·转化装置";
        if(melee(main)&&(job==BrickJob.GUARD||job==BrickJob.SENTRY))return "持剑·待机";
        return "待机";
    }

    @Override public void setupAnim(TamedRat rat,float limb,float speed,float age,float yaw,float pitch){
        String name=pick(rat,speed);
        if(!clips.containsKey(name))name="待机";      // 旧资源里没有的段退回待机
        clip=clips.get(name);
        BrickJob job=BrickJob.of(rat);
        spatula=rat.isBrickCooking()||rat.getMainHandItem().is(RatsItemRegistry.BRICK_SPATULA.get());
        converter=rat.getMainHandItem().is(RatsItemRegistry.BRICK_CONVERTER.get())&&!spatula;
        hat=job!=BrickJob.NONE?job.mesh:rat.isBrickCooking()?"hat":null;
        float clock=name.equals("走路")?limb*0.19F:name.equals("持剑·挥砍")?rat.attackAnim*clip.duration:age/20F;
        float t=(clock/clip.duration)%1F;if(t<0)t+=1;
        float sample=t*clip.frames.length;
        int frame=(int)sample%clip.frames.length;
        from=clip.frames[frame];to=clip.frames[(frame+1)%clip.frames.length];blend=sample-(int)sample;
    }
    private void apply(PoseStack p,int i){
        Transform a=from[i],b=to[i];
        p.translate(a.x+(b.x-a.x)*blend,a.y+(b.y-a.y)*blend,a.z+(b.z-a.z)*blend);
        p.mulPose(new Quaternionf(a.rotation).slerp(b.rotation,blend));
    }
    private void root(PoseStack p){
        p.translate(0,1.501,0);p.scale(0.042F,-0.042F,-0.042F);
        apply(p,0);p.translate(0,-6,0);
    }
    /** 把姿势栈移到手上；返回 true = 按预览规则摆（用 NONE 渲染），false = 原版第三人称手持 */
    public boolean hand(PoseStack p,boolean left){
        root(p);apply(p,tracks.get(left?"grip_l":"grip_r"));
        Held h=left?clip.left:clip.right;
        if(h.custom()){
            // 预览规则：贴图像素 (x,y) → ((x+0.5-px)*s, -(y+0.5-py)*s)；NONE 渲染时物品占 [-0.5,0.5]，16 像素
            p.translate((8-h.px)*h.scale,(h.py-8)*h.scale,0);
            p.scale(16*h.scale,16*h.scale,16*h.scale);
            return true;
        }
        p.scale(11,11,11);
        return false;
    }
    public boolean modeledMainHand(){return spatula||converter;}
    @Override public void renderToBuffer(PoseStack p,VertexConsumer sink,int light,int overlay,float r,float g,float b,float a){
        p.pushPose();root(p);
        for(var entry:meshes.entrySet()){
            String n=entry.getKey();
            if(n.startsWith("hat")&&!n.equals(hat)||n.equals("spatula")&&!spatula||n.equals("converter")&&!converter)continue;
            Integer track=tracks.get(n.equals("spatula")||n.equals("converter")?"grip_r":n);
            if(track==null)continue;
            p.pushPose();apply(p,track);
            var pose=p.last();
            for(Face f:entry.getValue())for(int i=0;i<20;i+=5){
                float[] v=f.vertices;sink.vertex(pose.pose(),v[i],v[i+1],v[i+2]).color(r,g,b,a).uv(v[i+3],v[i+4]).overlayCoords(overlay).uv2(light).normal(pose.normal(),f.nx,f.ny,f.nz).endVertex();
            }
            p.popPose();
        }
        p.popPose();
    }
}
