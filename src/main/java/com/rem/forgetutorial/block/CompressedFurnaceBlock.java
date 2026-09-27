package com.rem.forgetutorial.block;

import com.rem.forgetutorial.Config;
import com.rem.forgetutorial.block.entity.CompressedFurnaceBlockEntity;
import com.rem.forgetutorial.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 压缩熔炉方块。
 * <p>
 * 朝向、点燃状态、GUI、比较器输出、经验掉落等等全部沿用原版 {@link AbstractFurnaceBlock} 的行为，
 * 唯一区别是熔炼速度按 {@link #speedMultiplier} 提升。
 */
public class CompressedFurnaceBlock extends AbstractFurnaceBlock {

    /** 压缩等级：1 表示"压缩熔炉"，8 表示"八重压缩熔炉"。 */
    private final int tier;

    public CompressedFurnaceBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return this.tier;
    }

    /**
     * 熔炼速度倍率，等于 {@code 基础倍率(1 + 0.5 × tier) × 配置加成系数}。
     * <p>
     * 每次读取都会查询配置，因此在游戏内用 {@code /reload} 之外的配置修改
     * （例如 Mod Menu 或直接改 toml 后重启）能立即生效。
     */
    public double getSpeedMultiplier() {
        return Config.getSpeedMultiplier(this.tier);
    }

    /**
     * 相对原版熔炉提升的速度百分比，例如 1.5× 时为 {@code 50}、5.0× 时为 {@code 400}。
     */
    public int getSpeedBonusPercent() {
        return (int) Math.round((getSpeedMultiplier() - 1.0D) * 100.0D);
    }

    /**
     * 方块名（用于 GUI 标题）。
     * <p>
     * 注意不要覆盖继承自 {@code Block} 的 {@code getName()}，两者返回类型不兼容。
     */
    public Component getDisplayName() {
        return Component.translatable("block.forge_tutorial.compressed_furnace_" + this.tier);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressedFurnaceBlockEntity(pos, state);
    }

    /**
     * 复用原版提供的 ticker 工厂：它会在客户端走 clientTick、服务端走 serverTick。
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createFurnaceTicker(level, type, ModBlockEntities.COMPRESSED_FURNACE.get());
    }

    /**
     * 打开熔炉界面，流程与原版 {@code FurnaceBlock#use} 一致。
     */
    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CompressedFurnaceBlockEntity furnace) {
            player.openMenu(furnace);
        }
    }

    /**
     * 复刻原版 {@code AbstractFurnaceBlock#animateTick}，让点燃的压缩熔炉也有火焰与烟雾粒子。
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.1D) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    1.0F, 1.0F, false);
        }

        Direction direction = state.getValue(FACING);
        Direction.Axis axis = direction.getAxis();
        double offset = 0.52D;
        double randomOffset = random.nextDouble() * 0.6D - 0.3D;
        double xOffset = axis == Direction.Axis.X ? direction.getStepX() * offset : randomOffset;
        double yOffset = random.nextDouble() * 6.0D / 16.0D;
        double zOffset = axis == Direction.Axis.Z ? direction.getStepZ() * offset : randomOffset;

        level.addParticle(ParticleTypes.SMOKE, x + xOffset, y + yOffset, z + zOffset, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.FLAME, x + xOffset, y + yOffset, z + zOffset, 0.0D, 0.0D, 0.0D);
    }
}
