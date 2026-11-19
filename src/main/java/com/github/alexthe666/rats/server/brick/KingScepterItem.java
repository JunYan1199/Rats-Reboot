package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 国王权杖：右键自己的布瑞克 → 打开任务界面（按帽子决定的职业列子任务）。
 * 界面里点「圈地 / 设驻守点」后，右键地面选角；选好的范围写进权杖 NBT，手持时客户端画粒子边框。
 */
public final class KingScepterItem extends Item {
    public KingScepterItem(Properties p) { super(p); }

    public static InteractionResult useOnBrick(TamedRat rat, Player player, ItemStack stack) {
        if (!rat.isOwnedBy(player)) return InteractionResult.PASS;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!rat.isBrick()) { player.displayClientMessage(Component.translatable("brick.rats.scepter.not_brick"), true); return InteractionResult.SUCCESS; }
        BrickJob job = BrickJob.of(rat);
        if (job == BrickJob.NONE) { player.displayClientMessage(Component.translatable("brick.rats.scepter.no_job"), true); return InteractionResult.SUCCESS; }
        if (job == BrickJob.CHEF) { player.displayClientMessage(Component.translatable("brick.rats.scepter.chef"), true); return InteractionResult.SUCCESS; }
        show(stack, rat, job);
        BrickWorkNetwork.open((ServerPlayer) player, rat);
        return InteractionResult.SUCCESS;
    }

    /** 把这只布瑞克当前职业的范围写进权杖，供客户端画边框 */
    static void show(ItemStack stack, TamedRat rat, BrickJob job) {
        CompoundTag t = stack.getOrCreateTag();
        t.remove("ShowA"); t.remove("ShowB"); t.remove("ShowPost");
        BlockPos[] a = rat.work.area(job);
        if (a != null) { t.put("ShowA", NbtUtils.writeBlockPos(a[0])); t.put("ShowB", NbtUtils.writeBlockPos(a[1])); }
        if (job == BrickJob.SENTRY && rat.work.post != null) t.put("ShowPost", NbtUtils.writeBlockPos(rat.work.post));
        t.putString("ShowDim", rat.level().dimension().location().toString());
        t.putString("ShowName", rat.getDisplayName().getString());
    }

    /** 开始选角：mode 0 = 两点圈地，1 = 驻守点 */
    static void beginSelect(ItemStack stack, TamedRat rat, BrickJob job, int mode) {
        CompoundTag s = new CompoundTag();
        s.putUUID("Rat", rat.getUUID());
        s.putInt("Job", job.ordinal());
        s.putInt("Mode", mode);
        stack.getOrCreateTag().put("Select", s);
    }

    /** 用 onItemUseFirst：右键箱子等方块时也先归权杖处理（不会打开箱子） */
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || stack.getTag() == null || !stack.getTag().contains("Select")) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        CompoundTag s = stack.getTag().getCompound("Select");
        var entity = ((ServerLevel) ctx.getLevel()).getEntity(s.getUUID("Rat"));
        if (!(entity instanceof TamedRat rat) || !rat.isOwnedBy(player)) {
            stack.getTag().remove("Select");
            player.displayClientMessage(Component.translatable("brick.rats.scepter.lost"), true);
            return InteractionResult.SUCCESS;
        }
        BrickJob job = BrickJob.values()[s.getInt("Job")];
        BlockPos pos = ctx.getClickedPos();
        if (s.getInt("Mode") == 1) {
            rat.work.post = pos.above().immutable();
            stack.getTag().remove("Select");
            player.displayClientMessage(Component.translatable("brick.rats.scepter.post_set", pos.getX(), pos.getY() + 1, pos.getZ()), false);
        } else if (!s.contains("First")) {
            s.put("First", NbtUtils.writeBlockPos(pos));
            player.displayClientMessage(Component.translatable("brick.rats.scepter.corner2"), true);
        } else {
            BlockPos first = NbtUtils.readBlockPos(s.getCompound("First"));
            rat.work.setArea(job, first, pos);
            BlockPos[] a = rat.work.area(job);
            stack.getTag().remove("Select");
            player.displayClientMessage(Component.translatable("brick.rats.scepter.area_set", Math.abs(a[1].getX() - a[0].getX()) + 1, Math.abs(a[1].getZ() - a[0].getZ()) + 1), false);
        }
        show(stack, rat, job);
        return InteractionResult.SUCCESS;
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rats.king_scepter.desc").withStyle(net.minecraft.ChatFormatting.GRAY));
        if (stack.getTag() != null && stack.getTag().contains("ShowName"))
            tip.add(Component.translatable("item.rats.king_scepter.bound", stack.getTag().getString("ShowName")).withStyle(net.minecraft.ChatFormatting.GOLD));
        if (stack.getTag() != null && stack.getTag().contains("Select"))
            tip.add(Component.translatable("brick.rats.scepter.selecting").withStyle(net.minecraft.ChatFormatting.YELLOW));
    }

    @Override public boolean isFoil(ItemStack stack) { return stack.getTag() != null && stack.getTag().contains("Select"); }
}
