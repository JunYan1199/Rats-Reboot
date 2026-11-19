package com.github.alexthe666.rats.server.legion;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.Polarice3.Goety.api.items.magic.*;
import com.Polarice3.Goety.api.magic.*;
import com.Polarice3.Goety.common.items.magic.FocusBag;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.utils.WandUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

class GoetyCast implements LegionSpell {
    protected final TamedRat rat;
    protected final ItemStack focus;
    protected final ISpell spell;
    private final ItemStack source;
    private final String id;
    private final java.util.function.BooleanSupplier present;
    private final ServerLevel world;
    private ItemStack wand;
    private SpellStat stats;
    private int duration,shots,nextShot;
    protected GoetyCast(TamedRat rat,String id,ItemStack source,ItemStack focus,ISpell spell,java.util.function.BooleanSupplier present){
        this.rat=rat;this.id=id;this.source=source;this.focus=focus.copy();this.spell=spell;
        this.present=present;world=(ServerLevel)rat.level();
    }
    static boolean hasFoci(ItemStack stack){
        var handler=stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        if(handler!=null)for(int i=0;i<handler.getSlots();i++)if(handler.getStackInSlot(i).getItem() instanceof IFocus)return true;
        return false;
    }
    static boolean carrier(ItemStack s){return s.getItem() instanceof FocusBag||s.getItem() instanceof IWand||s.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,new ResourceLocation("rats","focus_bags")));}
    static void collect(TamedRat r,int slot,ItemStack stack,List<LegionSpell> out){
        var h=stack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);if(h==null)return;
        for(int i=0;i<h.getSlots();i++){var f=h.getStackInSlot(i);
            if(f.isEmpty()||!(f.getItem() instanceof IFocus item)||item.getSpell()==null)continue;
            String id=ForgeRegistries.ITEMS.getKey(f.getItem()).toString();
            if(SpellRoles.get(id)==null)continue;
            int pocket=i;ItemStack original=f.copy();
            out.add(new GoetyCast(r,id,stack,f,item.getSpell(),()->r.getInventory().getItem(slot)==stack&&!stack.isEmpty()
                &&pocket<h.getSlots()&&ItemStack.isSameItemSameTags(original,h.getStackInSlot(pocket))));
        }
    }
    public String id(){return id;}
    public boolean available(){return present.getAsBoolean();}
    protected boolean isAstrologyFocus(){return "goety_astrology".equals(ForgeRegistries.ITEMS.getKey(focus.getItem()).getNamespace());}
    protected ItemStack medium(){
        if(source.getItem() instanceof IWand)return source;
        ItemStack wand=new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(isAstrologyFocus()?new ResourceLocation("goety_astrology","starcaller_staff"):new ResourceLocation("goety","dark_wand"))));
        IWand.getItemHandler(wand).insertItem(0,focus.copy(),false);
        return wand;
    }
    protected SpellStat castStats(ItemStack wand){return WandUtil.getStats(rat,spell);}
    public int duration(){return duration;}
    public int cooldown(){return spell.spellCooldown(rat);}
    public boolean begin(LivingEntity target){
        wand=medium();
        if(wand==null||wand.isEmpty())return false;
        return rat.withSpellMedium(wand,()->startWithMedium());
    }
    private boolean startWithMedium(){
        stats=castStats(wand);
        if(!spell.conditionsMet(world,rat,stats))return false;
        duration=Math.max(1,spell.castDuration(rat,wand));
        if(spell instanceof IChargingSpell c){nextShot=Math.max(0,c.castUp(rat,wand));if(c.everCharge())duration=Math.max(45,nextShot+1);}
        spell.startSpell(world,rat,wand,stats);return true;
    }
    public void tick(int elapsed){
        rat.withSpellMedium(wand,()->{tickWithMedium(elapsed);return null;});
    }
    private void tickWithMedium(int elapsed){
        spell.useSpell(world,rat,wand,elapsed,stats);
        if(spell instanceof IChargingSpell c&&elapsed>=nextShot&&(c.shotsNumber(rat,wand)<=0||shots<c.shotsNumber(rat,wand))){
            spell.SpellResult(world,rat,wand,stats);shots++;nextShot=elapsed+Math.max(1,c.Cooldown(rat,wand,shots));
        }
    }
    public void finish(boolean cancelled){
        rat.withSpellMedium(wand,()->{
            spell.stopSpell(world,rat,wand,focus,cancelled?0:duration,stats);
            if(!cancelled&&!(spell instanceof IChargingSpell))spell.SpellResult(world,rat,wand,stats);
            return null;
        });
    }
}
