package com.rem.forgetutorial.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /diamondzombie [pos]}
 * <p>
 * 在执行者所在位置（或给定坐标）生成一只装备全套钻石盔甲、手持钻石剑的僵尸。
 * 盔甲不会掉落死亡 items，也不会被地上的垃圾替换掉。
 */
public final class DiamondZombieCommand {

    private static final SimpleCommandExceptionType ERROR_SPAWN_FAILED =
            new SimpleCommandExceptionType(Component.translatable("commands.forge_tutorial.diamond_zombie.failed"));

    private DiamondZombieCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("diamondzombie")
                        .requires(source -> source.hasPermission(2))
                        // /diamondzombie
                        .executes(ctx -> spawn(ctx.getSource(), ctx.getSource().getPosition()))
                        // /diamondzombie <x> <y> <z>  （也支持 ~ ^ 相对/局部坐标）
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> spawn(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos"))))
        );
    }

    private static int spawn(CommandSourceStack source, Vec3 pos) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();

        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw ERROR_SPAWN_FAILED.create();
        }

        zombie.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        zombie.setYBodyRot(zombie.getYRot());

        // 全套钻石装备
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

        // 死亡时不掉装备，想要掉落把这里的 0.0F 改回 1.0F 即可
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            zombie.setDropChance(slot, 0.0F);
        }

        // 防止它捡起地上的东西把钻石套换掉，同时避免自然消失
        zombie.setCanPickUpLoot(false);
        zombie.setPersistenceRequired();

        level.addFreshEntity(zombie);

        BlockPos at = BlockPos.containing(pos);
        source.sendSuccess(
                () -> Component.translatable("commands.forge_tutorial.diamond_zombie.success", at.toShortString()),
                true
        );
        return 1;
    }
}
