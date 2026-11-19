package com.github.alexthe666.rats.server.misc;
import net.minecraft.world.level.Level;import net.minecraft.core.BlockPos;import net.minecraft.core.Direction;import net.minecraftforge.items.IItemHandler;import net.minecraftforge.common.capabilities.ForgeCapabilities;
public final class RebornCapabilities {public static IItemHandler items(Level level,BlockPos pos,Direction side) { var be=level.getBlockEntity(pos);return be==null?null:be.getCapability(ForgeCapabilities.ITEM_HANDLER,side).orElse(null);}}
