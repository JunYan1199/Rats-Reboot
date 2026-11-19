package guildjane;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(GuildJane.ID)
public class GuildJane {
    public static final String ID="guildjane";
    static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
    public static final RegistryObject<EntityType<Receptionist>> NPC=ENTITIES.register("receptionist",()->EntityType.Builder.of(Receptionist::new,MobCategory.MISC).sized(.52f,1.8f).clientTrackingRange(8).build(ID+":receptionist"));
    public GuildJane() {
        GuildWire.register();
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        PowerStation.init(bus);
        ENTITIES.register(bus);
        GuildHall.BLOCKS.register(bus);GuildHall.ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e)->e.put(NPC.get(),Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.22).build()));
    }
    public static class Receptionist extends PathfinderMob {
        final GuildDuty duty=new GuildDuty(this);
        int flourish,speechAge=1000,speechLength=52;
        float gesture,gestureOld;
        static final EntityDataAccessor<Integer> SPEAK_UNTIL=SynchedEntityData.defineId(Receptionist.class,EntityDataSerializers.INT);
        @Override protected net.minecraft.world.InteractionResult mobInteract(Player p,net.minecraft.world.InteractionHand hand){
            if(hand==net.minecraft.world.InteractionHand.MAIN_HAND && p instanceof net.minecraft.server.level.ServerPlayer sp)Conversation.open(sp,this);
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        @Override public void handleEntityEvent(byte event){
            if(event==61)flourish=22;
            else if(event==62)speechAge=0;
            else super.handleEntityEvent(event);
        }
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> id){
            super.onSyncedDataUpdated(id);
            if(SPEAK_UNTIL.equals(id)&&level().isClientSide)speechLength=net.minecraft.util.Mth.clamp(entityData.get(SPEAK_UNTIL),16,110);
        }
        @Override public void tick(){
            super.tick();
            if(flourish>0)flourish--;
            if(!level().isClientSide&&!isNoAi())duty.tick();
            if(level().isClientSide){
                speechAge=Math.min(1000,speechAge+1);gestureOld=gesture;
                gesture=net.minecraft.util.Mth.approach(gesture,speechAge<speechLength?1:0,.12f);
            }
            if(level() instanceof net.minecraft.server.level.ServerLevel w && (tickCount==1||tickCount%10==0))
                entityData.set(NIGHT,GuildLedger.get(w.getServer()).night());
        }
        static final EntityDataAccessor<Boolean> NIGHT=SynchedEntityData.defineId(Receptionist.class,EntityDataSerializers.BOOLEAN);
        public Receptionist(EntityType<? extends Receptionist> type,Level level){super(type,level);setPersistenceRequired();setInvulnerable(true);}
        @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(NIGHT,false);entityData.define(SPEAK_UNTIL,52);}
        @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Nocturne",entityData.get(NIGHT));tag.putInt("GuildMotionVersion",1);duty.save(tag);}
        @Override public void readAdditionalSaveData(CompoundTag tag){
            super.readAdditionalSaveData(tag);entityData.set(NIGHT,tag.getBoolean("Nocturne"));duty.load(tag);
            if(tag.contains("Nocturne")&&!tag.contains("GuildMotionVersion"))setNoAi(false);
        }
        @Override public Component getTypeName(){return Component.literal("公会接待员 · Jane Doe");}
    }
}
