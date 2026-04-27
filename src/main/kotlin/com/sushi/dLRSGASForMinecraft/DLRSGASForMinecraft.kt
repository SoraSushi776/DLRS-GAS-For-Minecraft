package com.sushi.dLRSGASForMinecraft

import com.sushi.dLRSGASForMinecraft.command.DLRSCommandHandler
import com.sushi.dLRSGASForMinecraft.config.DLRSConfig
import com.sushi.dLRSGASForMinecraft.listener.PlayerCommandInterceptor
import com.sushi.dLRSGASForMinecraft.listener.PlayerLockListener
import com.sushi.dLRSGASForMinecraft.service.DLRSAutoLoginService
import com.sushi.dLRSGASForMinecraft.service.DLRSLoginService
import com.sushi.dLRSGASForMinecraft.service.MaintenanceService
import com.sushi.dLRSGASForMinecraft.service.PlayerDataService
import com.sushi.dLRSGASForMinecraft.service.PlayerLockService
import com.sushi.dLRSGASForMinecraft.service.TabListService
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DLRSGASForMinecraft : JavaPlugin(), Listener {

    private lateinit var config: DLRSConfig
    private lateinit var loginService: DLRSLoginService
    private lateinit var autoLoginService: DLRSAutoLoginService
    private lateinit var lockService: PlayerLockService
    private lateinit var dataService: PlayerDataService
    private lateinit var commandHandler: DLRSCommandHandler
    private lateinit var lockListener: PlayerLockListener
    private lateinit var tabListService: TabListService
    private lateinit var maintenanceService: MaintenanceService
    private lateinit var commandInterceptor: PlayerCommandInterceptor

    companion object {
        lateinit var instance: DLRSGASForMinecraft
            private set
        lateinit var lockServiceInstance: PlayerLockService
            private set
        // 存储已登录玩家（用于控制加入消息显示）
        val loggedPlayers = ConcurrentHashMap<UUID, String>()
    }

    override fun onEnable() {
        instance = this

        // 保存默认配置
        saveDefaultConfig()

        // 初始化配置管理器
        config = DLRSConfig(this)

        // 初始化数据库服务
        dataService = PlayerDataService(dataFolder)
        dataService.initialize()

        // 初始化服务
        loginService = DLRSLoginService(config, dataService)
        autoLoginService = DLRSAutoLoginService(config, dataService)
        lockService = PlayerLockService()

        // 保存静态引用
        lockServiceInstance = lockService

        // 初始化命令处理器
        commandHandler = DLRSCommandHandler(loginService, autoLoginService, dataService)

        // 初始化锁定监听器
        lockListener = PlayerLockListener(lockService)

        // 初始化 Tab 列表服务
        tabListService = TabListService(this)
        tabListService.initialize()

        // 初始化维护状态检查服务
        maintenanceService = MaintenanceService(config)
        maintenanceService.initialize()

        // 初始化命令拦截器（阻止未登录玩家执行其他插件命令）
        commandInterceptor = PlayerCommandInterceptor(loginService)

        // 获取维护状态（用于初始化时检查）
        val maintenanceMsg = maintenanceService.getMaintenanceMessage()

        // 启动定时任务，每 5 秒更新一次所有玩家的 Tab 列表
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            tabListService.updateAllPlayersTabList()
        }, 100L, 100L) // 20 ticks = 1 second, 100 ticks = 5 seconds

        // 注册命令（使用 registerCommand 方法）
        registerGasCommand()

        // 注册事件监听器
        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(this, this)
        pluginManager.registerEvents(lockListener, this)
        pluginManager.registerEvents(commandInterceptor, this)

        // 输出启动信息
        logger.info("§a========================================")
        logger.info("§a  DLRS-GAS For Minecraft 插件已启用!")
        logger.info("§a  版本：${description.version}")
        logger.info("§a  作者：${description.authors}")
        logger.info("§a========================================")
    }

    /**
     * 注册 GAS 命令（使用 registerCommand 方法）
     */
    private fun registerGasCommand() {
        // 创建一个包装类，同时支持 execute 和 suggest (tabComplete)
        val gasCommand = GasCommand()

        // 使用 registerCommand 注册命令
        this.registerCommand(
            "gas",
            "DLRS-GAS 账号系统主命令",
            listOf("gasl", "dlrs", "dlrsgas"),
            gasCommand
        )
    }

    /**
     * GAS 命令实现类
     */
    private inner class GasCommand : BasicCommand {

        private val bukkitCommand = object : org.bukkit.command.Command("gas", "", "/gas", listOf("gasl", "dlrs", "dlrsgas")) {
            override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
                return commandHandler.onCommand(sender, this, label, args)
            }

            override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
                return commandHandler.onTabComplete(sender, this, alias, args) ?: emptyList()
            }
        }

        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val sender = source.sender
            bukkitCommand.execute(sender, "gas", args)
        }

        override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
            val sender = source.sender
            return bukkitCommand.tabComplete(sender, "gas", args)
        }
    }

    override fun onDisable() {
        // 取消所有调度任务
        Bukkit.getScheduler().cancelTasks(this)

        // 清除所有玩家的锁定状态
        Bukkit.getOnlinePlayers().forEach { player ->
            lockService.clearLock(player)
            lockService.resetPlayerDisplayName(player)
        }

        // 关闭数据库连接
        dataService.shutdown()

        // 关闭维护状态检查服务
        maintenanceService.shutdown()

        // 输出关闭信息
        logger.info("§c========================================")
        logger.info("§c  DLRS-GAS For Minecraft 插件已禁用!")
        logger.info("§c========================================")
    }

    /**
     * 玩家加入事件 - 自动登录
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // 检查维护状态（在登录前，同步检查）
        val (isMaintaining, maintMsg) = maintenanceService.checkMaintenanceStatusSync()
        if (isMaintaining) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, Runnable {
                player.kickPlayer(convertColorCodes(maintMsg))
            }, 1L)
            event.joinMessage = null
            logger.info("[DLRS-GAS] 玩家 ${player.name} 尝试加入，但服务器正在维护中")
            return
        }

        // 异步执行自动登录
        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            if (loginService.isLoggedIn(player)) {
                // 尝试自动登录
                val success = autoLoginService.tryAutoLogin(player)
                if (!success) {
                    // 自动登录失败，锁定玩家
                    Bukkit.getScheduler().runTask(this, Runnable {
                        event.joinMessage = null // 不显示加入消息
                        lockService.lockPlayer(player)
                        setUnloggedPlayerListName(player)
                        player.sendMessage("§e[DLRS-GAS] §7自动登录失败，请使用 /gas login 进行登录")
                    })
                } else {
                    // 自动登录成功，显示正常加入消息（在 loggedPlayers 中有记录）
                    loggedPlayers[player.uniqueId] = player.name
                    // 更新 Tab 列表显示
                    tabListService.updatePlayerTabList(player)
                }
            } else {
                // 未登录，锁定玩家
                Bukkit.getScheduler().runTask(this, Runnable {
                    event.joinMessage = null // 不显示加入消息
                    lockService.lockPlayer(player)
                    setUnloggedPlayerListName(player)
                    player.sendMessage("§e[DLRS-GAS] §7请使用 /gas login 进行登录")
                    // 即使未登录也更新 Tab 列表显示
                    tabListService.updatePlayerTabList(player)
                })
            }
        })
    }

    /**
     * 转换颜色代码
     */
    private fun convertColorCodes(text: String): String {
        return text.replace('&', '§')
    }

    /**
     * 设置未登录玩家的 Tab 列表名称（灰色斜体）
     */
    private fun setUnloggedPlayerListName(player: Player) {
        val unloggedName = "§8§o（未登录）${player.name}"
        player.setPlayerListName(unloggedName)
    }

    /**
     * 玩家退出事件 - 清理状态
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        // 清理锁定状态（不发送消息）
        lockService.clearLock(player)
        lockService.resetPlayerDisplayName(player)
    }

    /**
     * 获取 DataService 实例（供其他服务使用）
     */
    fun getDataService(): PlayerDataService {
        return dataService
    }

    /**
     * 获取 TabListService 实例（供其他服务使用）
     */
    fun getTabListService(): TabListService {
        return tabListService
    }

    /**
     * 获取 Config 实例（供其他服务使用）
     */
    fun getConfigManager(): DLRSConfig {
        return config
    }

    /**
     * 重载插件配置
     */
    fun reloadPluginConfig(): Boolean {
        return try {
            // 重新加载配置文件
            reloadConfig()

            // 重新初始化配置管理器
            config = DLRSConfig(this)

            // 重新初始化服务
            loginService = DLRSLoginService(config, dataService)
            autoLoginService = DLRSAutoLoginService(config, dataService)

            // 重新初始化命令处理器
            commandHandler = DLRSCommandHandler(loginService, autoLoginService, dataService)

            // 重新初始化 Tab 列表服务
            tabListService.reload()

            // 重新初始化维护状态检查服务
            maintenanceService.shutdown()
            maintenanceService = MaintenanceService(config)
            maintenanceService.initialize()

            logger.info("§a[DLRS-GAS] 配置已成功重载!")
            true
        } catch (e: Exception) {
            logger.severe("§c[DLRS-GAS] 重载配置失败：${e.message}")
            e.printStackTrace()
            false
        }
    }
}
