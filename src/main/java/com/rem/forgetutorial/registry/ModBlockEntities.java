package com.rem.forgetutorial.registry;

import com.rem.forgetutorial.ForgeTutorial;
import com.rem.forgetutorial.block.entity.CompressedFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体类型注册。
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ForgeTutorial.MOD_ID);

    /**
     * 八个压缩等级共用同一个方块实体类型，各自的实体在构造时把自己的速度倍率带进去。
     */
    public static final RegistryObject<BlockEntityType<CompressedFurnaceBlockEntity>> COMPRESSED_FURNACE =
            BLOCK_ENTITIES.register("compressed_furnace",
                    () -> BlockEntityType.Builder
                            .of(CompressedFurnaceBlockEntity::new, ModBlocks.compressedFurnaces())
                            .build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
