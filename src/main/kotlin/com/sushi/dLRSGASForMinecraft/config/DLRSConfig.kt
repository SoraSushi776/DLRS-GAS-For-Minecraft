package com.sushi.dLRSGASForMinecraft.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * 配置管理类
 * 管理DLRS-GAS的配置信息
 */
class DLRSConfig(private val plugin: JavaPlugin) {

    companion object {
        const val APP_ID_KEY = "dlrs.app-id"
        const val APP_TOKEN_KEY = "dlrs.app-token"
        const val LANGUAGE_KEY = "dlrs.language"

        // 登录超时配置
        const val LOGIN_TIMEOUT_ENABLED_KEY = "login-timeout.enabled"
        const val LOGIN_TIMEOUT_SECONDS_KEY = "login-timeout.timeout-seconds"

        // 默认配置值
        const val DEFAULT_LANGUAGE = "en"
        const val DEFAULT_TIMEOUT_ENABLED = true
        const val DEFAULT_TIMEOUT_SECONDS = 60
    }
    
    private val config: FileConfiguration = plugin.config
    
    /**
     * 获取应用ID
     */
    fun getAppId(): String {
        return config.getString(APP_ID_KEY, "55") ?: "55"
    }
    
    /**
     * 获取应用Token
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
