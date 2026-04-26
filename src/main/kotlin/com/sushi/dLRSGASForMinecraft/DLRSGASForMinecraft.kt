package com.sushi.dLRSGASForMinecraft

import com.sushi.dLRSGASForMinecraft.command.DLRSCommandHandler
import com.sushi.dLRSGASForMinecraft.config.DLRSConfig
import com.sushi.dLRSGASForMinecraft.listener.PlayerLockListener
import com.sushi.dLRSGASForMinecraft.service.DLRSAutoLoginService
import com.sushi.dLRSGASForMinecraft.service.DLRSLoginService
import com.sushi.dLRSGASForMinecraft.service.PlayerLockService
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class DLRSGASForMinecraft : JavaPlugin(), Listener {

    private lateinit var config: DLRSConfig
    private lateinit var loginService: DLRSLoginService
    private lateinit var autoLoginService: DLRSAutoLoginService
    private lateinit var lockService: PlayerLockService
    private lateinit var commandHandler: DLRSCommandHandler
    private lateinit var lockListener: PlayerLockListener

    companion object {
        lateinit var instance: DLRSGASForMinecraft
            private set
        lateinit var lockServiceInstance: PlayerLockService
            private set
    }

    override fun onEnable() {
        instance = this

        // 保存默认配置
        saveDefaultConfig()

        // 初始化配置管理器
        config = DLRSConfig(this)

        // 初始化服务
        loginService = DLRSLoginService(config)
        autoLoginService = DLRSAutoLoginService(config)
        lockService = PlayerLockService()

        // 保存静态引用
        lockServiceInstance = lockService

        // 初始化命令处理器
        commandHandler = DLRSCommandHandler(loginService, autoLoginService)

        // 初始化锁定监听器
        lockListener = PlayerLockListener(lockService)

        // 注册命令（使用 registerCommand 方法）
        registerDlrsCommand()

        // 注册事件监听器
        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(this, this)
        pluginManager.registerEvents(lockListener, this)

        // 输出启动信息
        logger.info("§a========================================")
        logger.info("§a  DLRS-GAS For Minecraft 插件已启用!")
        logger.info("§a  版本：${description.version}")
        logger.info("§a  作者：${description.authors}")
        logger.info("§a========================================")
    }

    /**
     * 注册 DLRS 命令（使用 registerCommand 方法）
     */
    private fun registerDlrsCommand() {
        // 创建一个包装类，同时支持 execute 和 suggest (tabComplete)
        val dlrsCommand = DlrsCommand()

        // 使用 registerCommand 注册命令
        this.registerCommand(
            "dlrs",
            "DLRS-GAS 账号系统主命令",
            listOf("dlrsgas", "dlrsl"),
            dlrsCommand
        )
    }

    /**
     * DLRS 命令实现类
     */
    private inner class DlrsCommand : BasicCommand {

        private val bukkitCommand = object : org.bukkit.command.Command("dlrs", "", "/dlrs", listOf("dlrsgas", "dlrsl")) {
            override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
                return commandHandler.onCommand(sender, this, label, args)
            }

            override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
                return commandHandler.onTabComplete(sender, this, alias, args) ?: emptyList()
            }
        }

        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val sender = source.executor as CommandSender
            bukkitCommand.execute(sender, "dlrs", args)
        }

        override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
            val sender = source.executor as CommandSender
            return bukkitCommand.tabComplete(sender, "dlrs", args)
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

        // 异步执行自动登录
        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            if (loginService.isLoggedIn(player)) {
                // 尝试自动登录
                val success = autoLoginService.tryAutoLogin(player)
                if (!success) {
                    // 自动登录失败，锁定玩家
                    Bukkit.getScheduler().runTask(this, Runnable {
                        lockService.lockPlayer(player)
                    })
                }
            } else {
                // 未登录，锁定玩家
                Bukkit.getScheduler().runTask(this, Runnable {
                    lockService.lockPlayer(player)
                    player.sendMessage("§e[DLRS-GAS] §7请使用 /dlrs login 进行登录")
                })
            }
        })
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
}
