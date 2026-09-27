package com.rem.forgetutorial.worldgen;

import com.rem.forgetutorial.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * 彩虹果树的世界生成。
 * <p>
 * 树形参考深色橡树 / 大型橡树：一根较高的主干（8~11 格），顶部向外斜向分枝，
 * 枝条末端托着若干团稀疏的球形叶簇。
 * <p>
 * <b>关键 1：树叶的性质决定它会不会腐烂</b>。
 * 原版 {@link LeavesBlock} 的腐烂逻辑在 <b>{@code tick()}</b>（计划刻）里：
 * <pre>
 *   tick():  if (!PERSISTENT && DISTANCE == 7) { dropResources(); removeBlock(); }
 * </pre>
 * 而 {@code DISTANCE} 由 {@code updateShape} 在邻接方块变化时重算，
 * 一旦被算成 7 就会 <b>scheduleTick</b> —— 这个被调度的 tick 即使之后距离改小，
 * 到期时仍会读<b>当前 state</b> 判断并掉落。
 * <p>
 * <b>关键 2：放置顺序会踩到这个坑</b>。
 * 如果逐格 {@code setBlock(..., 3)} 放树叶，每放一格都会触发周围方块的
 * {@code updateShape}。此时邻接关系还不完整，距离被算成 7 →
 * <b>立刻调度一堆腐烂 tick</b>。等整棵树放完，这些 tick 已经排在队列里，
 * 到期就把树叶干掉 —— 表现为"树长得挺好，但过一会儿成片消失"。
 * <p>
 * <b>解决方案</b>：
 * <ol>
 *     <li>放置时用 {@code setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)}，
 *         即 <b>不发邻接更新</b>，避免任何 {@code updateShape} / 腐烂调度；</li>
 *     <li>形状在内存里用 BFS 算好真实距离（上限 6），直接写成最终值；</li>
 *     <li>BFS 到不了的孤立树叶直接丢弃，保证树冠完全连通。</li>
 * </ol>
 */
public class RainbowTreeFeature extends Feature<NoneFeatureConfiguration> {

    /** 主干高度范围（参考图是一棵高瘦的树，主干很长且裸露）。 */
    private static final int MIN_TRUNK = 11;
    private static final int MAX_TRUNK = 14;

    /** 树冠分层的层数（自顶部向下，每层向水平四方伸枝托着一团叶簇）。 */
    private static final int BRANCH_LAYERS = 3;

    /** 最底层枝条长度（向外伸多远）。 */
    private static final int BRANCH_LENGTH = 4;

    /** 最大的叶簇半径（顶部主冠）。 */
    private static final int MAX_CLUSTER_RADIUS = 3;

    /** 树叶距离上限（原版最大 7 会腐烂，这里封到 6 才安全）。 */
    private static final int MAX_LEAF_DISTANCE = 6;

    /**
     * 放置方块用的 flag。
     * <p>
     * {@code UPDATE_CLIENTS(2)} 让客户端知道方块变了；
     * {@code UPDATE_KNOWN_SHAPE(16)} <b>不触发邻居的 updateShape</b>，
     * 从而避免树叶因放置顺序被误判距离而调度腐烂。
     */
    private static final int PLACE_FLAGS =
            net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                    | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;

    public RainbowTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int trunkHeight = MIN_TRUNK + random.nextInt(MAX_TRUNK - MIN_TRUNK + 1);

        if (!hasSpace(level, origin, trunkHeight)) {
            return false;
        }

        // ---- 第一步：在内存里算出整棵树的形状（相对 origin 的偏移 -> 类型）----
        Map<BlockPos, BlockType> shape = new HashMap<>();
        buildShape(shape, trunkHeight, random);

        // ---- 第二步：从所有原木出发 BFS，给树叶算距离 ----
        Map<BlockPos, Integer> leafDistance = computeLeafDistances(shape);

        // ---- 第三步：真正写进世界 ----
        writeToWorld(level, origin, shape, leafDistance, trunkHeight);

        // ---- 第四步：挂果 ----
        hangFruits(level, origin, trunkHeight, random);

