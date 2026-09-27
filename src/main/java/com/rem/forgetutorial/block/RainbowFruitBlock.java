package com.rem.forgetutorial.block;

import com.rem.forgetutorial.registry.ModBlocks;
import com.rem.forgetutorial.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 彩虹果。
 * <p>
 * 像葡萄一样从树叶下方垂下来。方块高度占满 <b>y=4 ~ y=16</b>，
 * 顶部 4 像素直接"顶"在上方树叶的下表面，视觉上和树叶是连在一起的，
 * 不会出现果实与树叶之间悬空断开的情况。
 * <p>
 * 有 {@code age} 生长阶段：
 * <ul>
 *     <li>age 0 —— 刚长出来的青果（不能采）</li>
 *     <li>age 1 —— 半熟</li>
 *     <li>age 2 —— 成熟，可以右键采摘，掉落 {@link ModItems#RAINBOW_APPLE 彩虹苹果}</li>
 * </ul>
 * 采摘后方块消失，树叶的随机刻会再长出新的果子，形成可持续循环。
 */
public class RainbowFruitBlock extends Block implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;

    /** 果实从幼到熟逐渐变长、变饱满；顶部始终紧贴上方树叶（y 上限都是 16）。 */
    private static final VoxelShape[] SHAPES = {
            Block.box(6.0D, 8.0D, 6.0D, 10.0D, 16.0D, 10.0D),
            Block.box(5.0D, 5.5D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.box(4.0D, 4.0D, 4.0D, 12.0D, 16.0D, 12.0D),
    };

    public RainbowFruitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    /** 必须挂在会结果的树叶或另一个彩虹果下面。 */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(ModBlocks.RAINBOW_LEAVES.get()) || above.is(this);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** 随机刻缓慢推进生长：约 1/10 概率进一档。 */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= 2) {
            return;
        }
        if (random.nextInt(10) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(AGE, age + 1));
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(AGE) < 2) {
            // 没熟，摘不了
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(ModItems.RAINBOW_APPLE.get(), 1 + level.random.nextInt(2)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 只有没熟透的果实才需要随机刻。 */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 2;
    }

    // ------------------------------------------------------------ 骨粉催熟
    // 骨粉只推进「一个」阶段，不直接催到成熟，避免一次骨粉就白拿果子。

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < 2;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(AGE, state.getValue(AGE) + 1));
    }
}
