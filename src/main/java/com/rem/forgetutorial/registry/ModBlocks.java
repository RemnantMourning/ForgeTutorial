package com.rem.forgetutorial.registry;

import com.rem.forgetutorial.ForgeTutorial;
import com.rem.forgetutorial.block.CompressedFurnaceBlock;
import com.rem.forgetutorial.block.RainbowFruitBlock;
import com.rem.forgetutorial.block.RainbowLeavesBlock;
import com.rem.forgetutorial.block.RainbowLogBlock;
import com.rem.forgetutorial.worldgen.RainbowTreeGrower;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 方块与方块物品注册。
 */
public final class ModBlocks {

    /** 压缩熔炉的最大等级：压缩熔炉(1) ~ 八重压缩熔炉(8)。 */
    public static final int MAX_TIER = 8;

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ForgeTutorial.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ForgeTutorial.MOD_ID);

    private static final List<RegistryObject<CompressedFurnaceBlock>> TIER_BLOCKS = new ArrayList<>(MAX_TIER);
    private static final List<RegistryObject<Item>> TIER_ITEMS = new ArrayList<>(MAX_TIER);

    static {
        for (int tier = 1; tier <= MAX_TIER; tier++) {
            final int level = tier;
            RegistryObject<CompressedFurnaceBlock> block = BLOCKS.register(
                    "compressed_furnace_" + level,
                    () -> new CompressedFurnaceBlock(furnaceProperties(), level)
            );
            TIER_BLOCKS.add(block);
            TIER_ITEMS.add(ITEMS.register(
                    "compressed_furnace_" + level,
                    () -> new BlockItem(block.get(), new Item.Properties())
            ));
        }
    }

    /**
     * 复制原版熔炉的方块属性（硬度 3.5、抗爆 3.5、需要石镐及以上才能采集）。
     */
    private static BlockBehaviour.Properties furnaceProperties() {
        return BlockBehaviour.Properties.copy(Blocks.FURNACE).mapColor(MapColor.STONE);
    }

    // ------------------------------------------------------------------
    // 彩虹果树系列
    // ------------------------------------------------------------------

    /** 彩虹果木：仿原版原木（硬度 2.0、可燃、斧头高效）。 */
    public static final RegistryObject<RainbowLogBlock> RAINBOW_LOG =
            BLOCKS.register("rainbow_log", () -> new RainbowLogBlock(
                    BlockBehaviour.Properties.copy(Blocks.OAK_LOG)
                            .mapColor(MapColor.COLOR_MAGENTA)));

    /** 彩虹果树苗。 */
    public static final RegistryObject<SaplingBlock> RAINBOW_SAPLING =
            BLOCKS.register("rainbow_sapling", () -> new SaplingBlock(
                    new RainbowTreeGrower(),
                    BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                            .mapColor(MapColor.PLANT)));

    /** 彩虹果树叶：会随随机刻在下方结出彩虹果。 */
    public static final RegistryObject<RainbowLeavesBlock> RAINBOW_LEAVES =
            BLOCKS.register("rainbow_leaves", () -> new RainbowLeavesBlock(
                    BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
                            .mapColor(MapColor.PLANT)
                            .randomTicks()));

    /** 彩虹果：挂在树叶下方的果实，成熟后可采摘。 */
    public static final RegistryObject<RainbowFruitBlock> RAINBOW_FRUIT =
            BLOCKS.register("rainbow_fruit", () -> new RainbowFruitBlock(
                    BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH)
                            .mapColor(MapColor.COLOR_MAGENTA)
                            .randomTicks()
                            .noCollission()));

    static {
        // 彩虹果树的方块物品
        ITEMS.register("rainbow_log", () -> new BlockItem(RAINBOW_LOG.get(), new Item.Properties()));
        ITEMS.register("rainbow_sapling", () -> new BlockItem(RAINBOW_SAPLING.get(), new Item.Properties()));
        // 树叶：方便直接放置 / 在物品栏里看到
        ITEMS.register("rainbow_leaves", () -> new BlockItem(RAINBOW_LEAVES.get(), new Item.Properties()));
        // 果实：可直接放置观察生长
        ITEMS.register("rainbow_fruit", () -> new BlockItem(RAINBOW_FRUIT.get(), new Item.Properties()));
    }

    private ModBlocks() {
    }

    /** 第 {@code tier} 级压缩熔炉的方块（1 表示"压缩熔炉"）。 */
    public static CompressedFurnaceBlock get(int tier) {
        return TIER_BLOCKS.get(tier - 1).get();
    }

    /** 第 {@code tier} 级压缩熔炉对应的物品。 */
    public static Item getItem(int tier) {
        return TIER_ITEMS.get(tier - 1).get();
    }

    /** 全部八个等级的物品，按等级升序。 */
    public static List<Item> allItems() {
        return TIER_ITEMS.stream().map(Supplier::get).toList();
    }

    /** 全部八个等级的方块，供方块实体类型注册使用。 */
    public static Block[] compressedFurnaces() {
        return TIER_BLOCKS.stream().map(Supplier::get).toArray(Block[]::new);
    }

    /** 全部八个等级的方块，按等级升序（只读）。 */
    public static List<RegistryObject<CompressedFurnaceBlock>> all() {
        return Collections.unmodifiableList(TIER_BLOCKS);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
