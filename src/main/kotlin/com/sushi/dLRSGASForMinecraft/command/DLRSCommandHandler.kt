package com.sushi.dLRSGASForMinecraft.command

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import com.sushi.dLRSGASForMinecraft.service.DLRSAutoLoginService
import com.sushi.dLRSGASForMinecraft.service.DLRSLoginService
import com.sushi.dLRSGASForMinecraft.service.DoublePasswordService
import com.sushi.dLRSGASForMinecraft.service.PlayerDataService
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * DLRS-GAS 命令处理器
 */
class DLRSCommandHandler(
    private val loginService: DLRSLoginService,
    private val autoLoginService: DLRSAutoLoginService,
    private val dataService: PlayerDataService,
    private val doublePasswordService: DoublePasswordService
) : CommandExecutor, TabCompleter {

    companion object {
        private const val COMMAND_USAGE = """
            §e========== DLRS-GAS 命令帮助 ==========
            §7/gas login      - 登录 DLRS 账号
            §7/gas logout     - 登出 DLRS 账号
            §7/gas status     - 查看登录状态
            §7/gas info       - 查看账号信息
            §7/gas redeem     - 兑换 DLRS 兑换码
            §7/gas double-password - 双重密码相关命令
            §7/gas ban <玩家/UID> [时长] [原因] - 封禁玩家账号 (需要 OP 权限)
            §7/gas reload     - 重载插件配置 (需要 OP 权限)
            §7/gas kickall    - 踢出所有玩家 (需要 OP 权限)
            §7/gas logoutall  - 登出所有已登录的 GAS 账号 (需要 OP 权限)
            §7/gas unbind     - 解绑 GAS 账号 (需要 OP 权限)
            §7/gas bind       - 查看绑定状态 (需要 OP 权限)
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

        // 处理 /login 简化命令
        if (args.isNotEmpty() && args[0].lowercase() == "login") {
            if (args.size >= 2) {
                handleSimplifiedLogin(player, args)
            } else {
                // 无密码参数，执行常规登录流程
                handleLogin(player)
            }
            return true
        }

        // 处理普通 /gas 命令
        if (args.isEmpty()) {
            player.sendMessage("§e[DLRS-GAS] §7使用 /gas help 查看命令帮助")
            return true
        }

        when (args[0].lowercase()) {
            "login" -> handleLogin(player)
            "logout" -> handleLogout(player)
            "status" -> handleStatus(player)
            "info" -> handleInfo(player)
            "redeem" -> handleRedeem(player, args)
            "double-password", "double" -> handleDoublePassword(player, args)
            "help" -> showHelp(player)
            else -> {
                player.sendMessage("§c[DLRS-GAS] §7未知命令，使用 /gas help 查看帮助")
                return false
            }
        }

        return true
    }

    private fun handleLogin(player: Player) {
        if (loginService.isLoggedIn(player)) {
            player.sendMessage("§c[DLRS-GAS] §7您已经登录了")
            return
        }

        loginService.startLogin(player)
    }

    /**
     * 处理简化登录命令 /login <密码>
     */
    private fun handleSimplifiedLogin(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/login <双重密码>")
            return
        }

        val password = args[1]

        // 检查是否已设置双重密码
        if (!doublePasswordService.hasDoublePassword(player.uniqueId)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未设置双重密码")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /gas double-password set <密码> 设置双重密码")
            return
        }

        // 验证双重密码
        if (doublePasswordService.verifyDoublePassword(player.uniqueId, password)) {
            player.sendMessage("§a[DLRS-GAS] §7双重密码验证成功！")
            player.sendMessage("§a[DLRS-GAS] §7您现在可以正常游戏了")
            // 解锁玩家（如果之前因为没有双重密码而锁定）
            DLRSGASForMinecraft.lockServiceInstance.unlockPlayer(player)
        } else {
            player.sendMessage("§c[DLRS-GAS] §7双重密码错误，请重试")
            player.sendMessage("§e[DLRS-GAS] §7如果忘记了双重密码，请联系管理员")
        }
    }

    private fun handleLogout(player: Player) {
        if (!loginService.isLoggedIn(player)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录")
            return
        }

        autoLoginService.logout(player)
    }

    private fun handleStatus(player: Player) {
        if (loginService.isLoggedIn(player)) {
            val userInfo = loginService.getPlayerInfo(player)
            if (userInfo != null) {
                player.sendMessage("§a[DLRS-GAS] §7您已登录")
                player.sendMessage("§a[DLRS-GAS] §7昵称：§f${userInfo.nickname}")
                player.sendMessage("§a[DLRS-GAS] §7邮箱：§f${userInfo.email}")
                player.sendMessage("§a[DLRS-GAS] §7UID：§f${userInfo.uid}")
            }
        } else {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录")
        }
    }

    private fun handleInfo(player: Player) {
        if (!loginService.isLoggedIn(player)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录")
            return
        }

        val userInfo = loginService.getPlayerInfo(player)
        if (userInfo != null) {
            player.sendMessage("§e[DLRS-GAS] §7账号详细信息：")
            player.sendMessage("§7昵称：§f${userInfo.nickname}")
            player.sendMessage("§7邮箱：§f${userInfo.email}")
            player.sendMessage("§7UID：§f${userInfo.uid}")
            player.sendMessage("§7用户组：§f${userInfo.userGroup}")
            player.sendMessage("§7头像：§f${userInfo.avatarUrl}")
        }
    }

    private fun handleRedeem(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas redeem <兑换码>")
            return
        }

        if (!loginService.isLoggedIn(player)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录，请先使用 /gas login 登录")
            return
        }

        // var redeemCode = args[1]
        // 从 DLRS 获取兑换码处理逻辑，这里省略（需要依赖 GST API）
        player.sendMessage("§e[DLRS-GAS] §7兑换码功能需要使用 API 实现")
        player.sendMessage("§e[DLRS-GAS] §7请前往 DLRS 开发者后台激活此功能")
    }

    /**
     * 处理双重密码命令
     */
    private fun handleDoublePassword(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas double-password <set|verify|change|remove|status>")
            player.sendMessage("§e[DLRS-GAS] §7示例：")
            player.sendMessage("§7/gas double-password set <密码> - 设置双重密码")
            player.sendMessage("§7/gas double-password verify <密码> - 验证双重密码")
            player.sendMessage("§7/gas double-password change <新密码> - 修改双重密码")
            player.sendMessage("§7/gas double-password remove - 移除双重密码")
            player.sendMessage("§7/gas double-password status - 查看双重密码状态")
            return
        }

        when (args[1].lowercase()) {
            "set" -> handleDoublePasswordSet(player, args)
            "verify" -> handleDoublePasswordVerify(player, args)
            "change" -> handleDoublePasswordChange(player, args)
            "remove" -> handleDoublePasswordRemove(player)
            "status" -> handleDoublePasswordStatus(player)
            else -> {
                player.sendMessage("§c[DLRS-GAS] §7未知子命令")
                player.sendMessage("§c[DLRS-GAS] §7使用 /gas double-password help 查看帮助")
            }
        }
    }

    /**
     * 处理设置双重密码
     */
    private fun handleDoublePasswordSet(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas double-password set <密码>")
            player.sendMessage(doublePasswordService.getDoublePasswordFormatRequirement())
            return
        }

        val password = args[2]

        // 验证密码格式
        if (!doublePasswordService.isValidDoublePasswordFormat(password)) {
            player.sendMessage("§c[DLRS-GAS] §7密码格式不正确")
            player.sendMessage(doublePasswordService.getDoublePasswordFormatRequirement())
            return
        }

        // 检查是否已经设置过双重密码
        val wasAlreadySet = doublePasswordService.hasDoublePassword(player.uniqueId)

        // 设置双重密码
        val (success, message) = doublePasswordService.setDoublePassword(player.uniqueId, password)
        player.sendMessage(message)

        if (success) {
            if (!wasAlreadySet) {
                // 第一次设置双重密码，立即解锁玩家（设置密码本身就是身份验证）
                player.sendMessage("§a[DLRS-GAS] §7双重密码设置成功！请记住您的密码")
                player.sendMessage("§a[DLRS-GAS] §7账号已解锁，祝您游戏愉快！")
                DLRSGASForMinecraft.lockServiceInstance.unlockPlayer(player)
            } else {
                // 修改密码，不需要解锁
                player.sendMessage("§a[DLRS-GAS] §7双重密码修改成功！请使用新密码验证")
            }
        }
    }

    /**
     * 处理验证双重密码
     */
    private fun handleDoublePasswordVerify(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas double-password verify <密码>")
            return
        }

        // 检查是否已设置双重密码
        if (!doublePasswordService.hasDoublePassword(player.uniqueId)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未设置双重密码")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /gas double-password set <密码> 设置双重密码")
            return
        }

        val password = args[2]

        // 验证双重密码
        if (doublePasswordService.verifyDoublePassword(player.uniqueId, password)) {
            player.sendMessage("§a[DLRS-GAS] §7双重密码验证成功！")
            player.sendMessage("§a[DLRS-GAS] §7您现在可以正常游戏了")
            // 解锁玩家（如果之前因为没有双重密码而锁定）
            DLRSGASForMinecraft.lockServiceInstance.unlockPlayer(player)
        } else {
            player.sendMessage("§c[DLRS-GAS] §7双重密码错误，请重试")
            player.sendMessage("§e[DLRS-GAS] §7如果忘记了双重密码，请联系管理员")
        }
    }

    /**
     * 处理修改双重密码
     */
    private fun handleDoublePasswordChange(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas double-password change <新密码>")
            player.sendMessage(doublePasswordService.getDoublePasswordFormatRequirement())
            return
        }

        // 检查是否已设置双重密码
        if (!doublePasswordService.hasDoublePassword(player.uniqueId)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未设置双重密码")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /gas double-password set <密码> 设置双重密码")
            return
        }

        val newPassword = args[2]

        // 验证新密码格式
        if (!doublePasswordService.isValidDoublePasswordFormat(newPassword)) {
            player.sendMessage("§c[DLRS-GAS] §7密码格式不正确")
            player.sendMessage(doublePasswordService.getDoublePasswordFormatRequirement())
            return
        }

        // 设置新密码
        val (success, message) = doublePasswordService.setDoublePassword(player.uniqueId, newPassword)
        player.sendMessage(message)

        if (success) {
            player.sendMessage("§a[DLRS-GAS] §7双重密码修改成功！请使用新密码验证")
        }
    }

    /**
     * 处理移除双重密码（仅 OP）
     */
    private fun handleDoublePasswordRemove(player: Player) {
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7您没有权限执行此命令")
            return
        }

        // 检查是否已设置双重密码
        if (!doublePasswordService.hasDoublePassword(player.uniqueId)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未设置双重密码")
            return
        }

        // 移除双重密码
        val success = doublePasswordService.deleteDoublePassword(player.uniqueId)

        if (success) {
            player.sendMessage("§a[DLRS-GAS] §7双重密码已移除")
        } else {
            player.sendMessage("§c[DLRS-GAS] §7移除双重密码失败")
        }
    }

    /**
     * 处理查看双重密码状态
     */
    private fun handleDoublePasswordStatus(player: Player) {
        if (doublePasswordService.hasDoublePassword(player.uniqueId)) {
            player.sendMessage("§a[DLRS-GAS] §7您已设置双重密码")
        } else {
            player.sendMessage("§c[DLRS-GAS] §7您尚未设置双重密码")
        }
    }

    /**
     * 显示帮助信息
     */
    private fun showHelp(player: Player) {
        player.sendMessage(COMMAND_USAGE)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        // 玩家未登录时的命令提示
        if (args.isEmpty()) {
            return mutableListOf("login", "logout", "status", "info", "redeem", "double-password", "help")
        }

        when (args[0].lowercase()) {
            "double-password", "double" -> {
                if (args.size == 2) {
                    return mutableListOf("set", "verify", "change", "remove", "status", "help")
                }
                return emptyList()
            }
            "help" -> return emptyList()
        }

        return emptyList()
    }
}