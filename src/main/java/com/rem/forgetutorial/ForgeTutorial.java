package com.rem.forgetutorial;

import com.rem.forgetutorial.command.ModCommands;
import com.rem.forgetutorial.registry.ModBlockEntities;
import com.rem.forgetutorial.registry.ModBlocks;
import com.rem.forgetutorial.registry.ModCreativeTabs;
import com.rem.forgetutorial.registry.ModFeatures;
import com.rem.forgetutorial.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraft.resources.ResourceLocation.tryBuild;

@Mod(ForgeTutorial.MOD_ID)
public class ForgeTutorial {
    public static final String MOD_ID = "forge_tutorial";
    public static final String NAME = "Forge Tutorial";
    public static final Logger LOGGER = LogManager.getLogger();

    public ForgeTutorial(FMLJavaModLoadingContext context) {
        IEventBus MOD_BUS = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(ModCommands::register);

        // 方块 -> 方块实体 -> 创造物品栏，顺序不能颠倒：
        // ModBlockEntities 构建时就会读取 ModBlocks 里已注册的方块实例。
        ModBlocks.register(MOD_BUS);
        ModItems.register(MOD_BUS);
        ModFeatures.register(MOD_BUS);
        ModBlockEntities.register(MOD_BUS);
        ModCreativeTabs.register(MOD_BUS);

        // 配置文件：config/forge_tutorial-common.toml
        Config.register(context);

        ResourceLocation item_id = ForgeTutorial.id("example_block");
    }

    public static ResourceLocation id(String name) {
        return tryBuild(MOD_ID, camelToSnake(name));
    }

    /**
     * 检查指定mod是否已加载
     *
     * @param modId 要检查的mod ID the mod id to check for
     * @return 指定ID的mod是否已加载 if the mod whose id is {@code modId} is loaded or not
     */
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static class Mods {

        public static boolean isAE2Loaded() {
            return isModLoaded("ae2");
        }

        public static boolean isFTBTeamsLoaded() {
            return isModLoaded("ftbteams");
        }

        public static boolean isCuriosLoaded() {
            return isModLoaded("curios");
        }
    }

    // "CreeperAnimal" -> "creeper_animal"
    public static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2") // 处理连续大写如 "XMLParser" -> "XML_Parser"
                .toLowerCase(java.util.Locale.US);
    }

}
