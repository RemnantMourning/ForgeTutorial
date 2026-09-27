package com.rem.forgetutorial.registry;

import com.rem.forgetutorial.ForgeTutorial;
import com.rem.forgetutorial.item.RainbowAppleItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本 mod 的普通物品（非方块物品）注册。
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ForgeTutorial.MOD_ID);

    /**
     * 彩虹苹果：可以吃，吃完把自己炸上天并附加生命恢复与抗性提升。
     * <p>
     * 食物属性参考原版苹果（回复 4 点饥饿值），但可以一直吃，方便测试。
     */
    public static final RegistryObject<Item> RAINBOW_APPLE = ITEMS.register("rainbow_apple",
            () -> new RainbowAppleItem(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(0.3F)
                            .alwaysEat()
                            .build())
                    .stacksTo(64)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
