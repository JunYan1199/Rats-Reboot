package com.github.alexthe666.rats.server.pizza;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

/**
 * 下水道鼠洞：主世界里是偷食鼠钻进去的洞，右键进入下水道副本；下水道里是返回用的出口，清完后亮起来。
 * 生存模式挖不掉，防止误挖丢了被偷的食物。
 */
public class RatHoleBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    private static final DustParticleOptions CRUMB = new DustParticleOptions(new Vector3f(0.86F, 0.62F, 0.28F), 0.9F);

    public RatHoleBlock() {
        super(Properties.of().mapColor(MapColor.DIRT).strength(-1.0F, 3600000.0F).sound(SoundType.ROOTED_DIRT).noLootTable()
                .lightLevel(s -> s.getValue(LIT) ? 12 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState(); }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        level.playSound(null, pos, SoundEvents.SILVERFISH_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.6F);
        if (level.dimension() == SewerManager.SEWER) { SewerManager.exit(sp, pos); return InteractionResult.SUCCESS; }
        PizzaRatData.Hole h = PizzaRatData.get(((ServerLevel) level).getServer()).holeAt(pos, level.dimension().location().toString());
        if (h == null) { player.displayClientMessage(Component.translatable("rats.pizza_rat.hole_empty"), true); return InteractionResult.SUCCESS; }
        SewerManager.enter(sp, h);
        return InteractionResult.SUCCESS;
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0) level.addParticle(CRUMB, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.02, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.01, 0);
        if (state.getValue(LIT)) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.6, pos.getY() + 1.1, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.6, 0, 0.04, 0);
        if (r.nextInt(40) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.SILVERFISH_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.8F, false);
    }
}
