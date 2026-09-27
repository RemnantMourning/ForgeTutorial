package com.rem.forgetutorial.client;

import com.rem.forgetutorial.registry.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端渲染设置。
 * <p>
 * 树苗（十字平面）和果实（带透明边缘的小方块）必须使用 <b>cutout</b> 渲染层，
 * 否则贴图的透明像素会被渲染成不透明的灰白色（就是玩家看到的"白边"）。
 * <p>
 * 树叶用 cutout_mipped（和原版树叶一致），保证远处 mipmap 不出白边。
 */
@Mod.EventBusSubscriber(modid = com.rem.forgetutorial.ForgeTutorial.MOD_ID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 树苗：十字平面，alpha 必须被裁剪
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RAINBOW_SAPLING.get(), RenderType.cutout());

            // 果实：带透明边缘，同样需要 cutout
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RAINBOW_FRUIT.get(), RenderType.cutout());

            // 树叶：和原版树叶一致
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RAINBOW_LEAVES.get(), RenderType.cutoutMipped());

            // 原木是实心方块，用默认的 solid，无需设置
        });
    }
}
