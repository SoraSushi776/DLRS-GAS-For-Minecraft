package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import com.sushi.dLRSGASForMinecraft.config.DLRSConfig
import com.sushi.dLRSGASForMinecraft.model.UserInfo
import com.sushi.dLRSGASForMinecraft.util.HttpUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * DLRS 自动登录服务
 * 使用 access_token 进行快速登录
 */
class DLRSAutoLoginService(
    private val config: DLRSConfig,
    private val dataService: PlayerDataService,
    private val doublePasswordService: DoublePasswordService
) {

    companion object {
        private const val AUTO_LOGIN_API_URL = "https://api.chinadlrs.com/developer/auto-login.php"
        private const val PROFILE_API_URL = "https://api.chinadlrs.com/developer/profile.php"
        private const val LOGOUT_API_URL = "https://api.chinadlrs.com/developer/oauth.php"
    }

    /**
     * 尝试使用存储的 access_token 自动登录
     *
     * @param player 玩家对象
     * @return 是否成功自动登录
     */
    fun tryAutoLogin(player: Player): Boolean {
        // 从数据库获取用户信息
        val userInfo = dataService.getPlayerInfo(player.uniqueId)

        if (userInfo == null || userInfo.accessToken.isEmpty()) {
            return false
        }

        player.sendMessage("§e[DLRS-GAS] §7正在尝试自动登录...")

        // 验证 access_token
        if (validateAccessToken(userInfo.email, userInfo.accessToken)) {
            // 获取最新用户信息
            val updatedUserInfo = fetchUserInfo(userInfo.email, userInfo.accessToken)
            if (updatedUserInfo != null) {
                // 检查并处理账号绑定
                val bindResult = dataService.checkAndBindAccount(
                    updatedUserInfo.uid,
                    player.uniqueId.toString(),
                    player.name
                )
                if (!bindResult.success) {
                    // 绑定冲突，在主线程中踢出玩家
                    Bukkit.getScheduler().scheduleSyncDelayedTask(
                        DLRSGASForMinecraft.instance,
                        {
                            player.kickPlayer(bindResult.message)
                        },
                        1L
                    )
                    return false
                }

                player.sendMessage("§a[DLRS-GAS] §7自动登录成功！")
                player.sendMessage("§a[DLRS-GAS] §7欢迎回来，§f${updatedUserInfo.nickname}§7!")

                // 在主线程中锁定玩家并设置权限（Bukkit API 必须在主线程调用）
                Bukkit.getScheduler().runTask(
                    DLRSGASForMinecraft.instance,
                    Runnable {
                        // 立即锁定玩家，等验证双重密码解锁（不发送锁定消息，因为已经自动登录成功）
                        DLRSGASForMinecraft.lockServiceInstance.lockPlayer(player, sendMessage = false)
                        // 为有权限的玩家设权限
                        DLRSGASForMinecraft.lockServiceInstance.setPlayerPermissions(player, updatedUserInfo)
                        DLRSGASForMinecraft.lockServiceInstance.setPlayerDisplayName(player, updatedUserInfo)

                        // 处理双重密码验证（在自动登录成功后，仍然需要验证双重密码才能解锁）
                        doublePasswordService.handleDoublePasswordVerification(player, updatedUserInfo)
                    }
                )

                // 发送加入消息
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("DLRS-GAS-For-Minecraft")!!,
                    Runnable {
                        Bukkit.broadcastMessage("§a[DLRS-GAS] §f${updatedUserInfo.nickname} §7(${player.name}) §7加入了游戏")
                        DLRSGASForMinecraft.loggedPlayers.put(player.uniqueId, player.name)
                    },
                    5L
                )

                return true
            } else {
                player.sendMessage("§c[DLRS-GAS] §7获取用户信息失败")
                return false
            }
        } else {
            player.sendMessage("§c[DLRS-GAS] §7自动登录失败，Token 已过期")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /gas login 重新登录")

            // 清除过期的 token
            dataService.clearPlayerData(player.uniqueId)
            return false
        }
    }

    /**
     * 验证 access_token 是否有效
     */
    private fun validateAccessToken(email: String, accessToken: String): Boolean {
        val appId = config.getAppId()
        val language = config.getLanguage()

        // 构建请求 JSON
        val requestJson = HttpUtil.createJsonObject(
            "appid" to appId,
            "email" to email,
            "access_token" to accessToken
        )

        // 发送请求
        val url = "$AUTO_LOGIN_API_URL?lang=$language"
        val response = HttpUtil.postJson(url, requestJson.toString())

        return response != null && response.getInt("code") == 200
    }

    /**
     * 获取用户信息
     */
    private fun fetchUserInfo(email: String, accessToken: String): UserInfo? {
        val appId = config.getAppId()
        val language = config.getLanguage()

        // 构建请求 JSON
        val requestJson = HttpUtil.createJsonObject(
            "appid" to appId,
            "email" to email,
            "access_token" to accessToken
        )

        // 发送请求
        val url = "$PROFILE_API_URL?lang=$language"
        val response = HttpUtil.postJson(url, requestJson.toString())

        if (response != null && response.getInt("code") == 200) {
            val data = response.getJSONObject("data")
            val userGroup = data.optString("user_group", "")

            return UserInfo(
                uid = data.opt("uid").toString(),
                nickname = data.getString("nickname"),
                email = email,
                accessToken = accessToken,
                avatarUrl = data.optString("avatar", ""),
                userGroup = userGroup,
                isInGroup2 = userGroup.split(",").contains("2")
            )
        }

        return null
    }

    /**
     * 清除玩家数据
     */
    private fun clearPlayerData(playerUuid: java.util.UUID) {
        dataService.clearPlayerData(playerUuid)
    }

    /**
     * 登出玩家
     */
    fun logout(player: Player): Boolean {
        // 从数据库获取用户信息，用于调用退出登录接口
        val userInfo = dataService.getPlayerInfo(player.uniqueId)

        // 调用 GAS 退出登录接口
        if (userInfo != null && userInfo.accessToken.isNotEmpty()) {
            logoutFromGAS(userInfo.email, userInfo.accessToken)
        }

        // 清除本地玩家数据
        clearPlayerData(player.uniqueId)

        // 清除锁定状态、重置显示名称和 OP 权限
        DLRSGASForMinecraft.lockServiceInstance.clearLock(player)
        DLRSGASForMinecraft.lockServiceInstance.resetPlayerDisplayName(player)
        if (player.isOp) {
            player.isOp = false
        }

        player.sendMessage("§a[DLRS-GAS] §7已成功登出")
        player.sendMessage("§e[DLRS-GAS] §7正在断开连接...")

        // 延迟 1 秒后断开连接，让玩家看到登出消息
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            DLRSGASForMinecraft.instance,
            {
                player.kickPlayer("§e[DLRS-GAS] §7您已登出，请重新登录")
            },
            20L // 1 秒 = 20 ticks
        )

        return true
    }

    /**
     * 调用 GAS 退出登录接口 (type=5)
     */
    private fun logoutFromGAS(email: String, accessToken: String) {
        val appId = config.getAppId()
        val language = config.getLanguage()

        // 构建请求 JSON
        val requestJson = HttpUtil.createJsonObject(
            "appid" to appId,
            "email" to email,
            "access_token" to accessToken
        )

        // 发送请求
        val url = "$LOGOUT_API_URL?type=5&lang=$language"
        val response = HttpUtil.postJson(url, requestJson.toString())

        if (response != null && response.getInt("code") == 200) {
            Bukkit.getLogger().info("[DLRS-GAS] 玩家 $email 已成功退出 GAS")
        } else {
            Bukkit.getLogger().warning("[DLRS-GAS] 退出 GAS 失败：${response?.getString("msg")}")
        }
    }
}