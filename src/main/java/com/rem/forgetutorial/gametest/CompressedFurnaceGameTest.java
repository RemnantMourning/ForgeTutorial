package com.rem.forgetutorial.gametest;

import com.rem.forgetutorial.ForgeTutorial;
import com.rem.forgetutorial.block.CompressedFurnaceBlock;
import com.rem.forgetutorial.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;

/**
 * 用游戏内测试验证压缩熔炉的速度倍率。
 * <p>
 * 直接调用原版私有的 {@code getTotalCookTime}（速度的唯一决定因素）并与理论值比对，
 * 这样既验证了 mixin 注入确实生效，也验证了"每压缩一级 +50%"的数值正确。
 * <p>
 * 同时对照原版熔炉，确认我们的注入没有误伤普通熔炉。
 */
@GameTestHolder(ForgeTutorial.MOD_ID)
@PrefixGameTestTemplate(false)
public class CompressedFurnaceGameTest {

    private static final String TEMPLATE = "forge_tutorial:empty";

    /** 反射拿到原版的 getTotalCookTime(Level, AbstractFurnaceBlockEntity)。 */
    private static Method totalCookTimeMethod() throws NoSuchMethodException {
        Method method = AbstractFurnaceBlockEntity.class.getDeclaredMethod(
                "getTotalCookTime",
                net.minecraft.world.level.Level.class,
                AbstractFurnaceBlockEntity.class);
        method.setAccessible(true);
        return method;
    }

    private static int totalCookTime(GameTestHelper helper, BlockPos pos) throws Exception {
        AbstractFurnaceBlockEntity furnace =
                (AbstractFurnaceBlockEntity) helper.getBlockEntity(pos);
        return (int) totalCookTimeMethod().invoke(null, helper.getLevel(), furnace);
    }

    /**
     * 逐级校验单个物品的熔炼耗时：200 / (1 + 0.5 × tier)，四舍五入取整。
     */
    @GameTest(template = TEMPLATE)
    public static void speedScalesWithTier(GameTestHelper helper) {
        int[] expected = {133, 100, 80, 67, 57, 50, 44, 40};

        for (int tier = 1; tier <= ModBlocks.MAX_TIER; tier++) {
            BlockPos pos = new BlockPos(tier * 2, 2, 2);
            helper.setBlock(pos, ModBlocks.get(tier));

            try {
                int actual = totalCookTime(helper, pos);
                if (actual != expected[tier - 1]) {
                    helper.fail("第 " + tier + " 级压缩熔炉单个物品耗时 " + actual
                            + " tick，期望 " + expected[tier - 1]);
                    return;
                }
            } catch (Exception e) {
                helper.fail("第 " + tier + " 级压缩熔炉读取耗时失败: " + e);
                return;
            }
        }

        helper.succeed();
    }

    /**
     * 燃料利用率：一份煤炭（1600 tick）能烧几个物品 = 1600 / 单个物品耗时。
     * <p>
     * 原版熔炉是 8 个，压缩熔炉应当依次变为 12 / 16 / 20 / 24 / 28 / 32 / 36 / 40。
     */
    @GameTest(template = TEMPLATE)
    public static void fuelYieldScales(GameTestHelper helper) {
        int[] expectedYield = {12, 16, 20, 24, 28, 32, 36, 40};

        for (int tier = 1; tier <= ModBlocks.MAX_TIER; tier++) {
            BlockPos pos = new BlockPos(tier * 2, 2, 4);
            helper.setBlock(pos, ModBlocks.get(tier));

            try {
                int perItem = totalCookTime(helper, pos);
                // 原版燃烧逻辑：ticksConsumed = cookTime / cookingTotalTime，向下取整
                int yield = 1600 / perItem;
                if (yield != expectedYield[tier - 1]) {
                    helper.fail("第 " + tier + " 级压缩熔炉一份煤能烧 " + yield
                            + " 个物品，期望 " + expectedYield[tier - 1]);
                    return;
                }
            } catch (Exception e) {
                helper.fail("第 " + tier + " 级压缩熔炉计算燃料利用率失败: " + e);
                return;
            }
        }

        helper.succeed();
    }

    /**
     * 对照测试：原版熔炉仍然是 200 tick / 一份煤 8 个物品，确保注入没有误伤它。
     */
    @GameTest(template = TEMPLATE)
    public static void vanillaFurnaceUnaffected(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 6);
        helper.setBlock(pos, Blocks.FURNACE);

        try {
            int perItem = totalCookTime(helper, pos);
            if (perItem != 200) {
                helper.fail("原版熔炉单个物品耗时被改成了 " + perItem + " tick，期望 200");
                return;
            }
        } catch (Exception e) {
            helper.fail("读取原版熔炉耗时失败: " + e);
            return;
        }

        helper.succeed();
    }

    /**
     * 校验速度倍率与等级定义的对应关系（1.5× ~ 5.0×）。
     */
    @GameTest(template = TEMPLATE)
    public static void speedMultiplierDefinition(GameTestHelper helper) {
        for (int tier = 1; tier <= ModBlocks.MAX_TIER; tier++) {
            CompressedFurnaceBlock block = ModBlocks.get(tier);
            double expected = 1.0D + 0.5D * tier;
            if (Math.abs(block.getSpeedMultiplier() - expected) > 1.0E-9) {
                helper.fail("第 " + tier + " 级速度倍率是 " + block.getSpeedMultiplier()
                        + "，期望 " + expected);
                return;
            }
        }
        helper.succeed();
    }
}
