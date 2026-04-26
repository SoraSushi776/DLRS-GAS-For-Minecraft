package com.sushi.dLRSGASForMinecraft.command

import com.sushi.dLRSGASForMinecraft.service.DLRSAutoLoginService
import com.sushi.dLRSGASForMinecraft.service.DLRSLoginService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * DLRS-GAS命令处理器
 */
class DLRSCommandHandler(
    private val loginService: DLRSLoginService,
    private val autoLoginService: DLRSAutoLoginService
) : CommandExecutor, TabCompleter {
    
    companion object {
        private const val COMMAND_USAGE = """
            §e========== DLRS-GAS 命令帮助 ==========
            §7/dlrs login     - 登录DLRS账号
            §7/dlrs logout    - 登出DLRS账号
            §7/dlrs status    - 查看登录状态
            §7/dlrs info      - 查看账号信息
            §e======================================
        """
    }
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        
        // 检查是否为玩家
        if (sender !is Player) {
            sender.sendMessage("§c[DLRS-GAS] §7此命令只能由玩家执行")
            return true
        }
        
        val player = sender
        
        // 处理子命令
        when {
            args.isEmpty() -> {
                player.sendMessage(COMMAND_USAGE.trimIndent())
            }
            else -> {
                when (args[0].lowercase()) {
                    "login" -> handleLogin(player)
                    "logout" -> handleLogout(player)
                    "status" -> handleStatus(player)
                    "info" -> handleInfo(player)
                    else -> {
                        player.sendMessage("§c[DLRS-GAS] §7未知命令，请使用 /dlrs 查看帮助")
                    }
                }
            }
        }
        
        return true
    }
    
    /**
     * 处理登录命令
     */
    private fun handleLogin(player: Player) {
        // 检查是否已登录
        if (loginService.isLoggedIn(player)) {
            player.sendMessage("§e[DLRS-GAS] §7您已经登录过了")
            player.sendMessage("§e[DLRS-GAS] §7如需重新登录，请先使用 /dlrs logout")
            return
        }
        
        // 开始登录流程
        loginService.startLogin(player)
    }
    
    /**
     * 处理登出命令
     */
    private fun handleLogout(player: Player) {
        if (!loginService.isLoggedIn(player)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录")
            return
        }
        
        autoLoginService.logout(player)
    }
    
    /**
     * 处理状态查询命令
     */
    private fun handleStatus(player: Player) {
        if (loginService.isLoggedIn(player)) {
            player.sendMessage("§a[DLRS-GAS] §7当前状态: §a已登录")
        } else {
            player.sendMessage("§c[DLRS-GAS] §7当前状态: §c未登录")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /dlrs login 进行登录")
        }
    }
    
    /**
     * 处理信息查询命令
     */
    private fun handleInfo(player: Player) {
        val userInfo = loginService.getPlayerInfo(player)
        
        if (userInfo == null) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录，无法查看信息")
            return
        }
        
        player.sendMessage("§e========== DLRS-GAS 账号信息 ==========")
        player.sendMessage("§7用户ID: §f${userInfo.uid}")
        player.sendMessage("§7昵称: §f${userInfo.nickname}")
        player.sendMessage("§7邮箱: §f${userInfo.email}")
        player.sendMessage("§7用户组: §f${userInfo.userGroup}")
        player.sendMessage("§7头像: §f${userInfo.avatarUrl}")
        player.sendMessage("§e======================================")
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        
        if (sender !is Player) {
            return emptyList()
        }
        
        return when (args.size) {
            1 -> {
                // 子命令补全
                listOf("login", "logout", "status", "info").filter {
                    it.startsWith(args[0].lowercase())
                }
            }
            else -> emptyList()
        }
    }
}
