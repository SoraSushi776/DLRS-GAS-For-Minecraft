package com.sushi.dLRSGASForMinecraft.listener

import com.sushi.dLRSGASForMinecraft.service.PlayerLockService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

/**
 * 玩家锁定事件监听器
 * 阻止被锁定玩家的攻击、破坏方块等行为
 */
class PlayerLockListener(
    private val lockService: PlayerLockService
) : Listener {

    /**
     * 阻止被锁定的玩家攻击其他实体
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerAttack(event: EntityDamageByEntityEvent) {
        if (event.damager !is org.bukkit.entity.Player) return

        val player = event.damager as org.bukkit.entity.Player
        if (lockService.isLocked(player)) {
            event.isCancelled = true
            player.sendMessage("§c[DLRS-GAS] §7请先登录再进行攻击")
        }
    }

    /**
     * 阻止被锁定的玩家受到伤害
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity !is org.bukkit.entity.Player) return

        val player = event.entity as org.bukkit.entity.Player
        if (lockService.isLocked(player)) {
            event.isCancelled = true
        }
    }

    /**
     * 阻止被锁定的玩家放置方块
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (lockService.isLocked(event.player)) {
            event.isCancelled = true
            event.player.sendMessage("§c[DLRS-GAS] §7请先登录再放置方块")
        }
    }

    /**
     * 阻止被锁定的玩家破坏方块
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (lockService.isLocked(event.player)) {
            event.isCancelled = true
            event.player.sendMessage("§c[DLRS-GAS] §7请先登录再破坏方块")
        }
    }

    /**
     * 阻止被锁定的玩家交互（右键、左键点击方块等）
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (lockService.isLocked(event.player)) {
            event.isCancelled = true
        }
    }
}
