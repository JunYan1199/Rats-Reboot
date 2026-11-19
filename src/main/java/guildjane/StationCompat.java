package guildjane;

import java.lang.reflect.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;

final class StationCompat {
    static java.util.function.IntSupplier rfCapacity=()->32000;
    static Method manaGet,manaRead,manaAdd,send;static Constructor<?> packet;static Attribute max;
    static void setup(){
        try{
            if(ModList.get().isLoaded("create")){
                Class<?> kinetic=Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity");
                PowerStation.registerOutput("机械动力应力",new PowerStation.Output(){
                    public boolean accepts(BlockEntity t){return kinetic.isInstance(t);}
                    public int supply(BlockEntity t,int n){return 0;}
                });
            }
            if(ModList.get().isLoaded("refinedstorage")){
                var config=Class.forName("com.refinedmods.refinedstorage.RS").getField("SERVER_CONFIG").get(null);
                var controller=config.getClass().getMethod("getController").invoke(config);
                var capacity=controller.getClass().getMethod("getCapacity");
                rfCapacity=()->(Integer)call(capacity,controller);
            }
            if(ModList.get().isLoaded("goety")){
                Class<?> arca=Class.forName("com.Polarice3.Goety.common.blocks.entities.ArcaBlockEntity"),se=Class.forName("com.Polarice3.Goety.utils.SEHelper");
                Method owner=arca.getMethod("getPlayer"),increase=se.getMethod("increaseSESouls",Player.class,int.class),bound=se.getMethod("getArcaBlock",Player.class),dim=se.getMethod("getArcaDimension",Player.class);
                PowerStation.registerOutput("灵魂能量",new PowerStation.Output(){
                    public boolean accepts(BlockEntity t){return arca.isInstance(t);}
                    public int supply(BlockEntity t,int n){var p=call(owner,t);if(p==null||!t.getBlockPos().equals(call(bound,null,p))||!t.getLevel().dimension().equals(call(dim,null,p)))return 0;int amount=Math.min(1000,n);return Boolean.TRUE.equals(call(increase,null,p,amount))?amount:0;}
                });
            }
            if(ModList.get().isLoaded("irons_spellbooks")){
                var data=Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");manaGet=data.getMethod("getPlayerMagicData",LivingEntity.class);manaRead=data.getMethod("getMana");manaAdd=data.getMethod("addMana",float.class);
                packet=Class.forName("io.redspace.ironsspellbooks.network.SyncManaPacket").getConstructor(data);send=Class.forName("io.redspace.ironsspellbooks.setup.Messages").getMethod("sendToPlayer",Object.class,ServerPlayer.class);
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    static Object call(Method m,Object target,Object...args){try{return m.invoke(target,args);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    static int refill(ServerPlayer p,int n){if(manaGet==null)return 0;
        var attr=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks:max_mana"));if(attr==null||p.getAttribute(attr)==null)return 0;
        Object data=call(manaGet,null,p);int gain=Math.max(0,Math.min(n,(int)(p.getAttributeValue(attr)-(float)call(manaRead,data))));if(gain>0){call(manaAdd,data,(float)gain);try{call(send,null,packet.newInstance(data),p);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}return gain;}
}
