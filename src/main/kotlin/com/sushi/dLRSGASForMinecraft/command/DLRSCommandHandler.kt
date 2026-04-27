package com.sushi.dLRSGASForMinecraft.command

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import com.sushi.dLRSGASForMinecraft.service.DLRSAutoLoginService
import com.sushi.dLRSGASForMinecraft.service.DLRSLoginService
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
    private val dataService: PlayerDataService
) : CommandExecutor, TabCompleter {

    companion object {
        private const val COMMAND_USAGE = """
            §e========== DLRS-GAS 命令帮助 ==========
            §7/gas login      - 登录 DLRS 账号
            §7/gas logout     - 登出 DLRS 账号
            §7/gas status     - 查看登录状态
            §7/gas info       - 查看账号信息
            §7/gas redeem     - 兑换 DLRS 兑换码
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
                    "redeem" -> handleRedeem(player, args.copyOf())
                    "reload" -> handleReload(player)
                    "kickall" -> handleKickall(player)
                    "logoutall" -> handleLogoutall(player)
                    "unbind" -> handleUnbind(player, args.copyOf())
                    "bind" -> handleBind(player, args.copyOf())
                    else -> {
                        player.sendMessage("§c[DLRS-GAS] §7未知命令，请使用 /gas 查看帮助")
                    }
                }
            }
        }

        return true
    }

    /**
     * 检查玩家是否已登录，未登录则提示
     */
    private fun checkLoggedIn(player: Player, commandName: String): Boolean {
        if (!loginService.isLoggedIn(player)) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录，无法使用 $commandName 命令")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /gas login 进行登录")
            return false
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
            player.sendMessage("§e[DLRS-GAS] §7如需重新登录，请先使用 /gas logout")
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
            player.sendMessage("§a[DLRS-GAS] §7当前状态：§a 已登录")
        } else {
            player.sendMessage("§c[DLRS-GAS] §7当前状态：§c 未登录")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /gas login 进行登录")
        }
    }

    /**
     * 处理信息查询命令
     */
    private fun handleInfo(player: Player) {
        if (!checkLoggedIn(player, "info")) return

        val userInfo = loginService.getPlayerInfo(player)

        if (userInfo == null) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录，无法查看信息")
            return
        }

        player.sendMessage("§e========== DLRS-GAS 账号信息 ==========")
        player.sendMessage("§7用户 ID: §f${userInfo.uid}")
        player.sendMessage("§7昵称：§f${userInfo.nickname}")
        player.sendMessage("§7邮箱：§f${userInfo.email}")
        player.sendMessage("§7用户组：§f${userInfo.userGroup}")
        player.sendMessage("§7头像：§f${userInfo.avatarUrl}")
        player.sendMessage("§e======================================")
    }

    /**
     * 处理兑换命令
     */
    private fun handleRedeem(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§c[DLRS-GAS] §7用法：/gas redeem <兑换码>")
            player.sendMessage("§e[DLRS-GAS] §7例如：/gas redeem ABCD-EFGH-IJKL-MNOP")
            return
        }

        // 检查是否已登录
        if (!checkLoggedIn(player, "redeem")) return

        val redeemCode = args[1]
        player.sendMessage("§e[DLRS-GAS] §7正在兑换兑换码...")

        // 异步执行兑换
        Bukkit.getScheduler().runTaskAsynchronously(DLRSGASForMinecraft.instance, Runnable {
            // 调用兑换接口
            val success = performRedeem(player, redeemCode)
            Bukkit.getScheduler().runTask(DLRSGASForMinecraft.instance, Runnable {
                if (success) {
                    player.sendMessage("§a[DLRS-GAS] §7兑换码兑换成功！")
                } else {
                    player.sendMessage("§c[DLRS-GAS] §7兑换码兑换失败，请检查兑换码是否正确")
                }
            })
        })
    }

    private fun performRedeem(player: Player, redeemCode: String): Boolean {
        try {
            // 从配置文件中获取应用 ID 和 Token
            val config = DLRSGASForMinecraft.instance.getConfigManager()
            val appId = config.getAppId()
            val appToken = config.getAppToken()

            if (appId.isEmpty() || appToken.isEmpty()) {
                Bukkit.getLogger().warning("[DLRS-GAS] 兑换码功能需要在 config.yml 中配置 app-id 和 app-token")
                player.sendMessage("§c[DLRS-GAS] §7配置错误：请在 config.yml 中配置 app-id 和 app-token")
                return false
            }

            // 获取玩家的登录信息（用于全局兑换码）
            val userInfo = loginService.getPlayerInfo(player)
            val email = userInfo?.email ?: ""
            val accessToken = userInfo?.accessToken ?: ""

            // 构建兑换请求
            // 发送到 https://api.chinadlrs.com/developer/redeem.php
            // 参数：
            // - appid: 应用 ID
            // - email: 玩家邮箱（全局兑换码必填）
            // - access_token: 访问令牌（全局兑换码必填）
            // - redeem_code: 兑换码

            val requestJson = org.json.JSONObject()
            requestJson.put("appid", appId)
            requestJson.put("redeem_code", redeemCode)

            // 如果玩家已登录，添加 email 和 access_token（用于全局兑换码）
            if (userInfo != null && email.isNotEmpty() && accessToken.isNotEmpty()) {
                requestJson.put("email", email)
                requestJson.put("access_token", accessToken)
            }

            val url = "https://api.chinadlrs.com/developer/redeem.php"

            // 直接调用 HttpUtil.postJson 方法
            val response = com.sushi.dLRSGASForMinecraft.util.HttpUtil.postJson(url, requestJson.toString())

            // 检查返回结果
            if (response == null) {
                Bukkit.getLogger().warning("[DLRS-GAS] 兑换码请求失败：无响应")
                player.sendMessage("§c[DLRS-GAS] §7兑换请求失败：请检查网络连接")
                return false
            }

            // 安全获取响应码
            val code = try {
                response.getInt("code")
            } catch (e: org.json.JSONException) {
                Bukkit.getLogger().warning("[DLRS-GAS] 兑换码响应格式错误：$response")
                player.sendMessage("§c[DLRS-GAS] §7服务器响应格式错误")
                return false
            }

            if (code == 200) {
                // 兑换成功，获取奖励信息
                try {
                    val data = response.getJSONObject("data")

                    // 解密兑换内容（data.content 是加密的）
                    val encryptedContent = data.optString("content", "")

                    val decryptedContent = try {
                        com.sushi.dLRSGASForMinecraft.util.AESEncryptor.decrypt(encryptedContent, appToken)
                    } catch (e: Exception) {
                        encryptedContent
                    }

                    // 将兑换码内容输出到聊天栏
                    player.sendMessage("§e[DLRS-GAS] §7=== 兑换码内容 ===")
                    player.sendMessage("§e[DLRS-GAS] §7奖励内容：$decryptedContent")
                    player.sendMessage("§e[DLRS-GAS] §7================ ")

                    return true
                } catch (e: org.json.JSONException) {
                    Bukkit.getLogger().warning("[DLRS-GAS] 兑换码数据格式错误：${response.toString()}")
                    player.sendMessage("§c[DLRS-GAS] §7服务器响应格式错误")
                    return false
                }
            } else {
                // 兑换失败
                val msg = response.optString("msg", "兑换失败")
                Bukkit.getLogger().warning("[DLRS-GAS] 兑换失败：$msg")
                player.sendMessage("§c[DLRS-GAS] §7兑换失败：$msg")
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Bukkit.getLogger().warning("[DLRS-GAS] 兑换码功能出现异常：${e.message}")
            player.sendMessage("§c[DLRS-GAS] §7兑换过程中出现错误，请稍后重试")
            return false
        }
    }

    /**
     * 处理重载命令
     */
    private fun handleReload(player: Player) {
        // 检查 OP 权限
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        player.sendMessage("§e[DLRS-GAS] §7正在重载配置...")

        val success = DLRSGASForMinecraft.instance.reloadPluginConfig()

        if (success) {
            player.sendMessage("§a[DLRS-GAS] §7配置重载成功!")
        } else {
            player.sendMessage("§c[DLRS-GAS] §7配置重载失败，请查看控制台日志")
        }
    }

    /**
     * 处理踢出所有玩家命令
     */
    private fun handleKickall(player: Player) {
        // 检查 OP 权限
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) {
            player.sendMessage("§e[DLRS-GAS] §7当前没有在线玩家")
            return
        }

        val count = onlinePlayers.size
        player.sendMessage("§e[DLRS-GAS] §7正在踢出所有玩家 ($count 人)...")

        onlinePlayers.forEach { p ->
            if (p != player) {
                p.kickPlayer("§e[DLRS-GAS] §7OP 操作 - 所有玩家已被踢出")
            }
        }

        player.sendMessage("§a[DLRS-GAS] §7已踢出所有玩家!")
    }

    /**
     * 处理登出所有玩家命令
     */
    private fun handleLogoutall(player: Player) {
        // 检查 OP 权限
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        val loggedPlayers = Bukkit.getOnlinePlayers().filter { p ->
            loginService.isLoggedIn(p)
        }

        if (loggedPlayers.isEmpty()) {
            player.sendMessage("§e[DLRS-GAS] §7当前没有已登录的玩家")
            return
        }

        val count = loggedPlayers.size
        player.sendMessage("§e[DLRS-GAS] §7正在登出所有已登录的玩家 ($count 人)...")

        loggedPlayers.forEach { p ->
            autoLoginService.logout(p)
        }

        player.sendMessage("§a[DLRS-GAS] §7已登出所有玩家!")
    }

    /**
     * 处理解绑命令
     */
    private fun handleUnbind(player: Player, args: Array<out String>) {
        // 检查 OP 权限
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        // 如果有参数，解绑指定的玩家或 UID
        if (args.size > 1) {
            val target = args[1]
            // 尝试按 UID 解绑
            val unbindSuccess = dataService.unbindAccountByUid(target)
            if (unbindSuccess) {
                // 清除玩家数据（如果在线）
                val targetPlayer = Bukkit.getPlayer(target)
                if (targetPlayer != null) {
                    dataService.clearPlayerData(targetPlayer.uniqueId)
                    targetPlayer.kickPlayer("§e[DLRS-GAS] §7你的账号已被管理员解绑")
                }
                player.sendMessage("§a[DLRS-GAS] §7已成功解绑 UID: §f$target")
                return
            }
            // 尝试按玩家名解绑
            val targetPlayer = Bukkit.getPlayerExact(target)
            if (targetPlayer != null) {
                val userInfo = loginService.getPlayerInfo(targetPlayer)
                if (userInfo != null) {
                    val success = dataService.unbindAccountByUid(userInfo.uid)
                    if (success) {
                        dataService.clearPlayerData(targetPlayer.uniqueId)
                        targetPlayer.kickPlayer("§e[DLRS-GAS] §7你的账号已被管理员解绑")
                        player.sendMessage("§a[DLRS-GAS] §7已成功解绑玩家：§f${targetPlayer.name}")
                        player.sendMessage("§e[DLRS-GAS] §7解绑的 UID: §f${userInfo.uid}")
                    } else {
                        player.sendMessage("§c[DLRS-GAS] §7解绑失败，未找到绑定记录")
                    }
                } else {
                    player.sendMessage("§c[DLRS-GAS] §7该玩家尚未登录 GAS")
                }
                return
            }
            player.sendMessage("§c[DLRS-GAS] §7未找到指定的玩家或 UID")
            return
        }

        // 无参数，解绑自己
        if (!checkLoggedIn(player, "unbind")) return

        val userInfo = loginService.getPlayerInfo(player)
        if (userInfo == null) {
            player.sendMessage("§c[DLRS-GAS] §7您尚未登录，无法解绑")
            return
        }

        val uid = userInfo.uid
        val playerName = player.name

        // 执行解绑
        val unbindSuccess = dataService.unbindAccountByUid(uid)
        if (!unbindSuccess) {
            player.sendMessage("§c[DLRS-GAS] §7解绑失败，未找到绑定记录")
            return
        }

        // 清除玩家数据
        dataService.clearPlayerData(player.uniqueId)

        player.sendMessage("§a[DLRS-GAS] §7已成功解绑 GAS 账号!")
        player.sendMessage("§e[DLRS-GAS] §7你的账号 (§f$uid§7) 已与玩家 §f$playerName §7解绑")
        player.sendMessage("§e[DLRS-GAS] §7你将被踢出服务器，请重新登录后重新绑定")

        // 延迟踢出玩家
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            DLRSGASForMinecraft.instance,
            {
                player.kickPlayer("§e[DLRS-GAS] §7账号已解绑，请重新登录")
            },
            60L // 3 秒后踢出
        )
    }

    /**
     * 处理绑定查询命令
     */
    private fun handleBind(player: Player, args: Array<out String>) {
        // 检查 OP 权限
        if (!player.isOp) {
            player.sendMessage("§c[DLRS-GAS] §7你没有权限执行此命令")
            return
        }

        // 如果没有参数，显示自己的绑定状态
        if (args.size < 2) {
            if (!checkLoggedIn(player, "bind")) return

            val userInfo = loginService.getPlayerInfo(player)
            if (userInfo == null) {
                player.sendMessage("§c[DLRS-GAS] §7您尚未登录，无法查看绑定状态")
                return
            }
            val boundPlayerName = dataService.getBoundPlayerName(userInfo.uid)
            val boundPlayerUuid = dataService.getBoundPlayerUuid(userInfo.uid)
            player.sendMessage("§e========== 绑定信息 ==========")
            player.sendMessage("§7GAS UID: §f${userInfo.uid}")
            player.sendMessage("§7绑定状态：§a 已绑定")
            player.sendMessage("§7绑定玩家：§f$boundPlayerName")
            player.sendMessage("§7绑定玩家 UUID: §f$boundPlayerUuid")
            player.sendMessage("§e================================")
            return
        }

        // 有参数，查询指定的玩家或 UID
        val target = args[1]

        // 尝试按 UID 查询
        val boundPlayerName = dataService.getBoundPlayerName(target)
        val boundPlayerUuid = dataService.getBoundPlayerUuid(target)
        if (boundPlayerName != null) {
            player.sendMessage("§e========== 绑定信息 ==========")
            player.sendMessage("§7GAS UID: §f$target")
            player.sendMessage("§7绑定状态：§a 已绑定")
            player.sendMessage("§7绑定玩家：§f$boundPlayerName")
            player.sendMessage("§7绑定玩家 UUID: §f$boundPlayerUuid")
            player.sendMessage("§e================================")
            return
        }

        // 尝试按玩家名查询
        val targetPlayer = Bukkit.getPlayerExact(target)
        if (targetPlayer != null) {
            val userInfo = loginService.getPlayerInfo(targetPlayer)
            if (userInfo != null) {
                player.sendMessage("§e========== 绑定信息 ==========")
                player.sendMessage("§7玩家：§f${targetPlayer.name}")
                player.sendMessage("§7玩家 UUID: §f${targetPlayer.uniqueId}")
                player.sendMessage("§7GAS UID: §f${userInfo.uid}")
                player.sendMessage("§7GAS 昵称：§f${userInfo.nickname}")
                player.sendMessage("§e================================")
            } else {
                player.sendMessage("§c[DLRS-GAS] §7该玩家尚未登录 GAS")
            }
            return
        }

        // 尝试按玩家 UUID 查询绑定
        val boundUid = dataService.getBoundUid(target)
        if (boundUid != null) {
            player.sendMessage("§e========== 绑定信息 ==========")
            player.sendMessage("§7玩家 UUID: §f$target")
            player.sendMessage("§7绑定的 GAS UID: §f$boundUid")
            player.sendMessage("§e================================")
            return
        }

        player.sendMessage("§c[DLRS-GAS] §7未找到相关的绑定信息")
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
                listOf("login", "logout", "status", "info", "redeem", "reload", "kickall", "logoutall", "unbind", "bind").filter {
                    it.startsWith(args[0].lowercase())
                }
            }
            2 -> {
                // 参数补全（unbind 和 bind 命令）
                when (args[0].lowercase()) {
                    "unbind", "bind" -> {
                        // 补全在线玩家名
                        Bukkit.getOnlinePlayers().map { it.name }.filter {
                            it.startsWith(args[1], ignoreCase = true)
                        }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}