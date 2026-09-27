package com.rem.forgetutorial.registry;

import com.rem.forgetutorial.ForgeTutorial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式物品栏。
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(ResourceLocation.fromNamespaceAndPath("minecraft", "creative_mode_tab"),
                    ForgeTutorial.MOD_ID);

    public static final RegistryObject<CreativeModeTab> COMPRESSED_FURNACE_TAB =
            CREATIVE_MODE_TABS.register("compressed_furnaces", () -> CreativeModeTab.builder()
                    // 图标使用八重压缩熔炉，一眼就能认出是"最厉害的那个"
                    .icon(() -> new ItemStack(ModBlocks.getItem(ModBlocks.MAX_TIER)))
                    .title(Component.translatable("itemGroup.forge_tutorial.compressed_furnaces"))
                    .displayItems((parameters, output) -> {
                        for (int tier = 1; tier <= ModBlocks.MAX_TIER; tier++) {
                            output.accept(ModBlocks.getItem(tier));
                        }
                    })
                    .build());

    /**
     * "彩虹物品栏"：收纳所有彩虹系列物品。
     */
    public static final RegistryObject<CreativeModeTab> RAINBOW_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("rainbow_items", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.RAINBOW_APPLE.get()))
                    .title(Component.translatable("itemGroup.forge_tutorial.rainbow_items"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RAINBOW_APPLE.get());
                        output.accept(ModBlocks.RAINBOW_SAPLING.get());
                        output.accept(ModBlocks.RAINBOW_LOG.get());
                        output.accept(ModBlocks.RAINBOW_LEAVES.get());
                        output.accept(ModBlocks.RAINBOW_FRUIT.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}
