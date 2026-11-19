package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.server.message.EvolutionEffectPacket;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.ArrayDeque;
import java.util.SplittableRandom;

/** Camera-facing ribbons around real skill activations, evaluated only on nearby clients. */
@Mod.EventBusSubscriber(modid="rats",value=Dist.CLIENT)
public final class EvolutionEffectRenderer {
    private record Flash(EvolutionEffectPacket fx,long start,int life) {}
    private static final ArrayDeque<Flash> flashes = new ArrayDeque<>();
    private static ClientLevel scene;
    private static final double TAU = 2 * Math.PI;

    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload e) {
        if(e.getLevel()==scene){flashes.clear();scene=null;}
    }

    public static void add(EvolutionEffectPacket fx) {
        var mc=Minecraft.getInstance();
        if (mc.level==null) return;
        if (scene!=mc.level) { flashes.clear(); scene=mc.level; }
        int cap=switch(mc.options.particles().get()){case MINIMAL->8;case DECREASED->20;default->40;};
        while(flashes.size()>=cap)flashes.removeFirst();
        int life=switch(fx.kind()){case ARC->fx.charged()?15:10;case BURST->13;case WARD->22;case STREAM->18;};
        flashes.addLast(new Flash(fx,scene.getGameTime(),life));
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || flashes.isEmpty()) return;
        var mc=Minecraft.getInstance();
        if(mc.level!=scene){flashes.clear();scene=null;return;}
        long tick=scene.getGameTime();
        flashes.removeIf(f->tick<f.start || tick-f.start>=f.life);
        if(flashes.isEmpty())return;
        var source=mc.renderBuffers().bufferSource();
        var renderType=RenderType.lightning();
        var brush=new Brush(source.getBuffer(renderType),event.getPoseStack().last().pose(),event.getCamera().getPosition());
        for(Flash flash:flashes){
            double t=Mth.clamp((tick-flash.start+event.getPartialTick())/flash.life,0,1);
            switch(flash.fx.kind()){
                case ARC -> lightning(brush,flash.fx,t);
                case WARD -> ward(brush,flash.fx,t);
                case BURST -> impact(brush,flash.fx,t);
                case STREAM -> flow(brush,flash.fx,t);
            }
        }
        source.endBatch(renderType);
    }

    private record Brush(VertexConsumer buffer,Matrix4f pose,Vec3 camera) {
        void point(Vec3 p,int color,int alpha){
            Vec3 v=p.subtract(camera);
            buffer.vertex(pose,(float)v.x,(float)v.y,(float)v.z).color(color>>16&255,color>>8&255,color&255,alpha).endVertex();
        }
        void stroke(Vec3 a,Vec3 b,double width,int color,double opacity){
            int alpha=(int)(255*Mth.clamp(opacity,0,1));
            if(alpha<2)return;
            Vec3 offset=b.subtract(a).cross(camera.subtract(a)).normalize().scale(width);
            if(offset.lengthSqr()<1e-10)return;
            point(a.add(offset),color,alpha);point(b.add(offset),color,alpha);
            point(b.subtract(offset),color,alpha);point(a.subtract(offset),color,alpha);
        }
        void glow(Vec3 a,Vec3 b,double width,int tint,double power) {
            stroke(a,b,width*3.1,tint,power*.17);
            stroke(a,b,width*1.55,tint,power*.55);
            stroke(a,b,width*.6,0xFFF7E7,power*.9);
        }
    }

    private static Vec3 polar(Vec3 p,double radius,double angle,double y){
        return p.add(radius*Math.cos(angle),y,radius*Math.sin(angle));
    }
    private static void circle(Brush b,Vec3 p,double radius,double start,double span,double height,double width,int tint,double alpha){
        int segments=Math.max(4,(int)Math.ceil(28*Math.abs(span)/TAU));
        Vec3 prev=polar(p,radius,start,height);
        for(int i=1;i<=segments;i++){
            Vec3 next=polar(p,radius,start+span*i/segments,height);
            b.stroke(prev,next,width,tint,alpha);prev=next;
        }
    }
    private static void lightning(Brush b,EvolutionEffectPacket fx,double t){
        Vec3 axis=fx.to().subtract(fx.from());
        double length=axis.length();
        if(length<.06){impact(b,fx,t);return;}
        Vec3 forward=axis.normalize();
        Vec3 side=perpendicular(forward),up=side.cross(forward).normalize();
        boolean high=fx.charged();
        // Two stable silhouettes give a second discharge without randomizing every frame.
        var random=new SplittableRandom(fx.seed()+(t>.43?17:0));
        int count=Mth.clamp((int)(length*2.8),5,16);
        double power=(1-t)*(t>.32 && t<.43?.35:1);
        double reach=Math.min(1,t*9+.16);
        Vec3 last=fx.from();
        for(int i=1;i<=count;i++){
            double u=(double)i/count, jitter=Math.sin(Math.PI*u)*Math.min(.46,length*.13);
            Vec3 end=fx.from().add(axis.scale(u)).add(side.scale(random.nextDouble(-1,1)*jitter)).add(up.scale(random.nextDouble(-1,1)*jitter));
            if((i-1.)/count>=reach)break;
            Vec3 tip=last.lerp(end,Math.min(1,reach*count-i+1));
            b.glow(last,tip,high?.028:.018,fx.tint(),power);
            if(i<count && i%(high?2:3)==0 && reach>=u){
                double sign=i%4==0?-1:1;
                Vec3 bend=end.add(side.scale(sign*jitter*.65)).add(up.scale(.10+jitter*.45));
                Vec3 fork=bend.add(forward.scale(.26)).add(side.scale(sign*.17));
                b.glow(end,bend,.012,fx.tint(),power*.82);b.stroke(bend,fork,.014,fx.tint(),power*.75);
            }
            last=end;
        }
        if(t>.13){
            double u=(t-.13)/.87, radius=(high?1.05:.50)*Math.sqrt(u);
            for(int i=0;i<3;i++)circle(b,fx.to(),radius,TAU*i/3+u,1.55,-.15+u*.12,.019,fx.tint(),(1-u)*.8);
            if(high)impact(b,fx,u);
        }
    }
    private static Vec3 perpendicular(Vec3 v){
        return v.cross(Math.abs(v.y)>.92?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
    }
    private static void impact(Brush b,EvolutionEffectPacket fx,double t){
        crest(b,fx,t,.45);
        var rand=new SplittableRandom(fx.seed());
        int count=switch(fx.form()){case EMBER,DRAKE,SPARK->11;case FROST,CRAG,ALLOY->8;default->6;};
        for(int i=0;i<count;i++){
            double a=TAU*(i+rand.nextDouble()*.6)/count;
            Vec3 dir=new Vec3(Math.cos(a),rand.nextDouble(-.18,1.05),Math.sin(a)).normalize();
            double dist=(.22+Math.sqrt(t)*.75)*rand.nextDouble(.7,1.35);
            Vec3 head=fx.to().add(dir.scale(dist)).add(0,-t*t*.18,0);
            b.glow(head.subtract(dir.scale(.22*(1-t)+.05)),head,.017,fx.tint(),1-t);
        }
    }
    // Local ornaments are oriented toward the camera, so the pattern stays legible from overhead.
    private static void crest(Brush b,EvolutionEffectPacket fx,double t,double scale) {
        Vec3 p=fx.to().add(0,.18+.32*t,0), right=perpendicular(p.subtract(b.camera).normalize());
        Vec3 up=right.cross(p.subtract(b.camera).normalize()).normalize();
        if(up.y<0)up=up.scale(-1);
        double alpha=Math.sin(Math.PI*Math.min(1,t*5)/2)*(1-t);
        switch(fx.form()){
            case FROST -> {
                for(int i=0;i<6;i++){
                    double angle=i*Math.PI/3+.12*t;
                    Vec3 dir=right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                    Vec3 cross=right.scale(-Math.sin(angle)).add(up.scale(Math.cos(angle)));
                    Vec3 mid=p.add(dir.scale(scale*.60)),tip=p.add(dir.scale(scale));
                    b.glow(p,tip,.012,fx.tint(),alpha);
                    for(int sign:new int[]{-1,1})b.stroke(mid,mid.add(dir.scale(scale*.19)).add(cross.scale(scale*.19*sign)),.014,fx.tint(),alpha);
                }
            }
            case EMBER, DRAKE -> {
                for(int i=0;i<5;i++){
                    double a=TAU*i/5+t;
                    Vec3 foot=polar(p,scale*.83,a,-.10);
                    Vec3 curl=polar(p,scale*.69,a+.28,.08+.24*t);
                    Vec3 tip=polar(p,scale*.57,a+.46,.30+.42*t);
                    b.glow(foot,curl,.026,fx.tint(),alpha);b.glow(curl,tip,.014,fx.tint(),alpha);
                }
            }
            case RIBBON, SEER, WISP, LUNAR -> {
                for(int i=0;i<3;i++){
                    Vec3 star=polar(p,scale*(.8+.15*t),TAU*i/3+t,.16+.16*Math.sin(i+t*4));
                    b.glow(star.subtract(right.scale(.09)),star.add(right.scale(.09)),.013,fx.tint(),alpha);
                    b.glow(star.subtract(up.scale(.15)),star.add(up.scale(.15)),.013,fx.tint(),alpha);
                }
            }
            default -> {}
        }
    }

    private static void ward(Brush b,EvolutionEffectPacket fx,double t){
        double radius=fx.radius()*(.38+.65*Math.min(1,t*4))*(1+.1*t),alpha=Math.sin(Math.PI*Math.min(1,t*4)/2)*(1-t);
        Vec3 p=fx.to();
        crest(b,fx,t,radius*.80);
        circle(b,p,radius,t*1.5,TAU,.025,.023,fx.tint(),alpha);
        for(int i=0;i<4;i++){
            double a=TAU*i/4+t;
            circle(b,p,radius*.78,a,1.08,.06,.012,fx.tint(),alpha);
            Vec3 base=polar(p,radius,a,.08),top=base.add(0,.25+radius*.25,0);
            Vec3 left=polar(p,radius,a-.14,.18),right=polar(p,radius,a+.14,.18);
            b.glow(base,left,.012,fx.tint(),alpha);b.glow(left,top,.012,fx.tint(),alpha);
            b.glow(top,right,.012,fx.tint(),alpha);b.glow(right,base,.012,fx.tint(),alpha);
        }
    }
    private static void flow(Brush b,EvolutionEffectPacket fx,double t){
        Vec3 d=fx.to().subtract(fx.from());if(d.lengthSqr()<.04){ward(b,fx,t);return;}
        Vec3 f=d.normalize(),side=perpendicular(f),up=side.cross(f).normalize();
        boolean breath=fx.form()==com.github.alexthe666.rats.server.entity.rat.RatEvolution.DRAKE;
        boolean ground=fx.form()==com.github.alexthe666.rats.server.entity.rat.RatEvolution.DUNE;
        int strands=breath?3:2;
        for(int s=0;s<strands;s++){
            Vec3 prev=null;
            for(int i=0;i<=18;i++){
                double u=Math.max(0,t*1.4-.42)+i/18.*(Math.min(1,t*1.7+.07)-Math.max(0,t*1.4-.42));
                double phase=u*TAU*(breath?1.3:.9)+s*TAU/strands-t*2;
                double radius=breath?.08+.52*u:.22*Math.sin(Math.PI*u);
                Vec3 p=fx.from().add(d.scale(u)).add(side.scale(ground?Math.sin(i*2.5+s)*.18:Math.cos(phase)*radius))
                    .add(up.scale(ground?0:Math.sin(phase)*radius));
                if(prev!=null)b.glow(prev,p,breath?.031:.018,fx.tint(),1-t*.8);prev=p;
            }
        }
        if(t>.48)crest(b,fx,(t-.48)/.52,.42);
        if(t>.53)circle(b,fx.to(),.15+(t-.53)*1.25,t,TAU,0,.016,fx.tint(),(1-t)*1.5);
    }
}

