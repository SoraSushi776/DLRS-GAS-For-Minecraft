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
class DLRSAutoLoginService(private val config: DLRSConfig) {

    companion object {
        private const val AUTO_LOGIN_API_URL = "https://api.chinadlrs.com/developer/auto-login.php"
        private const val PROFILE_API_URL = "https://api.chinadlrs.com/developer/profile.php"
    }

    /**
     * 尝试使用存储的 access_token 自动登录
     *
     * @param player 玩家对象
     * @return 是否成功自动登录
     */
    fun tryAutoLogin(player: Player): Boolean {
        val plugin = Bukkit.getPluginManager().getPlugin("DLRS-GAS-For-Minecraft")!!
        val pluginConfig = plugin.config
        val path = "players.${player.uniqueId.toString()}"

        // 检查是否有存储的 access_token
        val accessToken = pluginConfig.getString("$path.access_token")
        val email = pluginConfig.getString("$path.email")

        if (accessToken.isNullOrEmpty() || email.isNullOrEmpty()) {
            return false
        }

        player.sendMessage("§e[DLRS-GAS] §7正在尝试自动登录...")

        // 验证 access_token
        if (validateAccessToken(email, accessToken)) {
            // 获取用户信息
            val userInfo = fetchUserInfo(email, accessToken)
            if (userInfo != null) {
                player.sendMessage("§a[DLRS-GAS] §7自动登录成功！")
                player.sendMessage("§a[DLRS-GAS] §7欢迎回来，§f${userInfo.nickname}§7!")

                // 解锁玩家并设置权限
                DLRSGASForMinecraft.lockServiceInstance.unlockPlayer(player)
                DLRSGASForMinecraft.lockServiceInstance.setPlayerPermissions(player, userInfo)
                DLRSGASForMinecraft.lockServiceInstance.setPlayerDisplayName(player, userInfo)

                return true
            } else {
                player.sendMessage("§c[DLRS-GAS] §7获取用户信息失败")
                return false
            }
        } else {
            player.sendMessage("§c[DLRS-GAS] §7自动登录失败，Token 已过期")
            player.sendMessage("§e[DLRS-GAS] §7请使用 /dlrs login 重新登录")

            // 清除过期的 token
            clearPlayerData(player.uniqueId)
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
        val plugin = Bukkit.getPluginManager().getPlugin("DLRS-GAS-For-Minecraft")!!
        val config = plugin.config

        config.set("players.${playerUuid.toString()}", null)
        plugin.saveConfig()
    }

    /**
     * 登出玩家
     */
    fun logout(player: Player): Boolean {
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
}