        return true;
    }

    private enum BlockType { LOG, LEAVES }

    /** 单层枝条的配置：{从主干顶部向下第几格, 枝条长度, 末端叶簇半径, 叶簇相对枝头的抬升}。 */
    private static final int[][] TIERS = {
            // 从最下层开始：伸得最远、叶簇最大
            {4, 4, 3, 0},
            {3, 3, 3, 0},
            {2, 2, 2, 1},
    };

    /**
     * 构造树形（全部用相对坐标，方便后续统一计算距离）。
     * <p>
     * 参考图是一棵"高瘦主干 + 分层华盖"的树：
     * 下半截是一根笔直裸露的长主干，上半截从主干向水平四方伸出
     * 若干根短枝，每根枝头托着一团<b>圆润、外缘毛糙</b>的叶簇；
     * 越往上叶簇越小、越靠内，顶部再收一个小尖。
     */
    private void buildShape(Map<BlockPos, BlockType> shape, int trunkHeight, RandomSource random) {
        // 主干
        for (int i = 0; i < trunkHeight; i++) {
            shape.put(new BlockPos(0, i, 0), BlockType.LOG);
        }

        // 分层伸枝 + 枝头叶簇
        for (int layer = 0; layer < BRANCH_LAYERS; layer++) {
            int below = TIERS[layer][0];
            int len = TIERS[layer][1];
            int radius = TIERS[layer][2];
            int lift = TIERS[layer][3];

            int y = trunkHeight - 1 - below;
            if (y < 2) {
                continue;
            }

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos tip = new BlockPos(0, y, 0);
                for (int step = 1; step <= len; step++) {
                    tip = tip.relative(dir);
                    // 枝条末端也是原木，给距离 BFS 提供稳定源头
                    shape.putIfAbsent(tip, BlockType.LOG);
                }
                addLeafCluster(shape, tip.above(lift), radius, random);
            }
        }

        // 顶部主冠：一大一小两层，最上面收一个尖
        addLeafCluster(shape, new BlockPos(0, trunkHeight, 0), 3, random);
        addLeafCluster(shape, new BlockPos(0, trunkHeight + 2, 0), 2, random);
        addLeafCluster(shape, new BlockPos(0, trunkHeight + 3, 0), 1, random);
    }

    /**
     * 一团圆润、外缘毛糙的球形叶簇。
     * <p>
     * 中心密实、外圈随机剔除部分格子，做出参考图那种"云朵感"的毛糙边缘。
     *
     * @param radius 球半径
     */
    private void addLeafCluster(Map<BlockPos, BlockType> shape, BlockPos center,
                                int radius, RandomSource random) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d > radius + 0.4D) {
                        continue;
                    }
                    // 中心密实（100%），外缘越靠边越稀疏（最低约 40%）
                    if (d > radius - 0.8D) {
                        if (random.nextFloat() > 0.42F) {
                            continue;
                        }
                    } else if (random.nextFloat() > 0.88F) {
                        // 内部也偶尔留点小洞，避免一大坨死实
                        continue;
                    }
                    shape.putIfAbsent(center.offset(dx, dy, dz), BlockType.LEAVES);
                }
            }
        }
    }

    /**
     * 从所有原木出发做多源 BFS，算出每片树叶到最近树干的步数，
     * 封顶在 {@link #MAX_LEAF_DISTANCE}。
     * <p>
     * <b>只有被 BFS 到达的树叶才会返回值</b>。没被到达的树叶说明它与树干之间
     * 没有连通的树叶链（孤立叶子），这种叶子在原版里会被判定腐烂并掉落。
     * 调用方应把它们<b>直接丢弃</b>，从而保证写进世界的树冠是完全连通的。
     */
    private Map<BlockPos, Integer> computeLeafDistances(Map<BlockPos, BlockType> shape) {
        Map<BlockPos, Integer> dist = new HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        // 源：所有原木，距离 0（写进方块时 +1）
        for (Map.Entry<BlockPos, BlockType> e : shape.entrySet()) {
            if (e.getValue() == BlockType.LOG) {
                dist.put(e.getKey(), 0);
                queue.add(e.getKey());
            }
        }

        // 六方向 BFS（树叶连通性按原版的 6 邻域算）
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            int d = dist.get(cur);
            if (d >= MAX_LEAF_DISTANCE) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (shape.get(next) != BlockType.LEAVES) {
                    continue;
                }
                if (dist.containsKey(next)) {
                    continue;
                }
                dist.put(next, d + 1);
                queue.add(next);
            }
        }

        return dist;
    }

    /** 把形状写入世界；树叶带正确的 DISTANCE，孤立的树叶直接丢弃。 */
    private void writeToWorld(WorldGenLevel level, BlockPos origin,
                              Map<BlockPos, BlockType> shape,
                              Map<BlockPos, Integer> leafDistances,
                              int trunkHeight) {
        BlockState log = ModBlocks.RAINBOW_LOG.get().defaultBlockState();

        // 铺一层土
        BlockPos ground = origin.below();
        if (isDirtLike(level.getBlockState(ground))) {
            level.setBlock(ground, Blocks.DIRT.defaultBlockState(), PLACE_FLAGS);
        }

        // 关键：全部用 PLACE_FLAGS 放置（不发邻接更新），
        // 这样无论放置顺序如何，都不会触发 LeavesBlock.updateShape /
        // scheduleTick，杜绝"放的过程中被误判距离 7 从而调度腐烂"的问题。
        for (Map.Entry<BlockPos, BlockType> e : shape.entrySet()) {
            if (e.getValue() == BlockType.LOG) {
                level.setBlock(origin.offset(e.getKey()), log, PLACE_FLAGS);
            }
        }

        for (Map.Entry<BlockPos, BlockType> e : shape.entrySet()) {
            if (e.getValue() != BlockType.LEAVES) {
                continue;
            }

            // 没被 BFS 到达的孤立树叶不写进世界，
            // 否则它会被原版判定腐烂而掉落，表现为"树叶一块一块消失"
            Integer d = leafDistances.get(e.getKey());
            if (d == null) {
                continue;
            }

            // PERSISTENT = true：从根上禁用原版的距离腐烂。
            // 彩虹果树叶的存活完全由 RainbowLeavesBlock#randomTick 里的
            // 连通性检测负责（够不到树干才掉落），因此绝不希望原版再插手。
            int distance = Math.min(MAX_LEAF_DISTANCE, d + 1);
            BlockState leaf = ModBlocks.RAINBOW_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.DISTANCE, distance)
                    .setValue(LeavesBlock.PERSISTENT, Boolean.TRUE);
            level.setBlock(origin.offset(e.getKey()), leaf, PLACE_FLAGS);
        }
    }

    /**
     * 在树冠叶簇的下表面挂果。
     * <p>
     * 约束：树叶正下方是空气；果子下方也必须是空气/树叶（保持悬垂）；
     * 离地至少 4 格，避免贴地挂果。
     */
    private void hangFruits(WorldGenLevel level, BlockPos origin, int trunkHeight, RandomSource random) {
        BlockState fruit = ModBlocks.RAINBOW_FRUIT.get().defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // 覆盖整片树冠的扫描半径：最长的枝 + 最大的叶簇半径 + 余量
        int radius = BRANCH_LENGTH + MAX_CLUSTER_RADIUS + 1;
        for (int dy = 0; dy < trunkHeight + 5; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);

                    if (!level.getBlockState(cursor).is(ModBlocks.RAINBOW_LEAVES.get())) {
                        continue;
                    }

                    BlockPos below = cursor.below();

                    if (!level.getBlockState(below).isAir()) {
                        continue;
                    }

                    BlockState underFruit = level.getBlockState(below.below());
                    if (!underFruit.isAir() && !underFruit.is(ModBlocks.RAINBOW_LEAVES.get())) {
                        continue;
                    }

                    if (!hasAirBelow(level, below, 4)) {
                        continue;
                    }

                    if (random.nextInt(3) == 0) {
                        // 同样用 PLACE_FLAGS，避免触发周围树叶的 updateShape
                        level.setBlock(below, fruit, PLACE_FLAGS);
                    }
                }
            }
        }
    }

    /** 从 {@code pos} 往下数 {@code depth} 格是否都是空气/树叶/原木（即悬空）。 */
    private boolean hasAirBelow(WorldGenLevel level, BlockPos pos, int depth) {
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

    /** 落点与主干空间检查。 */
    private boolean hasSpace(WorldGenLevel level, BlockPos origin, int trunkHeight) {
        for (int i = 0; i < trunkHeight; i++) {
            BlockState state = level.getBlockState(origin.above(i));
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }
        return isDirtLike(level.getBlockState(origin.below()));
    }

    private boolean isDirtLike(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(ModBlocks.RAINBOW_SAPLING.get());
    }
}
