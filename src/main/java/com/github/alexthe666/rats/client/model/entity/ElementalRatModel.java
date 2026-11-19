package com.github.alexthe666.rats.client.model.entity;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.IOException;

/** Extra cuboids share the original rat's texture atlas and animated bones. */
public final class ElementalRatModel extends RatModel<TamedRat> {
    private record Box(String bone,float[] center,float[] size,float[] angles,int swatch){}
    private record Shape(float[] joint,Box[] boxes,java.util.Map<String,float[]> poses,String[] hide,java.util.Map<String,float[]> bulk){}
    private AdvancedModelBox bone(String name){
        return switch(name){
            case "body1" -> body1;
            case "body2" -> body2;
            case "neck" -> neck;
            case "head" -> head;
            case "snout" -> snout;
            case "nose" -> nose;
            case "wisker1" -> wisker1;
            case "wisker2" -> wisker2;
            case "leftEar" -> leftEar;
            case "rightEar" -> rightEar;
            case "leftEye" -> leftEye;
            case "rightEye" -> rightEye;
            case "leftArm" -> leftArm;
            case "rightArm" -> rightArm;
            case "leftHand" -> leftHand;
            case "rightHand" -> rightHand;
            case "leftThigh" -> leftThigh;
            case "rightThigh" -> rightThigh;
            case "leftFoot" -> leftFoot;
            case "rightFoot" -> rightFoot;
            case "tail1" -> tail1;
            case "tail2" -> tail2;
            default -> throw new IllegalArgumentException("Unknown rat bone "+name);
        };
    }
    public ElementalRatModel(ResourceManager resources,String form){
        var asset=new ResourceLocation("rats","evolution/"+form+".json");
        try(var reader=resources.getResourceOrThrow(asset).openAsReader()){
            var shape=new Gson().fromJson(reader,Shape.class);
            tail1.cubeList.clear();
            tail2.cubeList.clear();
            tail2.setRotationPoint(shape.joint[0],shape.joint[1],shape.joint[2]);
            if(shape.poses!=null) shape.poses.forEach((name,p)->bone(name).setRotationPoint(p[0],p[1],p[2]));
            if(shape.hide!=null) for(String name:shape.hide)bone(name).cubeList.clear();
            // Scale only the textured cuboids. Eye, limb and tail attachments keep their original pivots.
            if(shape.bulk!=null) shape.bulk.forEach((name,scale)->{
                var parent=bone(name);
                var fur=new AdvancedModelBox(this,name+"_fur");
                fur.cubeList.addAll(parent.cubeList);
                parent.cubeList.clear();
                fur.setScale(scale[0],scale[1],scale[2]);
                parent.addChild(fur);
            });
            updateDefaultPose();
            for(var b:shape.boxes){
                var limb=bone(b.bone);
                var part=new AdvancedModelBox(this,48+b.swatch%4*4,b.swatch/4*2);
                part.setRotationPoint(b.center[0],b.center[1],b.center[2]);
                part.addBox(-.5F,-.5F,-.5F,1,1,1);
                part.setScale(b.size[0],b.size[1],b.size[2]);
                setRotateAngle(part,(float)Math.toRadians(b.angles[0]),(float)Math.toRadians(b.angles[1]),(float)Math.toRadians(b.angles[2]));
                limb.addChild(part);
            }
        }catch(IOException e){throw new IllegalStateException("Cannot read "+asset,e);}
    }
}