package com.rem.forgetutorial;

import com.rem.forgetutorial.registry.ModBlocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 本 mod 的配置文件（{@code config/forge_tutorial-common.toml}）。
 * <p>
 * 每种压缩熔炉有独立的"速度加成系数"条目：系数为 {@code 1.0} 表示速度与原版熔炉完全一致，
 * {@code 2.0} 表示速度翻倍，以此类推。实际速度倍率 = {@code 基础倍率(1+0.5×等级) × 加成系数}。
 * <p>
 * 之所以用 {@code COMMON} 类型：熔炼速度属于游戏逻辑，单人/多人世界里服务端说了算，
 * 但同时也需要同步给客户端用于显示，{@code COMMON} 正好满足这两点。
 */
public final class Config {

    public static final ForgeConfigSpec SPEC;

    /** 每个等级对应的速度加成系数条目，下标 0 对应等级 1。 */
    private static final List<ForgeConfigSpec.DoubleValue> SPEED_BONUSES = new ArrayList<>(ModBlocks.MAX_TIER);

    /** 是否启用配置中的速度加成系数；关掉就退回纯"每级 +50%"的默认行为。 */
    private static final ForgeConfigSpec.BooleanValue USE_CONFIG_MULTIPLIERS;

    /** 速度加成系数的允许范围。 */
    private static final double MIN_BONUS = 0.0D;
    private static final double MAX_BONUS = 100.0D;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "压缩熔炉配置",
                "Compressed Furnace settings"
        ).push("compressed_furnaces");

        USE_CONFIG_MULTIPLIERS = builder
                .comment(
                        "是否让下面的速度加成系数生效。",
                        "设为 false 时，所有压缩熔炉使用内置规则：速度倍率 = 1 + 0.5 × 压缩等级。",
                        "",
                        "Whether the speed bonus multipliers below take effect.",
                        "When false, every compressed furnace falls back to the built-in rule: 1 + 0.5 * tier."
                )
                .define("useConfigMultipliers", true);

        builder.comment(
                "每种压缩熔炉的额外速度加成系数（乘算）。",
                "1.0 = 保持该等级的基础速度；2.0 = 该等级速度再翻一倍；0.5 = 减速一半。",
                "基础速度倍率为 1 + 0.5 × 压缩等级（压缩熔炉 1.5×，八重压缩熔炉 5.0×）。",
                "例如把 compressedFurnace1 设为 2.0，压缩熔炉的最终速度倍率就是 1.5 × 2.0 = 3.0。",
                "",
                "Per-tier extra speed multiplier (multiplicative).",
                "1.0 = keep that tier's base speed, 2.0 = double it, 0.5 = halve it.",
                "Base multiplier is 1 + 0.5 * tier (tier 1 = 1.5x ... tier 8 = 5.0x)."
        ).push("speedBonuses");

        String[] chinese = {"压缩熔炉", "二重压缩熔炉", "三重压缩熔炉", "四重压缩熔炉",
                "五重压缩熔炉", "六重压缩熔炉", "七重压缩熔炉", "八重压缩熔炉"};
        String[] english = {"Compressed Furnace", "Double Compressed Furnace", "Triple Compressed Furnace",
                "Quadruple Compressed Furnace", "Quintuple Compressed Furnace", "Sextuple Compressed Furnace",
                "Septuple Compressed Furnace", "Octuple Compressed Furnace"};

        for (int tier = 1; tier <= ModBlocks.MAX_TIER; tier++) {
            double base = 1.0D + 0.5D * tier;
            SPEED_BONUSES.add(builder
                    .comment(
                            String.format("第 %d 级 %s：基础倍率 %.1f×。", tier, chinese[tier - 1], base),
                            String.format("Tier %d %s - base multiplier %.1fx.", tier, english[tier - 1], base)
                    )
                    .defineInRange("compressedFurnace" + tier, 1.0D, MIN_BONUS, MAX_BONUS));
        }

        builder.pop(); // speedBonuses
        builder.pop(); // compressed_furnaces

        SPEC = builder.build();
    }

    private Config() {
    }

    /** 在 mod 构造阶段注册配置文件。 */
    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC, "forge_tutorial-common.toml");
    }

    /**
     * 第 {@code tier} 级压缩熔炉的最终速度倍率。
     * <p>
     * = 基础倍率(1 + 0.5 × tier) × 配置的加成系数。
     * 配置文件尚未加载时（例如数据生成阶段）返回基础倍率，保证行为可预期。
     *
     * @param tier 压缩等级，1 ~ {@link ModBlocks#MAX_TIER}
     */
    public static double getSpeedMultiplier(int tier) {
        double base = 1.0D + 0.5D * tier;
        if (tier < 1 || tier > SPEED_BONUSES.size()) {
            return base;
        }

        try {
            if (!USE_CONFIG_MULTIPLIERS.get()) {
                return base;
            }
            double bonus = SPEED_BONUSES.get(tier - 1).get();
            return base * bonus;
        } catch (IllegalStateException e) {
            // 配置尚未加载/已卸载
            return base;
        }
    }
}
