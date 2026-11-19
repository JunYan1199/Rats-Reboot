package com.github.alexthe666.rats.server.legion;

import cn.blockforge.generated.minecraftmodid246279.bridge.FocusScrolls;
import cn.blockforge.generated.minecraftmodid246279.bridge.GoetyCastService;
import cn.blockforge.generated.minecraftmodid246279.spells.FocusProxySpell;
import com.Polarice3.Goety.api.magic.ISpell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.item.ItemStack;

/** A Twilight book page normally enters its native focus through a player-only cast callback. */
final class TwilightFocusPage extends GoetyCast {
    private final FocusProxySpell page;
    private final IronCast bookEntry;

    static LegionSpell adapt(IronCast bookEntry,TamedRat rat,ItemStack book,AbstractSpell spell){
        if(!(spell instanceof FocusProxySpell proxy))return bookEntry;
        ISpell nativeSpell=proxy.goetySpell();
        if(nativeSpell==null)return null;
        ItemStack focus=FocusScrolls.focusForCast(proxy.freshFocus(),book,proxy.getSpellResource());
        if(focus==null||focus.isEmpty())return null;
        return new TwilightFocusPage(bookEntry,rat,focus,nativeSpell,proxy);
    }
    private TwilightFocusPage(IronCast entry,TamedRat rat,ItemStack focus,ISpell spell,FocusProxySpell page){
        super(rat,entry.id(),ItemStack.EMPTY,focus,spell,entry::available);
        this.page=page;
        this.bookEntry=entry;
    }
    @Override protected ItemStack medium(){
        return isAstrologyFocus()?super.medium():GoetyCastService.mediumFor(rat,spell,focus);
    }
    @Override protected SpellStat castStats(ItemStack wand){
        SpellStat stats=GoetyCastService.statsWithMedium(rat,spell,focus,wand);
        int power=Math.round((stats.getPotency()+1)*page.getEntityPowerMultiplier(rat))-1;
        return stats.setPotency(Math.max(0,power));
    }
    @Override public boolean isBook(){return true;}
    public int cooldown(){return Math.max(super.cooldown(),bookEntry.cooldown());}
}
