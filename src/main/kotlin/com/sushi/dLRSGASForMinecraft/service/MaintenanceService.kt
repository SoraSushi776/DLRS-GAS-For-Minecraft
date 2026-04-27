package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.config.DLRSConfig
import com.sushi.dLRSGASForMinecraft.util.AESEncryptor
import com.sushi.dLRSGASForMinecraft.util.HttpUtil
import org.bukkit.Bukkit
import org.json.JSONObject

/**
 * 服务器维护状态检查服务
 * 在玩家登录时检查 DLRS 服务器的维护状态
 */
class MaintenanceService(private val config: DLRSConfig) {

    companion object {
        private const val MAINTENANCE_API_URL = "https://api.chinadlrs.com/developer/maint.php"
    }

    // 是否启用维护检查
    private val isEnabled: Boolean = config.isMaintenanceCheckEnabled()

    /**
     * 检查维护状态（同步阻塞，用于玩家登录时）
     * @return Pair(是否维护中，维护消息)
     */
    fun checkMaintenanceStatusSync(): Pair<Boolean, String> {
        if (!isEnabled) {
            return Pair(false, "")
        }

        val appId = config.getAppId()
        val appToken = config.getAppToken()
        val language = config.getLanguage()

        try {
            // 直接同步执行 HTTP 请求
            val encryptedToken = AESEncryptor.encrypt(appToken, appToken)

            val appIdInt = appId.toIntOrNull() ?: 0
            val requestJson = JSONObject()
            requestJson.put("appid", appIdInt)
            requestJson.put("apptoken", encryptedToken)

            val url = "$MAINTENANCE_API_URL?lang=$language"
            val response = HttpUtil.postJson(url, requestJson.toString())

            if (response != null) {
                val code = response.getInt("code")

                if (code == 200) {
                    // 有维护事件 - 踢出玩家
                    val data = response.getJSONObject("data")

                    // 解密维护内容和结束时间
                    val encryptedContent = data.optString("content", "")
                    val encryptedEndTime = data.optString("end_time", "")

                    val decryptedMessage = try {
                        AESEncryptor.decrypt(encryptedContent, appToken)
                    } catch (e: Exception) {
                        encryptedContent
                    }

                    val decryptedEndTime = try {
                        AESEncryptor.decrypt(encryptedEndTime, appToken)
                    } catch (e: Exception) {
                        encryptedEndTime
                    }

                    // 构建完整的维护消息
                    val maintMsg = buildString {
                        appendLine("&c&l[DLRS-GAS] 服务器正在维护中！")
                        appendLine()
                        appendLine("&e维护信息：")
                        appendLine(decryptedMessage.ifEmpty { "&7暂无详细说明" })
                        appendLine()
                        appendLine("&e维护结束时间：")
                        appendLine(decryptedEndTime.ifEmpty { "&7暂未公布" })
                    }

                    return Pair(true, maintMsg)
                } else if (code == 201) {
                    // 无维护事件
                    return Pair(false, "")
                } else {
                    // 其他错误，假设不在维护
                    return Pair(false, "")
                }
            } else {
                // 请求失败，假设不在维护
                return Pair(false, "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(false, "")
        }
    }

    /**
     * 初始化服务（无需操作，因为不再需要定时任务）
     */
    fun initialize() {
        // 无需初始化
    }

    /**
     * 获取维护消息（用于插件初始化时检查）
     * @return 维护消息，如果不在维护则返回空字符串
     */
    fun getMaintenanceMessage(): String {
        if (!isEnabled) {
            return ""
        }
        val (isMaintaining, message) = checkMaintenanceStatusSync()
        return if (isMaintaining) message else ""
    }

    /**
     * 关闭服务（无需操作，因为没有定时任务）
     */
    fun shutdown() {
        // 无需清理
    }
}
