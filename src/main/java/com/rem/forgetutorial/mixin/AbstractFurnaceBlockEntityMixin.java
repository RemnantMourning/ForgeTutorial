package com.rem.forgetutorial.mixin;

import com.rem.forgetutorial.block.CompressedFurnaceBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 压缩熔炉的速度来源。
 * <p>
 * 原版把"熔炼一个物品所需 tick 数"写死在静态方法 {@code getTotalCookTime(Level, AbstractFurnaceBlockEntity)} 里，
 * 返回恒定的 {@code cookingTotalTime}（默认 200），子类无法改写。这里在方法返回前判断熔炉方块是否为
 * {@link CompressedFurnaceBlock}，是则把返回值按速度倍率缩短。
 * <p>
 * 之所以能改变"一份燃料能烧几个物品"：原版的燃烧逻辑里
 * {@code ticksConsumed = cookTime / cookingTotalTime}，燃料本身燃烧值不变，
 * 而单个物品耗时刻变短后，每一份燃料自然就能烧更多物品。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Inject(method = "getTotalCookTime", at = @At("RETURN"), cancellable = true)
    private static void forge_tutorial$applyCompressedSpeed(
            Level level,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfoReturnable<Integer> cir) {
        if (furnace.getBlockState().getBlock() instanceof CompressedFurnaceBlock block) {
            double multiplier = block.getSpeedMultiplier();
            if (multiplier > 1.0D) {
                cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValue() / multiplier)));
            }
        }    }
}
