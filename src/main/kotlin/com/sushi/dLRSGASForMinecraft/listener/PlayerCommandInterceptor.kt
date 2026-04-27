package com.sushi.dLRSGASForMinecraft.listener

import com.sushi.dLRSGASForMinecraft.service.DLRSLoginService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

/**
 * 命令预处理监听器
 * 用于拦截未登录玩家的命令执行
 */
class PlayerCommandInterceptor(
    private val loginService: DLRSLoginService
) : Listener {

    // 允许未登录玩家使用的命令白名单
    private val whitelist = setOf(
        "gas",
        "gasl",
        "dlrs",
        "dlrsgas",
        "help",
        "h",
        "version",
        "about",
        "plugins",
        "pl"
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val message = event.message

        // 如果是控制台命令，不处理
        if (player !is org.bukkit.entity.Player) {
            return
        }

        // 检查玩家是否已登录
        if (loginService.isLoggedIn(player)) {
            return
        }

        // 提取命令名称（去掉 / 和参数）
        val command = message.removePrefix("/").split(" ")[0].lowercase()

        // 检查是否在白名单中
        if (whitelist.contains(command)) {
            return
        }

        // 阻止命令执行
        event.isCancelled = true

        // 发送提示信息
        player.sendMessage("§c[DLRS-GAS] §7您尚未登录，无法执行此命令")
        player.sendMessage("§e[DLRS-GAS] §7请使用 /gas login 进行登录")
    }
}
