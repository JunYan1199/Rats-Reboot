package com.github.alexthe666.rats.server.entity.rat;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="rats")
public enum RatEvolution {
    NONE, RIBBON, LUNAR, TIDE, EMBER, SPARK,
    PLAIN, VOLT, SEER, LEAF, FROST, BRAWL, VENOM, DUNE, GALE, MOTH, CRAG, WISP, DRAKE, ALLOY;

    public static final int MIN_LEVEL=50;
    private static final RatEvolution[] FORMS=values();
    private static final UUID SPEED_ID=UUID.fromString("b8408d46-4274-4bd6-98c6-bdc1a8784f93");
    public final String id=name().toLowerCase(Locale.ROOT);
    public final ResourceLocation texture=new ResourceLocation("rats","textures/entity/rat/evolution/"+id+".png");
    final TagKey<Item> stones=TagKey.create(Registries.ITEM,new ResourceLocation("rats","evolution/"+id));
    public static RatEvolution byIndex(int index){return index>=0 && index<FORMS.length?FORMS[index]:NONE;}
    public static RatEvolution fromSaved(String key){for(var form:FORMS)if(form.id.equals(key))return form;return NONE;}
    public static RatEvolution forStone(ItemStack stack){
        if(!stack.isEmpty())for(var form:FORMS)if(form!=NONE && stack.is(form.stones))return form;
        return NONE;
    }
    public Component title(){return Component.translatable("evolution.rats."+id);}
    public Component bonus(){return Component.translatable("evolution.rats."+id+".bonus");}
    public Component mastery(int tier,int level){
        int gate=tier==1?50:100;
        return Component.translatable(level>=gate?"evolution.rats.mastery.open":"evolution.rats.mastery.locked",gate,Component.translatable("evolution.rats."+id+".tier"+tier));
    }
    public void updateSpeed(TamedRat rat){
        var attr=rat.getAttribute(Attributes.MOVEMENT_SPEED);if(attr==null)return;
        double amount=this==SPARK?.12:this==TIDE&&rat.isInWater()?.30:0;
        var existing=attr.getModifier(SPEED_ID);
        if(existing!=null && existing.getAmount()==amount)return;
        if(existing!=null)attr.removeModifier(SPEED_ID);
        if(amount>0)attr.addTransientModifier(new AttributeModifier(SPEED_ID,"Rat evolution movement",amount,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    public void tickMovement(TamedRat rat){
        if(this==TIDE||this==SPARK)updateSpeed(rat);
        if(this==GALE && !rat.onGround() && !rat.isInWaterOrBubble() && !rat.isInLava() && !rat.isPassenger()){
            var v=rat.getDeltaMovement();
            if(v.y<-.30)rat.setDeltaMovement(v.x,-.30,v.z);
        }
    }
    @SubscribeEvent public static void onRecovery(LivingHealEvent event){
        if(!(event.getEntity() instanceof TamedRat rat))return;
        if(rat.getEvolution()==RIBBON)event.setAmount(event.getAmount()*1.15F);
        if(rat.getEvolution()==LEAF && rat.level().isDay() && rat.level().canSeeSky(rat.blockPosition()))event.setAmount(event.getAmount()*1.25F);
    }
    @SubscribeEvent public static void onKnockback(LivingKnockBackEvent event){
        if(!(event.getEntity() instanceof TamedRat rat))return;
        if(rat.getEvolution()==LUNAR)event.setStrength(event.getStrength()*.80F);
        if(rat.getEvolution()==DUNE && rat.onGround())event.setStrength(event.getStrength()*.70F);
    }
    @SubscribeEvent public static void onDamage(LivingHurtEvent event){
        var source=event.getSource();
        boolean melee=source.is(DamageTypes.MOB_ATTACK)||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)||source.is(DamageTypes.PLAYER_ATTACK);
        var target=event.getEntity();
        if(target instanceof TamedRat rat){
            double multiplier=switch(rat.getEvolution()){
                case EMBER -> source.is(DamageTypeTags.IS_FIRE)?.75:1;
                case VOLT -> source.is(DamageTypes.LIGHTNING_BOLT)?.40:1;
                case SEER -> source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC)?.80:1;
                case FROST -> source.is(DamageTypeTags.IS_FREEZING)?.50:1;
                case CRAG -> source.is(DamageTypeTags.IS_EXPLOSION)?.75:1;
                case WISP -> source.is(DamageTypeTags.IS_PROJECTILE)?.85:1;
                case ALLOY -> melee?.88:1;
                default -> 1;
            };
            if(multiplier!=1)event.setAmount((float)(event.getAmount()*multiplier));
        }
        if(melee && source.getDirectEntity() instanceof TamedRat attacker){
            double multiplier=switch(attacker.getEvolution()){
                case BRAWL -> 1.10;
                case MOTH -> target.getMobType()==net.minecraft.world.entity.MobType.ARTHROPOD?1.15:1;
                case DRAKE -> target.getMaxHealth()>=100?1.12:1;
                default -> 1;
            };
            if(multiplier!=1)event.setAmount((float)(event.getAmount()*multiplier));
        }
    }
}
