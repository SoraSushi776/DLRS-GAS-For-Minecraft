package com.sushi.dLRSGASForMinecraft.command

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import com.sushi.dLRSGASForMinecraft.service.DLRSAutoLoginService
import com.sushi.dLRSGASForMinecraft.service.DLRSLoginService
import com.sushi.dLRSGASForMinecraft.service.DoublePasswordService
import com.sushi.dLRSGASForMinecraft.service.PlayerDataService
import com.sushi.dLRSGASForMinecraft.util.AESEncryptor
import com.sushi.dLRSGASForMinecraft.util.HttpUtil
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.json.JSONObject

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
            §7/gas ban <玩家|UID> [时长] [原因] - 封禁玩家账号 (需要 OP 权限)
            §7/gas reload     - 重载插件配置 (需要 OP 权限)
            §7/gas kickall    - 踢出所有玩家 (需要 OP 权限)
            §7/gas logoutall  - 登出所有已登录的 GAS 账号 (需要 OP 权限)
            §7/gas unbind <玩家名|UID> - 解绑指定玩家的 GAS 账号 (需要 OP 权限)
            §7/gas bind       - 查看当前服务器的 GAS 绑定状态 (需要 OP 权限)
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
            "ban" -> handleBan(player, args)
            "reload" -> handleReload(player)
            "kickall" -> handleKickAll(player)
            "logoutall" -> handleLogoutAll(player)
            "unbind" -> handleUnbind(player, args)
            "bind" -> handleBind(player)
            "help" -> showHelp(player)
            else -> {
                player.sendMessage("§c[DLRS-GAS] §7未知命令，使用 /gas help 查看帮助")
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

    /**
     * 处理兑换码命令
     * 调用 DLRS 兑换码 API：https://api.chinadlrs.com/developer/redeem.php
     * 兑换内容 data.content 使用 AES/AppToken 解密后解析处理
     */
    private fun handleRedeem(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas redeem <兑换码>")
            return
        }

        if (!loginService.isLoggedIn(player)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录，请先使用 /gas login 登录")
            return
        }

        val redeemCode = args[1]
        val userInfo = loginService.getPlayerInfo(player)

        if (userInfo == null) {
            player.sendMessage("§c[DLRS-GAS] §7无法获取您的账号信息，请重新登录")
            return
        }

        player.sendMessage("§e[DLRS-GAS] §7正在验证兑换码...")

        // 异步执行 HTTP 请求，不阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(DLRSGASForMinecraft.instance, Runnable {
            try {
                val configManager = DLRSGASForMinecraft.instance.getConfigManager()
                val appId = configManager.getAppId()
                val appToken = configManager.getAppToken()
                val language = configManager.getLanguage()

                // 构建请求 JSON：全局兑换码需提供 email + access_token
                val requestJson = HttpUtil.createJsonObject(
                    "appid" to appId,
                    "redeem_code" to redeemCode,
                    "email" to userInfo.email,
                    "access_token" to userInfo.accessToken
                )

                val url = "https://api.chinadlrs.com/developer/redeem.php?lang=$language"
                val response = HttpUtil.postJson(url, requestJson.toString())

                // 切回主线程处理响应
                Bukkit.getScheduler().runTask(DLRSGASForMinecraft.instance, Runnable {
                    if (response != null) {
                        val code = response.optInt("code", -1)
                        if (code == 200) {
                            val data = response.optJSONObject("data")
                            if (data != null) {
                                val encryptedContent = data.optString("content", "")
                                if (encryptedContent.isNotEmpty()) {
                                    try {
                                        // 使用 AppToken 解密兑换内容
                                        val decryptedContent = AESEncryptor.decrypt(encryptedContent, appToken)
                                        val contentJson = JSONObject(decryptedContent)

                                        val type = contentJson.optString("type", "")
                                        val value = contentJson.optInt("value", 0)

                                        when (type) {
                                            "eco" -> {
                                                // 执行 eco give <玩家名> <数量>
                                                player.sendMessage("§a[DLRS-GAS] §7兑换成功！获得 §f$value §7货币")
                                                Bukkit.dispatchCommand(
                                                    Bukkit.getConsoleSender(),
                                                    "eco give ${player.name} $value"
                                                )
                                            }
                                            else -> {
                                                player.sendMessage("§a[DLRS-GAS] §7兑换成功！")
                                                player.sendMessage("§7兑换内容：§f$decryptedContent")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        player.sendMessage("§c[DLRS-GAS] §7解析兑换内容失败：${e.message}")
                                    }
                                } else {
                                    player.sendMessage("§c[DLRS-GAS] §7兑换失败：兑换内容为空")
                                }
                            } else {
                                player.sendMessage("§c[DLRS-GAS] §7兑换失败：响应数据为空")
                            }
                        } else {
                            val msg = response.optString("msg", "未知错误")
                            player.sendMessage("§c[DLRS-GAS] §7兑换失败：$msg")
                        }
                    } else {
                        player.sendMessage("§c[DLRS-GAS] §7兑换失败：无法连接到服务器")
                    }
                })
            } catch (e: Exception) {
                Bukkit.getScheduler().runTask(DLRSGASForMinecraft.instance, Runnable {
                    player.sendMessage("§c[DLRS-GAS] §7兑换过程发生错误：${e.message}")
                })
                e.printStackTrace()
            }
        })
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

    // ==================== 管理员命令 ====================

    /**
     * 处理封禁命令（需要 OP 权限）
     */
    private fun handleBan(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        if (args.size < 2) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas ban <玩家|UID> [时长] [原因]")
            player.sendMessage("§e[DLRS-GAS] §7示例：/gas ban Steve 7d 恶意行为")
            player.sendMessage("§e[DLRS-GAS] §7示例：/gas ban 123456 永久 违规")
            return
        }

        val target = args[1]
        val duration = if (args.size >= 3) args[2] else "永久"
        val reason = if (args.size >= 4) args.drop(3).joinToString(" ") else "未指定原因"

        // 封禁功能需要调用 DLRS API 实现
        player.sendMessage("§e[DLRS-GAS] §7封禁功能需要配置 DLRS 开发者 API")
        player.sendMessage("§e[DLRS-GAS] §7目标：§f$target")
        player.sendMessage("§e[DLRS-GAS] §7时长：§f$duration")
        player.sendMessage("§e[DLRS-GAS] §7原因：§f$reason")
        player.sendMessage("§e[DLRS-GAS] §7请前往 DLRS 开发者后台配置封禁 API 后使用")
    }

    /**
     * 处理重载配置命令（需要 OP 权限）
     */
    private fun handleReload(player: Player) {
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        player.sendMessage("§e[DLRS-GAS] §7正在重载配置...")

        val success = DLRSGASForMinecraft.instance.reloadPluginConfig()
        if (success) {
            player.sendMessage("§a[DLRS-GAS] §7配置已成功重载！")
        } else {
            player.sendMessage("§c[DLRS-GAS] §7配置重载失败，请检查控制台日志")
        }
    }

    /**
     * 处理踢出所有玩家命令（需要 OP 权限）
     */
    private fun handleKickAll(player: Player) {
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        val onlinePlayers = Bukkit.getOnlinePlayers()
        val count = onlinePlayers.size

        if (count == 0) {
            player.sendMessage("§e[DLRS-GAS] §7当前没有在线玩家")
            return
        }

        player.sendMessage("§e[DLRS-GAS] §7正在踢出全部 $count 名玩家...")

        onlinePlayers.forEach { target ->
            if (target != player) { // 不踢自己
                target.kickPlayer("§c[DLRS-GAS] §7管理员已将所有玩家踢出服务器")
            }
        }

        player.sendMessage("§a[DLRS-GAS] §7已踢出 ${count - 1} 名玩家")
    }

    /**
     * 处理登出所有 GAS 账号命令（需要 OP 权限）
     */
    private fun handleLogoutAll(player: Player) {
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        val onlinePlayers = Bukkit.getOnlinePlayers()
        var logoutCount = 0

        onlinePlayers.forEach { target ->
            if (loginService.isLoggedIn(target)) {
                // 清除玩家数据但不踢出
                dataService.clearPlayerData(target.uniqueId)
                DLRSGASForMinecraft.lockServiceInstance.clearLock(target)
                DLRSGASForMinecraft.lockServiceInstance.resetPlayerDisplayName(target)
                if (target.isOp) {
                    target.isOp = false
                }
                target.sendMessage("§c[DLRS-GAS] §7管理员已强制登出您的 GAS 账号")
                // 重新锁定
                DLRSGASForMinecraft.lockServiceInstance.lockPlayer(target)
                logoutCount++
            }
        }

        player.sendMessage("§a[DLRS-GAS] §7已强制登出 $logoutCount 名玩家的 GAS 账号")
    }

    /**
     * 处理解绑命令（需要 OP 权限）
     */
    private fun handleUnbind(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        if (args.size < 2) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas unbind <玩家名|UID>")
            player.sendMessage("§e[DLRS-GAS] §7示例：/gas unbind Steve")
            player.sendMessage("§e[DLRS-GAS] §7示例：/gas unbind 123456")
            return
        }

        val target = args[1]

        // 先尝试当作玩家名搜索在线玩家
        val onlineTarget = Bukkit.getPlayerExact(target)
        if (onlineTarget != null) {
            val uid = dataService.getBoundUid(onlineTarget.uniqueId.toString())
            if (uid != null) {
                dataService.unbindAccountByPlayerUuid(onlineTarget.uniqueId.toString())
                dataService.clearPlayerData(onlineTarget.uniqueId)
                DLRSGASForMinecraft.lockServiceInstance.clearLock(onlineTarget)
                DLRSGASForMinecraft.lockServiceInstance.resetPlayerDisplayName(onlineTarget)
                if (onlineTarget.isOp) {
                    onlineTarget.isOp = false
                }
                onlineTarget.sendMessage("§c[DLRS-GAS] §7管理员已解绑您的 GAS 账号")
                player.sendMessage("§a[DLRS-GAS] §7已解绑玩家 §f${onlineTarget.name} §7的 GAS 账号 (UID: $uid)")
                return
            } else {
                player.sendMessage("§e[DLRS-GAS] §7玩家 §f${onlineTarget.name} §7没有绑定 GAS 账号")
                return
            }
        }

        // 尝试当作 UID 搜索绑定记录
        val boundPlayerName = dataService.getBoundPlayerName(target)
        if (boundPlayerName != null) {
            dataService.unbindAccountByUid(target)
            // 如果被解绑的玩家在线，清理其状态
            val offlineTarget = Bukkit.getPlayerExact(boundPlayerName)
            if (offlineTarget != null) {
                dataService.clearPlayerData(offlineTarget.uniqueId)
                DLRSGASForMinecraft.lockServiceInstance.clearLock(offlineTarget)
                DLRSGASForMinecraft.lockServiceInstance.resetPlayerDisplayName(offlineTarget)
                if (offlineTarget.isOp) {
                    offlineTarget.isOp = false
                }
                offlineTarget.sendMessage("§c[DLRS-GAS] §7管理员已解绑您的 GAS 账号")
            }
            player.sendMessage("§a[DLRS-GAS] §7已解绑 UID §f$target §7的绑定 (玩家: $boundPlayerName)")
        } else {
            player.sendMessage("§c[DLRS-GAS] §7未找到玩家或 UID §f$target §7的绑定记录")
        }
    }

    /**
     * 处理查看绑定状态命令（需要 OP 权限）
     */
    private fun handleBind(player: Player) {
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        val onlinePlayers = Bukkit.getOnlinePlayers()
        var boundCount = 0

        player.sendMessage("§e========== GAS 绑定状态 ==========")

        onlinePlayers.forEach { target ->
            val uid = dataService.getBoundUid(target.uniqueId.toString())
            if (uid != null) {
                val userInfo = dataService.getPlayerInfo(target.uniqueId)
                val nickname = userInfo?.nickname ?: "未知"
                player.sendMessage("§7${target.name} §f→ §a$nickname §7(UID: $uid)")
                boundCount++
            } else {
                player.sendMessage("§7${target.name} §f→ §c未绑定")
            }
        }

        if (onlinePlayers.isEmpty()) {
            player.sendMessage("§7当前没有在线玩家")
        }

        player.sendMessage("§e==================================")
        player.sendMessage("§7在线玩家：${onlinePlayers.size} 人，已绑定：$boundCount 人")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.isEmpty()) {
            return emptyList()
        }

        // 第一级参数
        if (args.size == 1) {
            val commands = mutableListOf(
                "login", "logout", "status", "info", "redeem",
                "double-password", "help"
            )
            // OP 玩家额外显示管理命令
            if (sender is Player && sender.isOp) {
                commands.addAll(listOf("ban", "reload", "kickall", "logoutall", "unbind", "bind"))
            }
            return commands.filter { it.startsWith(args[0].lowercase()) }
        }

        // 第二级参数
        when (args[0].lowercase()) {
            "double-password", "double" -> {
                if (args.size == 2) {
                    return mutableListOf("set", "verify", "change", "remove", "status", "help")
                        .filter { it.startsWith(args[1].lowercase()) }
                }
                return emptyList()
            }
            "ban" -> {
                if (args.size == 2) {
                    // 补全在线玩家名
                    return Bukkit.getOnlinePlayers().map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                if (args.size == 3) {
                    // 补全常见时长
                    return listOf("1d", "3d", "7d", "14d", "30d", "永久")
                        .filter { it.startsWith(args[2].lowercase()) }
                }
                return emptyList()
            }
            "unbind" -> {
                if (args.size == 2 && sender is Player && sender.isOp) {
                    // 补全在线玩家名
                    return Bukkit.getOnlinePlayers().map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                return emptyList()
            }
            "help", "reload", "kickall", "logoutall", "bind" -> return emptyList()
        }

        return emptyList()
    }
}