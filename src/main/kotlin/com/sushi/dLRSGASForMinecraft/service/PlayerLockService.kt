package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import com.sushi.dLRSGASForMinecraft.model.UserInfo
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * 玩家锁定服务
 * 用于管理未登录玩家的限制效果
 */
class PlayerLockService {

    companion object {
        // 存储被锁定的玩家 UUID
        private val lockedPlayers = mutableSetOf<UUID>()
        // 存储玩家原来的游戏模式
        private val originalGameModes = mutableMapOf<UUID, GameMode>()
    }

    /**
     * 锁定玩家 - 施加限制效果
     * - 失明效果
     * - 无法移动（缓慢 VI + 跳跃提升负面效果）
     * - 无法攻击
     * - 无法破坏方块
     * - 不受伤害
     */
    fun lockPlayer(player: Player) {
        if (lockedPlayers.contains(player.uniqueId)) {
            return // 已经锁定，不重复施加
        }

        lockedPlayers.add(player.uniqueId)

        // 保存玩家原来的游戏模式
        originalGameModes[player.uniqueId] = player.gameMode

        // 施加失明效果（无限持续时间）
        player.addPotionEffect(PotionEffect(
            PotionEffectType.BLINDNESS,
            PotionEffect.INFINITE_DURATION,
            0,
            false, // 不显示粒子效果
            false  // 不显示图标
        ))

        // 施加缓慢 VI 效果（让速度降到极低，几乎无法移动）
        // 在 1.20.5+ 中，缓慢 VI 可以将速度降到 0
        player.addPotionEffect(PotionEffect(
            PotionEffectType.SLOWNESS,
            PotionEffect.INFINITE_DURATION,
            255, // 最大等级，完全无法移动
            false,
            false
        ))

        // 施加跳跃提升负面效果（无法跳跃）
        player.addPotionEffect(PotionEffect(
            PotionEffectType.JUMP_BOOST,
            PotionEffect.INFINITE_DURATION,
            -4, // 负面等级，无法跳跃
            false,
            false
        ))

        // 设置为冒险模式，防止破坏/放置方块
        player.gameMode = GameMode.ADVENTURE

        player.isInvulnerable = true // 无敌

        player.sendMessage("§c[DLRS-GAS] §7您尚未登录，账号已锁定")
        player.sendMessage("§e[DLRS-GAS] §7请使用 /gas login 进行登录以解锁")
    }

    /**
     * 解锁玩家 - 移除所有限制效果
     */
    fun unlockPlayer(player: Player) {
        if (!lockedPlayers.contains(player.uniqueId)) {
            return // 未被锁定，无需解锁
        }

        lockedPlayers.remove(player.uniqueId)

        // 移除失明效果
        player.removePotionEffect(PotionEffectType.BLINDNESS)

        // 移除缓慢效果
        player.removePotionEffect(PotionEffectType.SLOWNESS)

        // 移除跳跃提升效果
        player.removePotionEffect(PotionEffectType.JUMP_BOOST)

        // 恢复玩家原来的游戏模式
        originalGameModes.remove(player.uniqueId)?.let {
            player.gameMode = it
        }

        // 取消无敌状态
        player.isInvulnerable = false

        player.sendMessage("§a[DLRS-GAS] §7账号已解锁，祝您游戏愉快！")
    }

    /**
     * 检查玩家是否被锁定
     */
    fun isLocked(player: Player): Boolean {
        return lockedPlayers.contains(player.uniqueId)
    }

    /**
     * 设置玩家权限 - 根据用户组设置 OP 状态
     * 用户组 104 = OP 权限
     */
    fun setPlayerPermissions(player: Player, userInfo: UserInfo) {
        val hasGroup104 = userInfo.getUserGroups().contains("104")

        if (hasGroup104) {
            // 给予 OP 权限
            if (!player.isOp) {
                player.isOp = true
                player.sendMessage("§a[DLRS-GAS] §7检测到您在用户组 104，已授予 OP 权限")
            }
        } else {
            // 取消 OP 权限
            if (player.isOp) {
                player.isOp = false
                player.sendMessage("§e[DLRS-GAS] §7您不在用户组 104，已取消 OP 权限")
            }
        }
    }

    /**
     * 设置玩家显示名称 - GAS 昵称（UID）
     * 登录成功后调用，Tab 列表显示白色：GAS 用户名（游戏 ID）
     */
    fun setPlayerDisplayName(player: Player, userInfo: UserInfo, joinMessage: String? = null) {
        val displayName = "§f${userInfo.nickname}§7(${userInfo.uid})"
        player.setDisplayName(displayName)

        // Tab 列表显示白色：GAS 用户名（游戏 ID）
        val playerListName = "§f${userInfo.nickname} §7(${player.name})"
        player.setPlayerListName(playerListName)

        player.setCustomName(displayName)
        player.isCustomNameVisible = true
    }

    /**
     * 重置玩家显示名称
     */
    fun resetPlayerDisplayName(player: Player) {
        player.setDisplayName(null)
        player.setPlayerListName(null)
        player.setCustomName(null)
        player.isCustomNameVisible = false
    }

    /**
     * 清除玩家锁定状态（不发送消息，用于登出）
     */
    fun clearLock(player: Player) {
        lockedPlayers.remove(player.uniqueId)
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.JUMP_BOOST)
        originalGameModes.remove(player.uniqueId)?.let {
            player.gameMode = it
        }
        player.isInvulnerable = false
    }
}
