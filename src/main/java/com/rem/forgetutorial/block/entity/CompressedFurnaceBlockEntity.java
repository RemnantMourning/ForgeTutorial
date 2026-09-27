package com.rem.forgetutorial.block.entity;

import com.rem.forgetutorial.block.CompressedFurnaceBlock;
import com.rem.forgetutorial.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 压缩熔炉的方块实体。
 * <p>
 * 熔炼流程完全复用原版 {@link AbstractFurnaceBlockEntity}：燃料的燃烧时长、以及"一份燃料能烧几个物品"
 * 都由原版逻辑根据 {@code cookingTotalTime} 换算。速度倍率的实际注入点在
 * {@link com.rem.forgetutorial.mixin.AbstractFurnaceBlockEntityMixin}，它把原版
 * {@code getTotalCookTime} 的返回值按倍率缩短。
 * <p>
 * 于是：燃料的燃烧速度（每秒消耗的燃烧值）保持不变，但同一份燃料能烧的物品变多了。
 * 例如煤炭在原版熔炉烧 8 个物品，在 1.5× 的压缩熔炉里能烧 12 个（1600 ÷ 133 ≈ 12）。
 */
public class CompressedFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    public CompressedFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSED_FURNACE.get(), pos, state, RecipeType.SMELTING);
    }

    /**
     * 速度由 {@link com.rem.forgetutorial.mixin.AbstractFurnaceBlockEntityMixin} 注入到原版
     * {@code getTotalCookTime} 中实现 —— 那里是原版唯一决定"单个物品耗时刻"的地方。
     */

    @Override
    protected Component getDefaultName() {
        return this.getBlockState().getBlock() instanceof CompressedFurnaceBlock block
                ? block.getDisplayName()
                : Component.translatable("block.forge_tutorial.compressed_furnace_1");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FurnaceMenu(containerId, inventory, this, this.dataAccess);
    }
}
