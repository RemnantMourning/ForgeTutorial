package com.rem.forgetutorial.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 集中注册本 mod 的所有指令。
 * <p>
 * Forge 会在服务端启动时（含单人世界）派发 {@link RegisterCommandsEvent}，此时 Brigadier 的
 * {@code Commands} 已经可用，可以在这里安全地把节点挂进 dispatcher。
 */
@Mod.EventBusSubscriber
public final class ModCommands {

    private ModCommands() {
    }

    /**
     * 由主类在 {@code MinecraftForge.EVENT_BUS} 上注册为监听器。
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        DiamondZombieCommand.register(event.getDispatcher());
    }
}
