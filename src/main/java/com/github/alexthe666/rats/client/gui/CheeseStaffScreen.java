package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.client.util.EntityRenderingUtil;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SyncRatStaffPacket;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CheeseStaffScreen extends Screen {

	private final TamedRat rat;
	private final BlockPos pos;
	private final Direction clickedFace;

	public CheeseStaffScreen(TamedRat rat, BlockPos pos, Direction clickedFace) {
		super(Component.translatable(RatsItemRegistry.CHEESE_STICK.get().getDescriptionId()));
		this.rat = rat;
		this.pos = pos;
		this.clickedFace = clickedFace;
		this.init();
	}

protected void init() {super.init();clearWidgets();int bw=Math.min(164,(width-24)/2),x=(width-(bw*2+4))/2,y=Math.max(74,(height-138)/2);String[] labels={"存放物品","取出物品","设置原版家位置","清除原版家位置","设置工作基地","清除工作基地","设置机器/使用物品目标","清除机器目标","绑定此房屋","解除房屋绑定","清除搬运位置"};int[] controls={0,1,2,3,8,9,12,13,10,11,7};for(int n=0;n<controls.length;n++){int control=controls[n];var b=Button.builder(Component.literal(labels[n]),button->{RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(rat.getId(),pos,clickedFace,control));Minecraft.getInstance().setScreen(null);}).bounds(x+(n%2)*(bw+4),y+(n/2)*23,bw,20).build();if(control==10)b.active=rat.level().getBlockEntity(pos) instanceof com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;addRenderableWidget(b);}
    if(rat.isBrick() && !isNoInventoryAtPos())addRenderableWidget(Button.builder(Component.translatable("brick.rats.bind"),button->{
        RatsNetworkHandler.CHANNEL.sendToServer(new SyncRatStaffPacket(rat.getId(),pos,clickedFace,14));Minecraft.getInstance().setScreen(null);
    }).bounds(x+bw+4,y+5*23,bw,20).build());}

	private String getPosName() {
		if (this.pos != null) {
			BlockState state = this.rat.level().getBlockState(this.pos);
			List<Component> namelist = null;
			ItemStack pick = state.getBlock().getCloneItemStack(this.rat.level(), this.pos, state);
			try {
				namelist = pick.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
			} catch (Throwable ignored) {
			}
			if (namelist != null && !namelist.isEmpty()) {
				return namelist.get(0).getString();
			}
		}
		return "";
	}

	private boolean isNoInventoryAtPos() {
		if (this.pos != null) {
			Level level = this.rat.level();
			return level.getBlockEntity(this.pos) == null;
		}
		return true;
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, float partialTicks) {
		this.renderBackground(graphics);
		super.render(graphics, x, y, partialTicks);
		int rowY=Math.max(74,(height-138)/2);
 graphics.drawCenteredString(font,title,width/2,12,0xFFFFFF);
		if (this.rat != null) {
			EntityRenderingUtil.drawEntityOnScreen(graphics, width/2, rowY-10, 35, 0, 0, this.rat, true);
		}

	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
