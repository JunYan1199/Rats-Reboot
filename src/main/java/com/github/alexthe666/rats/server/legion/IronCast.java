package com.github.alexthe666.rats.server.legion;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;

final class IronCast implements LegionSpell {
    private static final boolean TWILIGHT=net.minecraftforge.fml.ModList.get().isLoaded("goety_iron_twilight");
    private final TamedRat rat;
    private final int slot,level;
    private final ItemStack book;
    private final AbstractSpell spell;
    private MagicData data;
    private int duration;
    private IronCast(TamedRat r,int slot,ItemStack stack,SpellData s){
        rat=r;this.slot=slot;book=stack;spell=s.getSpell();level=s.getLevel();
    }
    static boolean hasPages(ItemStack s){var c=ISpellContainer.get(s);if(c!=null)for(var page:c.getActiveSpells())if(page.spellData()!=null&&page.spellData().getSpell()!=null&&page.spellData().getLevel()>0)return true;return false;}
    static boolean carrier(ItemStack s){return !(s.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll) && ISpellContainer.isSpellContainer(s);}
    static void collect(TamedRat r,int slot,ItemStack stack,List<LegionSpell> out){
        var book=ISpellContainer.get(stack);
        if(book!=null)for(SpellSlot page:book.getActiveSpells()){
            var s=page.spellData();if(s==null||s.getSpell()==null||!s.getSpell().isEnabled()||SpellRoles.get(s.getSpell().getSpellId())==null)continue;
            IronCast entry=new IronCast(r,slot,stack,s);
            LegionSpell cast=TWILIGHT?TwilightFocusPage.adapt(entry,r,stack,s.getSpell()):entry;
            if(cast!=null)out.add(cast);
        }
    }
    public String id(){return spell.getSpellId();}
    public int duration(){return duration;}
    public boolean isBook(){return true;}
    public int cooldown(){return spell.getSpellCooldown();}
    public boolean available(){
        if(rat.getInventory().getItem(slot)!=book||book.isEmpty())return false;
        var c=ISpellContainer.get(book);if(c==null)return false;
        int i=c.getIndexForSpell(spell);return i>=0&&c.getSpellAtIndex(i).getLevel()==level;
    }
    public boolean begin(LivingEntity target){
        data=new MagicData(true);data.setSyncedData(new SyncedSpellData(rat));data.setPlayerCastingItem(book);
        if(!spell.checkPreCastConditions(rat.level(),level,rat,data))return false;
        if(data.getAdditionalCastData() instanceof TargetEntityCastData) data.setAdditionalCastData(new TargetEntityCastData(target));
        duration=Math.max(1,spell.getEffectiveCastTime(level,rat));
        data.initiateCast(spell,level,duration,CastSource.SPELLBOOK,"mainhand");
        spell.onServerPreCast(rat.level(),level,rat,data);
        return true;
    }
    public void tick(int elapsed){
        spell.onServerCastTick(rat.level(),level,rat,data);
        if(spell.getCastType()==CastType.CONTINUOUS&&elapsed%10==0)cast();
        data.handleCastDuration();
    }
    private void cast(){spell.onCast(rat.level(),level,rat,CastSource.SPELLBOOK,data);}
    public void finish(boolean cancelled){
        try{if(!cancelled&&spell.getCastType()!=CastType.CONTINUOUS)cast();}
        finally{spell.onServerCastComplete(rat.level(),level,rat,data,cancelled);data.resetAdditionalCastData();}
    }
}
