package com.rem.forgetutorial.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 彩虹苹果。
 * <p>
 * 吃下后会把食用者向上炸飞到天空（不会造成任何伤害），并给予
 * <ul>
 *     <li>生命恢复 20 秒</li>
 *     <li>抗性提升 20 秒</li>
 * </ul>
 * <p>
 * 这里没有使用原版爆炸实体：{@code Level#explode} 难免会破坏地形、并对其余生物造成伤害，
 * 而需求是"只把自己炸飞、但没有伤害"。因此改为显式地给自己施加一个向上的速度冲量，
 * 再播放爆炸音效与粒子，观感一致却完全不产生伤害判定。
 */
public class RainbowAppleItem extends Item {

    /**
     * 向上飞行的初始速度。
     * <p>
     * 原版重力约 {@code 0.08 方块/tick²}、终端速度 {@code 3.92}，粗略估算最大高度 ≈ {@code v² / (2g)}：
     * {@code 3.0² / 0.16 ≈ 56} 个方块，足以"炸到天空"。
     */
    private static final double LAUNCH_VELOCITY = 3.0D;

    /** 效果持续时间：20 秒 = 400 tick。 */
    private static final int EFFECT_DURATION_TICKS = 20 * 20;

    /** 效果等级，0 表示 I 级。 */
    private static final int AMPLIFIER = 0;

    /** 物品冷却：3 秒 = 60 tick。 */
    public static final int COOLDOWN_TICKS = 3 * 20;

    public RainbowAppleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide) {
            // 1. 向上炸飞：只改写竖直方向的速度，水平方向保持原样
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, LAUNCH_VELOCITY, motion.z);
            // 必须标记 hasImpulse，否则服务端会因为"玩家没有主动移动"而把速度重置掉
            entity.hasImpulse = true;
            entity.hurtMarked = true;

            // 2. 正面效果：生命恢复 + 抗性提升，各 20 秒
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, AMPLIFIER));

            // 3. 3 秒冷却，防止连续食用把自己喷到世界外面去
            if (entity instanceof Player player) {
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
        }

        // 4. 爆炸音效与粒子：纯表现层，不产生任何伤害
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.6F);
        if (level.isClientSide) {
            level.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                    entity.getX(), entity.getY() + 0.5D, entity.getZ(), 0.0D, 0.0D, 0.0D);
        }

        return result;
    }

    /**
     * 彩色材质 + 附魔光效，让这个道具在物品栏里一眼可辨。
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.forge_tutorial.rainbow_apple.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.forge_tutorial.rainbow_apple.cooldown", COOLDOWN_TICKS / 20)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 效果时长（秒），供外部复用以避免魔法数字散落。 */
    public static int getEffectDurationSeconds() {
        return EFFECT_DURATION_TICKS / 20;
    }

    /** 冷却时长（秒）。 */
    public static int getCooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }
}
