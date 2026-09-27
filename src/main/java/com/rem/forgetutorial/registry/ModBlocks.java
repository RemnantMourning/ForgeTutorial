package com.rem.forgetutorial.registry;

import com.rem.forgetutorial.ForgeTutorial;
import com.rem.forgetutorial.block.CompressedFurnaceBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
