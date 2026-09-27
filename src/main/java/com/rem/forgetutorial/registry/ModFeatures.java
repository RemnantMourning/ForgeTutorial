package com.rem.forgetutorial.registry;

import com.rem.forgetutorial.ForgeTutorial;
import com.rem.forgetutorial.worldgen.RainbowTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 世界生成相关的注册：自定义 Feature 与对应的 ConfiguredFeature 键。
 */
public final class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, ForgeTutorial.MOD_ID);

    public static final RegistryObject<RainbowTreeFeature> RAINBOW_TREE =
            FEATURES.register("rainbow_tree", RainbowTreeFeature::new);

    /**
     * 树苗用到的 ConfiguredFeature 键。
     * <p>
     * 这里只登记一个键，具体实例由数据包（{@code data/forge_tutorial/worldgen/configured_feature/rainbow_tree.json}）提供，
     * 因此 {@code AbstractTreeGrower#getConfiguredFeature} 返回键后，原版会自动从注册表里查到它。
     */
    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINBOW_TREE_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            ForgeTutorial.MOD_ID, "rainbow_tree"));

    private ModFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
