package com.rem.forgetutorial.worldgen;

import com.rem.forgetutorial.registry.ModFeatures;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import javax.annotation.Nullable;

/**
 * 彩虹果树苗的生长器：把树苗与 {@link RainbowTreeFeature} 的已配置特征接起来。
 */
public class RainbowTreeGrower extends AbstractTreeGrower {

    @Nullable
    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        return ModFeatures.RAINBOW_TREE_KEY;
    }
}
