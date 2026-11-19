package guildjane;

import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Weighted pose matrices in the same, player-height coordinate system as the imported mesh. */
final class JaneRig {
    private record Joint(String name,int parent,float x,float y,float z,Matrix4f pose,float[] values){}
    final Joint[] joints;
    final int[][] bones;
    final float[][] weights;
    final float[] palette;
    final int[][] cornerWeights;
    JaneRig(String outfit){
        var file=new ResourceLocation(GuildJane.ID,"meshes/"+outfit+".rig");
        try(var in=new DataInputStream(new BufferedInputStream(Minecraft.getInstance().getResourceManager().open(file)))){
            if(in.readInt()!=0x474a5231)throw new IOException("Unexpected rig header "+file);
            joints=new Joint[in.readInt()];
            for(int i=0;i<joints.length;i++)joints[i]=new Joint(in.readUTF(),in.readInt(),in.readFloat(),in.readFloat(),in.readFloat(),new Matrix4f(),new float[16]);
            int n=in.readInt();bones=new int[n][];weights=new float[n][];palette=new float[16*n];
            for(int i=0;i<n;i++){int k=in.readUnsignedByte();bones[i]=new int[k];weights[i]=new float[k];for(int j=0;j<k;j++){bones[i][j]=in.readUnsignedByte();weights[i][j]=in.readFloat();}}
            cornerWeights=new int[in.readInt()][];
            for(int i=0;i<cornerWeights.length;i++){int[] ids=cornerWeights[i]=new int[in.readInt()];for(int j=0;j<ids.length;j++)ids[j]=16*in.readUnsignedShort();}
        }catch(IOException ex){throw new UncheckedIOException(ex);}
    }
    static float sin(float t){return Mth.sin(t);}
    static float cos(float t){return Mth.cos(t);}
    static float pulse(float t){return t<0||t>1?0:sin(t*Mth.PI)*sin(t*Mth.PI);}
    void animate(GuildJane.Receptionist e,float partial,float bodyYaw){
        float t=e.tickCount+partial,offset=(e.getId()%23)*.37f;
        float breath=sin(t*.080f+offset),sway=sin(t*.041f+offset);
        float phase=e.walkAnimation.position(partial)*1.31f;
        float walk=Mth.clamp(e.walkAnimation.speed(partial)*3.5f,0,1);
        float talk=Mth.lerp(partial,e.gestureOld,e.gesture);
        float st=e.speechAge+partial;
        float present=talk*(.55f+.45f*sin(st*.125f-.6f));
        float mouth=talk*(.25f+.75f*Math.abs(sin(st*.85f)*cos(st*.21f)));
        float yaw=Mth.clamp(-Mth.wrapDegrees(Mth.rotLerp(partial,e.yHeadRotO,e.yHeadRot)-bodyYaw),-52,52);
        float pitch=Mth.clamp(-Mth.lerp(partial,e.xRotO,e.getXRot()),-22,24);
        for(Joint b:joints){
            String n=b.name;float x=0,y=0,z=0,dy=0,dx=0;
            int side=n.charAt(0)=='L'?1:-1;
            float step=side*sin(phase), swing=Math.max(0,side*cos(phase)), flex=walk*swing*swing;
            switch(n){
                case "hips" -> {dy=walk*(-.023f+.019f*cos(phase*2));dx=.019f*walk*step;z=1.8f*walk*sin(phase)+.5f*sway;y=3*walk*sin(phase);}
                case "waist" -> {x=.9f*breath+2*walk;z=-.7f*sway-1.8f*walk*sin(phase);}
                case "chest" -> {x=-1.3f*breath;dy=.004f*breath;y=-5*walk*sin(phase)-2*present;}
                case "neck" -> {x=.24f*pitch;y=.25f*yaw;}
                case "head" -> {x=.76f*pitch+.9f*breath+talk*(1.5f+3*sin(st*.18f));y=.75f*yaw+2*present;z=1.4f*sway+2*present;}
                case "Lcollar","Rcollar" -> {z=side*.7f*breath;}
                case "Larm","Rarm" -> {x=-step*19*walk+(side<0?14:6)*present;z=side*(1.4f*breath+5*present);}
                case "Lelbow","Relbow" -> {x=3+3*breath+6*walk*(1-step)+(side<0?48:14)*present;z=side*(side<0?13:5)*present;}
                case "Lhand","Rhand" -> {y=side*(5*walk*step+16*present);z=side*7*present;}
                case "Lthigh","Rthigh" -> {x=walk*(26*step+7*swing);z=side*walk*1.4f;}
                case "Lknee","Rknee" -> {x=-33*flex-4*walk*(1-step);}
                case "Lfoot","Rfoot" -> {x=walk*(-17*step)+16*flex;}
                case "Ltoe","Rtoe" -> {x=walk*8*Math.max(0,-step);}
                case "Lear","Rear","Learhair","Rearhair" -> {
                    float flick=pulse(((t+offset*19+(side>0?0:8))%89-13)/9);
                    float amount=n.endsWith("hair")?.52f:1;
                    z=side*amount*(3*breath+18*flick+6*present+3*walk*cos(phase));
                    x=amount*(2*sway+9*flick+4*walk*sin(phase-.6f));y=-yaw*.07f*amount;
                }
                case "mouth","Lmouth","Rmouth" -> {dy=-mouth*(n.equals("mouth")?.007f:.0027f);}
                default -> {
                    if(n.startsWith("tail")){
                        int i=n.charAt(4)-'0';
                        y=(i==1?5.5f:2.2f)*sin(t*.078f-i*.33f+offset)+walk*3.7f*sin(phase-i*.40f);
                        z=1.8f*cos(t*.067f-i*.30f+offset)+walk*1.1f*cos(phase-i*.36f);
                        x=1.2f*sin(t*.054f-i*.25f)+2.2f*present*cos(i*.32f);
                    }
                }
            }
            Matrix4f m=b.pose;if(b.parent<0)m.identity();else m.set(joints[b.parent].pose);
            m.translate(b.x+dx,b.y+dy,b.z).rotateXYZ(x*Mth.DEG_TO_RAD,y*Mth.DEG_TO_RAD,z*Mth.DEG_TO_RAD).translate(-b.x,-b.y,-b.z).get(b.values);
        }
        Arrays.fill(palette,0);
        for(int i=0;i<bones.length;i++){
            int start=16*i;
            for(int j=0;j<bones[i].length;j++){
                float weight=weights[i][j];float[] a=joints[bones[i][j]].values;
                for(int k=0;k<15;k++)palette[start+k]+=a[k]*weight;
            }
        }
    }
}
