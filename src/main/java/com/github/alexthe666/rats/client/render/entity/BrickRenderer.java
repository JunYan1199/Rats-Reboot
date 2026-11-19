package com.github.alexthe666.rats.client.render.entity;
import com.github.alexthe666.rats.client.model.entity.BrickModel;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public final class BrickRenderer extends MobRenderer<TamedRat,BrickModel> {
    private final TamedRatRenderer original;
    public BrickRenderer(EntityRendererProvider.Context context,TamedRatRenderer original){
        super(context,new BrickModel(context.getResourceManager()),0.25F);
        this.original=original;
        addLayer(new RenderLayer<TamedRat,BrickModel>(this){
            @Override public void render(PoseStack p,MultiBufferSource buffers,int light,TamedRat rat,float limb,float speed,float partial,float age,float yaw,float pitch){
                for(boolean left:new boolean[]{false,true}){
                    var item=left?rat.getOffhandItem():rat.getMainHandItem();
                    if(item.isEmpty()||!left&&getModel().modeledMainHand())continue;
                    p.pushPose();
                    // 动画段里记录了这只手的握点/缩放 → 按预览规则摆，用 NONE 画；否则用原版第三人称手持
                    boolean custom=getModel().hand(p,left);
                    // 盾牌走特殊渲染器：它会先平移 -0.5 再 scale(1,-1,-1)，这里先抵消掉，盾面才落在手上、朝前
                    if(custom&&item.getItem() instanceof net.minecraft.world.item.ShieldItem){p.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));p.scale(1,-1,-1);p.translate(0.5,0.5,0.5);}   // 转 180° 让盾面朝外
                    ItemDisplayContext ctx=custom?ItemDisplayContext.NONE:left?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                    Minecraft.getInstance().getItemRenderer().renderStatic(rat,item,ctx,left,p,buffers,rat.level(),light,LivingEntityRenderer.getOverlayCoords(rat,0),rat.getId());
                    p.popPose();
                }
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(TamedRat rat){return BrickModel.TEXTURE;}
    @Override protected boolean shouldShowName(TamedRat rat){return original.shouldShowName(rat);}
    @Override protected void scale(TamedRat rat,PoseStack pose,float tick){original.scale(rat,pose,tick);if(rat.isBaby())pose.scale(.6F,.6F,.6F);}
}
