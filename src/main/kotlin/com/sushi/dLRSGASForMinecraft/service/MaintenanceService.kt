package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.config.DLRSConfig
import com.sushi.dLRSGASForMinecraft.util.AESEncryptor
import com.sushi.dLRSGASForMinecraft.util.HttpUtil
import org.bukkit.Bukkit
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 服务器维护状态检查服务
 * 在玩家登录时检查 DLRS 服务器的维护状态
 */
class MaintenanceService(private val config: DLRSConfig) {

    companion object {
        private const val MAINTENANCE_API_URL = "https://api.chinadlrs.com/developer/maint.php"
        private const val CHECK_TIMEOUT_SECONDS = 5L
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

        val result = AtomicBoolean(false)
        val message = AtomicReference<String>("")
        val latch = CountDownLatch(1)

        val appId = config.getAppId()
        val appToken = config.getAppToken()
        val language = config.getLanguage()

        // 异步执行 HTTP 请求
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("DLRS-GAS-For-Minecraft")!!,
            Runnable {
                try {
                    // 加密 appToken
                    val encryptedToken = AESEncryptor.encrypt(appToken, appToken)

                    val appIdInt = appId.toIntOrNull() ?: 0
                    val requestJson = JSONObject()
                    requestJson.put("appid", appIdInt)
                    requestJson.put("apptoken", encryptedToken)

                    val url = "$MAINTENANCE_API_URL?lang=$language"
                    val response = HttpUtil.postJson(url, requestJson.toString())

                    if (response != null) {
                        val code = response.getInt("code")

                        when (code) {
                            200 -> {
                                // 有维护事件
                                val data = response.getJSONObject("data")
                                result.set(true)

                                // 尝试解密维护内容
                                val encryptedContent = data.optString("content", "")
                                val decryptedMessage = try {
                                    AESEncryptor.decrypt(encryptedContent, appToken)
                                } catch (e: Exception) {
                                    encryptedContent
                                }

                                message.set(decryptedMessage.ifEmpty { config.getMaintenanceCustomMessage() })
                            }
                            201 -> {
                                // 无维护事件
                                result.set(false)
                                message.set("")
                            }
                            else -> {
                                // 其他错误，假设不在维护
                                result.set(false)
                                message.set(config.getMaintenanceCustomMessage())
                            }
                        }
                    } else {
                        // 请求失败，假设不在维护
                        result.set(false)
                        message.set("")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    result.set(false)
                    message.set("")
                } finally {
                    latch.countDown()
                }
            }
        )

        // 等待结果（最多 5 秒）
        try {
            latch.await(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        return Pair(result.get(), message.get())
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
