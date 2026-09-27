package com.rem.forgetutorial.block;

import com.rem.forgetutorial.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * 彩虹果树叶。
 * <p>
 * <b>为什么不能直接用原版的腐烂机制？</b>
 * <p>
 * 原版 {@link LeavesBlock} 的腐烂完全围绕 {@code DISTANCE} 属性：
 * <pre>
 *   decaying(state)   = !PERSISTENT &amp;&amp; DISTANCE == 7
 *   randomTick(...)   = if (decaying) { dropResources(); removeBlock(); }
 * </pre>
 * 而 {@code DISTANCE} 由 {@code updateShape} 在<b>任何邻接方块变化</b>时重算：
 * 相邻树叶距离最小值 + 1。世界生成时我们逐格放置，
 * 中途邻接关系不完整 → 距离被算成 7 → 一旦有 scheduled tick / 随机刻到期，
 * 就会把叶子打掉。表现为"树长得挺好看，可过一会儿树叶成片消失"。
 * <p>
 * 与其和这套距离机制搏斗，这里干脆 <b>接管腐烂逻辑</b>：
 * <ol>
 *     <li>{@link #decaying} 恒返回 {@code false} —— 原版永远不会主动打掉我们的树叶；</li>
 *     <li>{@link #isRandomlyTicking} 恒返回 {@code true} —— 让所有树叶都能收到随机刻
 *         （顺带修复了"正常树叶不随机刻 → 从不结果"的隐藏 bug）；</li>
 *     <li>{@link #randomTick} 里做我们自己的判定：
 *         <b>从本格出发 BFS，若 6 格内够不到任何彩虹果木，说明它是孤立的，才掉落</b>；
 *         否则保持原样，并按概率在下方结出彩虹果。</li>
 * </ol>
 * 这样"树叶之间的连接"就成了唯一的存活条件 ——
 * 连在一起就永远不掉，树被砍断、叶子悬空断开时才会自然枯萎，
 * 完全符合"不是永远不腐烂，只是树叶要连起来"的诉求。
 */
public class RainbowLeavesBlock extends LeavesBlock {

    /** 判定树叶与树干是否连通时的最大搜索步数（和原版 7 的语义对齐）。 */
    private static final int MAX_CONNECT_DISTANCE = 6;

    /** 挂果位置距离地面至少要有这么多格空气。 */
    private static final int MIN_AIR_BELOW = 3;

    /** 挂果概率分母：约 1/16 的随机刻尝试一次。 */
    private static final int FRUIT_CHANCE = 16;

    public RainbowLeavesBlock(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // 一、关掉原版腐烂
    // ------------------------------------------------------------------

    /**
     * 始终认为"不腐烂"，从根上掐断原版 {@code randomTick} 里的掉落分支。
     * <p>
     * 这个方法在 1.20.1 里是 {@code protected}，可以安全覆盖。
     */
    @Override
    protected boolean decaying(BlockState state) {
        return false;
    }

    /**
     * 让所有树叶都能被随机刻选中。
     * <p>
     * 原版这里只在 {@code DISTANCE == 7} 时返回 true，意味着<b>正常的、连通的树叶
     * 根本收不到随机刻</b> —— 我们依赖随机刻结果，所以必须放开。
     */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    // ------------------------------------------------------------------
    // 二、自定义随机刻：连通性腐烂 + 结果
    // ------------------------------------------------------------------

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 不调用 super：super 会走原版距离腐烂逻辑，这里完全自己来。

        // 1) 孤立检测：够不到任何彩虹果木 → 自然枯萎掉落
        if (!isConnectedToLog(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }

        // 2) 结果：约 1/16 概率尝试
        if (random.nextInt(FRUIT_CHANCE) != 0) {
            return;
        }

        BlockPos below = pos.below();

        // 叶子正下方必须是空气
        if (!level.getBlockState(below).isAir()) {
            return;
        }

        // 果子下方也必须空着（悬垂），不能直接坐在实体方块上，
        // 否则 RainbowFruitBlock#canSurvive 判定失败，果子会立刻掉落
        BlockState underFruit = level.getBlockState(below.below());
        if (!underFruit.isAir() && !underFruit.is(ModBlocks.RAINBOW_LEAVES.get())) {
            return;
        }

        // 离地至少 MIN_AIR_BELOW 格，避免矮树/贴地挂果
        if (!hasClearSpaceBelow(level, below, MIN_AIR_BELOW)) {
            return;
        }

        level.setBlockAndUpdate(below, ModBlocks.RAINBOW_FRUIT.get().defaultBlockState());
    }

    // ------------------------------------------------------------------
    // 三、连通性判定（从本格出发，六方向 BFS 找彩虹果木）
    // ------------------------------------------------------------------

    /**
     * 从 {@code start} 出发，沿<b>树叶/原木</b>六方向扩散，
     * 在 {@link #MAX_CONNECT_DISTANCE} 步内能否够到任意彩虹果木。
     * <p>
     * 思路和原版的"距离扩散"一致，但这里是<b>主动、一次性</b>地查，
     * 不依赖会被邻居更新改写的 {@code DISTANCE} 属性，因此不会被放置顺序坑到。
     */
    private boolean isConnectedToLog(ServerLevel level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> frontier = new ArrayDeque<>();
        Queue<Integer> depths = new ArrayDeque<>();

        visited.add(start);
        frontier.add(start);
        depths.add(0);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!frontier.isEmpty()) {
            BlockPos cur = frontier.poll();
            int depth = depths.poll();

            if (depth >= MAX_CONNECT_DISTANCE) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                cursor.set(cur).move(dir);
                BlockPos next = cursor.immutable();

                if (visited.contains(next)) {
                    continue;
                }

                BlockState ns = level.getBlockState(next);

                // 碰到彩虹果木 → 连上了
                if (ns.is(ModBlocks.RAINBOW_LOG.get()) || ns.is(BlockTags.LOGS)) {
                    return true;
                }

                // 只沿树叶继续扩散
                if (!ns.is(ModBlocks.RAINBOW_LEAVES.get())) {
                    continue;
                }

                visited.add(next);
                frontier.add(next);
                depths.add(depth + 1);
            }
        }

        return false;
    }

    /** 从 {@code pos} 往下数 {@code depth} 格是否都是空气/树叶/原木（即悬空）。 */
    private boolean hasClearSpaceBelow(ServerLevel level, BlockPos pos, int depth) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < depth; i++) {
            cursor.move(Direction.DOWN);
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.is(ModBlocks.RAINBOW_LEAVES.get())
                    && !state.is(BlockTags.LOGS)) {
                return false;
            }
        }
        return true;
    }
}
