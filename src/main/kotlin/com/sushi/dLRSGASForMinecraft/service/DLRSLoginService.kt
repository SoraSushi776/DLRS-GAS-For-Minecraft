package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import com.sushi.dLRSGASForMinecraft.config.DLRSConfig
import com.sushi.dLRSGASForMinecraft.model.UserInfo
import com.sushi.dLRSGASForMinecraft.util.AESEncryptor
import com.sushi.dLRSGASForMinecraft.util.HttpUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * DLRS OAuth登录服务
 * 实现完整的OAuth登录流程
 */
class DLRSLoginService(private val config: DLRSConfig, private val dataService: PlayerDataService) {
    
    companion object {
        private const val OAUTH_API_URL = "https://api.chinadlrs.com/developer/oauth.php"
        private const val PROFILE_API_URL = "https://api.chinadlrs.com/developer/profile.php"
        private const val OAUTH_PAGE_URL = "https://gas.chinadlrs.com/oauth"
        
        // 存储正在进行的登录会话 <玩家UUID, 登录会话>
        private val loginSessions = ConcurrentHashMap<UUID, LoginSession>()
    }
    
    /**
     * 登录会话数据
     */
    data class LoginSession(
        val authToken: String,
        val player: Player,
        var pollingTaskId: Int? = null
    )
    
    /**
     * 开始OAuth登录流程
     * 
     * @param player 玩家对象
     * @return 是否成功启动登录流程
     */
    fun startLogin(player: Player): Boolean {
        try {
            player.sendMessage("§e[DLRS-GAS] §7正在获取授权令牌...")
            
            // 步骤1: 获取OAuth Token
            val oauthToken = getOAuthToken()
            if (oauthToken == null) {
                player.sendMessage("§c[DLRS-GAS] §7获取授权令牌失败，请检查网络连接")
                return false
            }
            
            player.sendMessage("§e[DLRS-GAS] §7授权令牌获取成功！")
            player.sendMessage("§e[DLRS-GAS] §7请在浏览器中打开以下链接完成登录：")
            player.sendMessage("§b${OAUTH_PAGE_URL}?appid=${config.getAppId()}&token=$oauthToken")
            player.sendMessage("§e[DLRS-GAS] §7登录后系统将自动验证...")
            
            // 创建登录会话
            val session = LoginSession(oauthToken, player)
            loginSessions[player.uniqueId] = session
            
            // 步骤3: 开始轮询验证登录状态
            startPolling(session)
            
            return true
            
        } catch (e: Exception) {
            e.printStackTrace()
            player.sendMessage("§c[DLRS-GAS] §7登录过程发生错误: ${e.message}")
            return false
        }
    }
    
    /**
     * 获取OAuth Token
     */
    private fun getOAuthToken(): String? {
        val appId = config.getAppId()
        val appToken = config.getAppToken()
        val language = config.getLanguage()
        
        // 加密appToken
        val encryptedToken = AESEncryptor.encrypt(appToken, appToken)
        
        // 构建请求JSON
        val requestJson = HttpUtil.createJsonObject(
            "appid" to appId,
            "apptoken" to encryptedToken
        )
        
        // 发送请求
        val url = "$OAUTH_API_URL?type=1&lang=$language"
        val response = HttpUtil.postJson(url, requestJson.toString())
        
        if (response != null && response.getInt("code") == 200) {
            val data = response.getJSONObject("data")
            return data.getString("auth_token")
        }
        
        return null
    }
    
    /**
     * 开始轮询验证登录状态
     */
    private fun startPolling(session: LoginSession) {
        val appId = config.getAppId()
        val language = config.getLanguage()
        val player = session.player
        
        // 使用Bukkit调度器进行轮询
        val taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            Bukkit.getPluginManager().getPlugin("DLRS-GAS-For-Minecraft")!!,
            {
                checkLoginStatus(session)
            },
            0L,
            100L // 5秒 = 100 ticks
        )
        
        session.pollingTaskId = taskId
        
        // 设置超时任务（5分钟后取消）
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            Bukkit.getPluginManager().getPlugin("DLRS-GAS-For-Minecraft")!!,
            {
                cancelPolling(session)
                if (loginSessions.containsKey(player.uniqueId)) {
                    player.sendMessage("§c[DLRS-GAS] §7登录超时，请重新尝试")
                    loginSessions.remove(player.uniqueId)
                }
            },
            6000L // 5分钟 = 6000 ticks
        )
    }
    
    /**
     * 检查登录状态
     */
    private fun checkLoginStatus(session: LoginSession) {
        val appId = config.getAppId()
        val language = config.getLanguage()
        
        // 构建请求JSON
        val requestJson = HttpUtil.createJsonObject(
            "appid" to appId,
            "auth_token" to session.authToken
        )
        
        // 发送请求
        val url = "$OAUTH_API_URL?type=4&lang=$language"
        val response = HttpUtil.postJson(url, requestJson.toString())
        
        if (response != null && response.getInt("code") == 200) {
            // 登录成功
            val data = response.getJSONObject("data")
            val email = data.getString("email")
            val accessToken = data.getString("access_token")
            
            // 取消轮询
            cancelPolling(session)
            
            // 获取用户信息
            val userInfo = fetchUserInfo(email, accessToken)
            if (userInfo != null) {
                session.player.sendMessage("§a[DLRS-GAS] §7登录成功！")
                session.player.sendMessage("§a[DLRS-GAS] §7欢迎, §f${userInfo.nickname}§7!")
                session.player.sendMessage("§a[DLRS-GAS] §7邮箱: §f${userInfo.email}")
                session.player.sendMessage("§a[DLRS-GAS] §7用户ID: §f${userInfo.uid}")
                
                // 保存用户信息到数据库
                dataService.saveUserInfo(session.player.uniqueId, userInfo)

                // 解锁玩家并设置权限
                DLRSGASForMinecraft.lockServiceInstance.unlockPlayer(session.player)
                DLRSGASForMinecraft.lockServiceInstance.setPlayerPermissions(session.player, userInfo)
                DLRSGASForMinecraft.lockServiceInstance.setPlayerDisplayName(session.player, userInfo)

                // 发送加入消息
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("DLRS-GAS-For-Minecraft")!!,
                    Runnable {
                        Bukkit.broadcastMessage("§a[DLRS-GAS] §f${userInfo.nickname} §7(${session.player.name}) §7加入了游戏")
                        DLRSGASForMinecraft.loggedPlayers.put(session.player.uniqueId, session.player.name)
                    },
                    5L
                )

                // 移除会话
                loginSessions.remove(session.player.uniqueId)
            } else {
                session.player.sendMessage("§c[DLRS-GAS] §7获取用户信息失败")
            }
        }
    }
    
    /**
     * 获取用户信息
     */
    private fun fetchUserInfo(email: String, accessToken: String): UserInfo? {
        val appId = config.getAppId()
        val language = config.getLanguage()
        
        // 构建请求JSON
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
     * 取消轮询
     */
    private fun cancelPolling(session: LoginSession) {
        session.pollingTaskId?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
        }
    }
    
    /**
     * 获取玩家的登录状态
     */
    fun isLoggedIn(player: Player): Boolean {
        return dataService.isLoggedIn(player.uniqueId)
    }

    /**
     * 获取玩家的用户信息
     */
    fun getPlayerInfo(player: Player): UserInfo? {
        return dataService.getPlayerInfo(player.uniqueId)
    }
}
