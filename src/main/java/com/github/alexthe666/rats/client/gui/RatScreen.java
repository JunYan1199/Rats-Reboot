package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.util.EntityRenderingUtil;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import com.github.alexthe666.rats.server.message.RatCommandPacket;
import com.github.alexthe666.rats.server.message.RatUpgradeVisibilityPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class RatScreen extends AbstractContainerScreen<RatMenu> {
	protected static final ResourceLocation TEXTURE = new ResourceLocation(RatsMod.MODID, "textures/gui/container/rat_inventory.png");
	private static final ResourceLocation TEXTURE_BACKDROP = new ResourceLocation(RatsMod.MODID, "textures/gui/container/rat_inventory_backdrop.png");
	private int currentDisplayCommand = 0;
	private final TamedRat rat;

	public RatScreen(RatMenu container, Inventory inv, TamedRat rat) {
		super(container, inv, rat.getDisplayName());
		this.rat = rat;
		this.imageWidth = 238;
		this.imageHeight = 203;
		this.currentDisplayCommand = rat.getCommand().ordinal();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void init() {
		super.init();
		int x = leftPos, y = topPos;
		if (!rat.isBaby()) {
			addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("‹"), b -> select(-1))
					.bounds(x + 153, y + 54, 14, 16).build());
			commandButton = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(selectedName(), b -> {
				rat.setCommand(RatCommand.values()[currentDisplayCommand]);
				RatsNetworkHandler.CHANNEL.sendToServer(new RatCommandPacket(rat.getId(), currentDisplayCommand));
			}).bounds(x + 167, y + 54, 49, 16).build());
			select(0);
			addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("›"), b -> select(1))
					.bounds(x + 216, y + 54, 14, 16).build());
			EquipmentSlot[] rows = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
			for (int i = 0; i < rows.length; i++) {
				EquipmentSlot row = rows[i];
				addRenderableWidget(new UpgradeVisibilityButton(x + 47, y + 25 + 18 * i, rat.isSlotVisible(row), b -> {
					var eye = (UpgradeVisibilityButton)b;
					eye.toggleVisibility();
					rat.setSlotVisibility(row, eye.getUpgradeVisibility());
					RatsNetworkHandler.CHANNEL.sendToServer(new RatUpgradeVisibilityPacket(rat.getId(), row, eye.getUpgradeVisibility()));
				}));
			}
		}
		addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("档案 / 改名"), b -> {
			minecraft.player.closeContainer();
			minecraft.setScreen(new RatStatsScreen(null, rat));
		}).bounds(x + 167, y + 4, 63, 15).build());
	}

	private net.minecraft.client.gui.components.Button commandButton;

	private Component selectedName() {
		return Component.translatable(RatCommand.values()[currentDisplayCommand].getTranslateName());
	}

	private void select(int step) {
		int count=RatCommand.values().length;
        currentDisplayCommand=Math.floorMod(currentDisplayCommand+step,count);
        if(!rat.isBrick()&&currentDisplayCommand==RatCommand.COOK.ordinal())currentDisplayCommand=Math.floorMod(currentDisplayCommand+(step<0?-1:1),count);
		commandButton.setMessage(selectedName());
		commandButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
				Component.translatable(RatCommand.values()[currentDisplayCommand].getTranslateDescription())));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
		g.fill(x + 1, y + 1, x + imageWidth - 2, y + imageHeight - 2, 0xFFFFFFFF);
		g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC6C6C6);
		g.fill(x + 57, y + 21, x + 104, y + 75, 0xFF25272A);
		EntityRenderingUtil.drawEntityOnScreen(g, x + 80, y + 69, rat.isBrick() ? 30 : 51, x + 80 - mouseX, y + 45 - mouseY, rat, false);
		g.blit(TEXTURE, x + 90, y + 21, rat.isMale() ? 0 : 16, 209, 16, 16);
		for (var slot : menu.slots) {
			int sx = x + slot.x, sy = y + slot.y;
			g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFFFFFFFF);
			g.fill(sx - 1, sy - 1, sx + 16, sy + 16, 0xFF373737);
			g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, getTitle(), 60, 7, 0x404040, false);
		g.drawString(font, "升级", 8, 9, 0x404040, false);
		if(rat.isBrick())g.drawString(font,Component.translatable("brick.rats.offhand"),130,12,0x404040,false);
		g.drawString(font, "储备", 130, 46, 0x404040, false);
		if(rat.getCommand()==RatCommand.SPELL_COMBAT)g.drawCenteredString(font,rat.spellBattleStatus(),158,79,0x284B53);
        g.drawString(font, "背包", 39, 79, 0x404040, false);
		g.drawString(font, playerInventoryTitle, 39, 111, 0x404040, false);
		g.drawCenteredString(font, Component.translatable(RatsLangConstants.RAT_CURRENT_COMMAND), 191, 24, 0x505050);
		g.drawCenteredString(font, Component.translatable(rat.getCommand().getTranslateName()), 191, 37, 0xFFFFFF);
	}
}
