package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.server.entity.rat.RatSpellAttributes;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SetRatNamePacket;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.github.alexthe666.rats.server.entity.rat.RatProgression;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

public class RatStatsScreen extends Screen {
    private static final int ROW = 13;
    private final Screen parent;
    private final TamedRat rat;
    private EditBox name;
    private boolean spells;
    private int x, y, panelHeight, scroll, maxScroll;

    public RatStatsScreen(Screen parent, TamedRat rat) {
        super(Component.literal("老鼠档案"));
        this.parent = parent;
        this.rat = rat;
    }

    @Override protected void init() {
        panelHeight = Math.min(244, height - 8);
        x = width / 2 - 140;
        y = (height - panelHeight) / 2;
        name = new EditBox(font, x, y + 24, 180, 20, Component.literal("名字"));
        name.setMaxLength(48);
        name.setValue(rat.hasCustomName() ? rat.getName().getString() : "");
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.literal("改名"), b -> RatsNetworkHandler.CHANNEL.sendToServer(
                new SetRatNamePacket(rat.getId(), name.getValue()))).bounds(x + 184, y + 24, 44, 20).build());
        addRenderableWidget(Button.builder(Component.literal("随机"), b -> {
            String[] first = rat.isMale() ? new String[]{"芝士","胡椒","小麦","松果"} : new String[]{"奶糖","花生","布丁","可可"};
            String[] last = {"尾巴","团子","球球","饼干"};
            name.setValue(first[rat.getRandom().nextInt(first.length)] + last[rat.getRandom().nextInt(last.length)]);
        }).bounds(x + 232, y + 24, 48, 20).build());
        addRenderableWidget(Button.builder(modeLabel(), b -> {
            spells = !spells;
            scroll = 0;
            b.setMessage(modeLabel());
        }).bounds(x + 42, y + panelHeight - 25, 94, 20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> onClose())
                .bounds(x + 144, y + panelHeight - 25, 94, 20).build());
    }

    private Component modeLabel() { return Component.translatable(spells ? "gui.rats.magic_back" : "gui.rats.magic_details"); }
    private static String number(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    private List<String> spellBonuses() {
        List<String> lines = new ArrayList<>();
        for (var attr : RatSpellAttributes.profileAttributes()) {
            var id = ForgeRegistries.ATTRIBUTES.getKey(attr);
            if (id == null || !rat.getAttributes().hasAttribute(attr)) continue;
            String key = id.getPath();
            if (key.equals("spell_potency") || key.equals("spell_power")) continue;
            double value = rat.getAttributeValue(attr);
            double delta = value - attr.getDefaultValue();
            if (Math.abs(delta) < 0.00001) continue;
            String stat = Component.translatable(attr.getDescriptionId()).getString();
            String row = key.endsWith("_spell_power") && attr.getDefaultValue() == 1.0
                    ? String.format(Locale.ROOT, "%s  ×%.2f (%+.0f%%)", stat, value, delta * 100)
                    : String.format(Locale.ROOT, "%s  %.2f (%+.2f)", stat, value, delta);
            if (RatSpellAttributes.isSchoolPower(id)) lines.add(0, row);
            else lines.add(row);
        }
        return lines;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(x - 10, y, x + 290, y + panelHeight, 0xEE20252B);
        g.drawCenteredString(font, title, width / 2, y + 6, 0xFFE8B4);
        List<String> lines = new ArrayList<>();
        if (!spells) {
            int level = rat.getRatLevel();
            lines.add((level == RatProgression.MAX_LEVEL ? Component.translatable("gui.rats.level_max", level)
                    : Component.translatable("gui.rats.level_xp", level, rat.getRatExperience() - RatProgression.threshold(level),
                            RatProgression.threshold(level + 1) - RatProgression.threshold(level))).getString());
            if(rat.getEvolution()!=com.github.alexthe666.rats.server.entity.rat.RatEvolution.NONE){
                lines.add(Component.translatable("evolution.rats.profile",rat.getEvolution().title()).getString());
                lines.add(rat.getEvolution().bonus().getString());
                for(int stage=1;stage<=2;stage++)lines.add(rat.getEvolution().mastery(stage,level).getString());
            }
            lines.add(Component.translatable("gui.rats.combat_stats", number(rat.getProfileAttack()), number(rat.getAttributeValue(Attributes.ARMOR))).getString());
            lines.add(String.format(Locale.ROOT, "生命 %.1f / %.1f   性别 %s",rat.getHealth(),rat.getMaxHealth(),rat.isMale()?"雄":"雌"));
            lines.add(String.format(Locale.ROOT, "基因  速度 ×%.2f   生命 ×%.2f",rat.getRatSpeedGeneForDisplay(),rat.getRatHealthGeneForDisplay()));
            lines.add(String.format(Locale.ROOT, "护甲 ×%.2f   攻击 ×%.2f   回复 ×%.2f",rat.getRatArmorGeneForDisplay(),rat.getRatDamageGeneForDisplay(),rat.getRatRegenGeneForDisplay()));
        }
        lines.add(Component.translatable("gui.rats.magic_genes", number(rat.getGoetyPowerGene()), number(rat.getIronPowerGene())).getString());
        lines.add(Component.translatable("gui.rats.magic_current", number(RatSpellAttributes.currentGoety(rat)), number(RatSpellAttributes.currentIron(rat))).getString());
        lines.add(Component.translatable("gui.rats.cooldown_gene",number(rat.getCooldownGene()),number(100*rat.spellCooldownReduction())).getString());
        List<String> bonuses = spellBonuses();
        if (spells) {
            if (bonuses.isEmpty()) lines.add(Component.translatable("gui.rats.magic_none").getString());
            else lines.addAll(bonuses);
        } else {
            if (!bonuses.isEmpty()) lines.add(bonuses.get(0));
            lines.add("今日奶酪 "+rat.getCheeseEatenToday()+" / 4   储备 "+rat.getCheeseReserveCount()+" / 16");
            lines.add("工作效率 "+Math.round(rat.getRatHungerEfficiency()*100)+"%   "+(RatDaySchedule.shouldSleep(rat.level())?"休息时段":"工作时段"));
            lines.add("房屋："+rat.getBoundHousePos().map(p->p.pos().toShortString()).orElse("未绑定"));
            lines.add("基地："+rat.getWorkAnchorPoint().map(p->p.pos().toShortString()).orElse("未设置"));
            lines.add("目标："+rat.getCreateWorkPos().map(p->p.pos().toShortString()).orElse("未设置"));
        }
        int rows = Math.max(1, (panelHeight - 79) / ROW);
        maxScroll = Math.max(0, lines.size() - rows);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        for (int i = 0; i < rows && scroll + i < lines.size(); i++)
            g.drawString(font, lines.get(scroll+i), x, y+52+i*ROW, 0xEEEEEE, false);
        if (maxScroll > 0) {
            int track = rows * ROW;
            int thumb = Math.max(8, track * rows / lines.size());
            int top = y + 52 + (track - thumb) * scroll / maxScroll;
            g.fill(x+278, y+52, x+280, y+52+track, 0xFF474D51);
            g.fill(x+278, top, x+280, top+thumb, 0xFFE8C9A0);
        }
        super.render(g,mx,my,pt);
    }

    @Override public boolean mouseScrolled(double mx,double my,double delta) {
        if (mx < x || mx > x+280 || my < y+48 || my > y+panelHeight-26 || maxScroll==0)
            return super.mouseScrolled(mx,my,delta);
        scroll=Mth.clamp(scroll-(int)Math.signum(delta),0,maxScroll);
        return true;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
