package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Tab列表管理服务
 * 负责管理玩家Tab列表的header和footer显示
 */
class TabListService(private val plugin: DLRSGASForMinecraft) {

    private val dataService: PlayerDataService by lazy {
        // 通过反射或其他方式获取dataService实例
        // 由于dataService是private，我们需要通过plugin的公共方法访问
        // 这里我们使用一个更好的方式：让plugin提供getter方法
        plugin.getDataService()
    }
    private var headerTemplate: String = ""
    private var footerTemplate: String = ""
    private var enabled: Boolean = true

    /**
     * 初始化Tab列表服务
     */
    fun initialize() {
        // 从配置文件中读取Tab列表设置
        val config = plugin.config
        
        enabled = config.getBoolean("tab-list.enabled", true)
        headerTemplate = config.getString("tab-list.header", "&e&lDLRS-GAS 服务器\n&7欢迎回来，%player%") ?: ""
        footerTemplate = config.getString("tab-list.footer", "&7GAS昵称: %gas_nickname%\n&7延迟: %ping%ms | TPS: %tps%") ?: ""
        
        if (enabled) {
            plugin.logger.info("§a[DLRS-GAS] Tab列表自定义功能已启用")
        } else {
            plugin.logger.info("§7[DLRS-GAS] Tab列表自定义功能已禁用")
        }
    }

    /**
     * 更新玩家的Tab列表显示
     */
    fun updatePlayerTabList(player: Player) {
        if (!enabled) return
        
        // 替换占位符
        val header = replacePlaceholders(headerTemplate, player)
        val footer = replacePlaceholders(footerTemplate, player)
        
        // 设置玩家的Tab列表header和footer
        player.setPlayerListHeaderFooter(header, footer)
    }

    /**
     * 更新所有在线玩家的Tab列表显示
     */
    fun updateAllPlayersTabList() {
        if (!enabled) return
        
        Bukkit.getOnlinePlayers().forEach { player ->
            updatePlayerTabList(player)
        }
    }

    /**
     * 替换模板中的占位符
     */
    private fun replacePlaceholders(template: String, player: Player): String {
        var result = template
        
        // 替换玩家相关占位符
        result = result.replace("%player%", player.name)
        result = result.replace("%player_uuid%", player.uniqueId.toString())
        
        // 替换延迟占位符
        result = result.replace("%ping%", player.ping.toString())
        
        // 替换TPS占位符（获取服务器TPS）
        val tps = getServerTPS()
        result = result.replace("%tps%", String.format("%.2f", tps))
        
        // 替换GAS用户昵称占位符
        val userInfo = dataService.getPlayerInfo(player.uniqueId)
        val gasNickname = userInfo?.nickname ?: "未登录"
        result = result.replace("%gas_nickname%", gasNickname)
        
        // 转换颜色代码
        result = convertColorCodes(result)
        
        return result
    }

    /**
     * 获取服务器TPS
     */
    private fun getServerTPS(): Double {
        return try {
            val tpsField = Bukkit.getServer().javaClass.getDeclaredField("recentTps")
            tpsField.isAccessible = true
            val recentTps = tpsField.get(Bukkit.getServer()) as DoubleArray
            recentTps[0] // 返回1分钟平均TPS
        } catch (e: Exception) {
            20.0 // 如果无法获取，返回默认值20.0
        }
    }

    /**
     * 转换颜色代码
     */
    private fun convertColorCodes(text: String): String {
        // 将&颜色代码转换为§颜色代码
        return text.replace('&', '§')
    }

    /**
     * 重新加载配置
     */
    fun reload() {
        initialize()
        updateAllPlayersTabList()
    }
}
