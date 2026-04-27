package com.sushi.dLRSGASForMinecraft.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * 配置管理类
 * 管理 DLRS-GAS 的配置信息
 */
class DLRSConfig(private val plugin: JavaPlugin) {

    companion object {
        const val APP_ID_KEY = "dlrs.app-id"
        const val APP_TOKEN_KEY = "dlrs.app-token"
        const val LANGUAGE_KEY = "dlrs.language"

        // 登录超时配置
        const val LOGIN_TIMEOUT_ENABLED_KEY = "login-timeout.enabled"
        const val LOGIN_TIMEOUT_SECONDS_KEY = "login-timeout.timeout-seconds"

        // 维护状态检查配置
        const val MAINTENANCE_ENABLED_KEY = "maintenance.enabled"
        const val MAINTENANCE_CUSTOM_MESSAGE_KEY = "maintenance.custom-message"

        // 默认配置值
        const val DEFAULT_LANGUAGE = "en"
        const val DEFAULT_TIMEOUT_ENABLED = true
        const val DEFAULT_TIMEOUT_SECONDS = 60
        const val DEFAULT_MAINTENANCE_ENABLED = false
    }

    private val config: FileConfiguration = plugin.config

    /**
     * 获取应用 ID
     */
    fun getAppId(): String {
        return config.getString(APP_ID_KEY, "55") ?: "55"
    }

    /**
     * 获取应用 Token
     */
    fun getAppToken(): String {
        return config.getString(APP_TOKEN_KEY, "XeUIxoREtW9w1Kmy") ?: "XeUIxoREtW9w1Kmy"
    }

    /**
     * 获取语言设置
     */
    fun getLanguage(): String {
        return config.getString(LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * 获取登录超时是否启用
     */
    fun isLoginTimeoutEnabled(): Boolean {
        return config.getBoolean(LOGIN_TIMEOUT_ENABLED_KEY, DEFAULT_TIMEOUT_ENABLED)
    }

    /**
     * 获取登录超时时间（秒）
     */
    fun getLoginTimeoutSeconds(): Int {
        return config.getInt(LOGIN_TIMEOUT_SECONDS_KEY, DEFAULT_TIMEOUT_SECONDS)
    }

    /**
     * 获取维护状态检查是否启用
     */
    fun isMaintenanceCheckEnabled(): Boolean {
        return config.getBoolean(MAINTENANCE_ENABLED_KEY, DEFAULT_MAINTENANCE_ENABLED)
    }

    /**
     * 获取自定义维护消息
     */
    fun getMaintenanceCustomMessage(): String {
        return config.getString(MAINTENANCE_CUSTOM_MESSAGE_KEY, "&c[DLRS-GAS] 服务器正在维护中\n&e 请稍后再试！") ?: "&c[DLRS-GAS] 服务器正在维护中\n&e 请稍后再试！"
    }

    /**
     * 保存配置
     */
    fun saveConfig() {
        plugin.saveConfig()
    }

    /**
     * 重新加载配置
     */
    fun reloadConfig() {
        plugin.reloadConfig()
    }
}
