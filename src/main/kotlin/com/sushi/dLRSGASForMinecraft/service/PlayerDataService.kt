package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.model.UserInfo
import org.bukkit.Bukkit
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

/**
 * 玩家数据 SQL 存储服务
 * 使用 SQLite 数据库持久化存储玩家登录信息
 */
class PlayerDataService(private val dataFolder: File) {

    /**
     * 绑定检查结果
     */
    data class BindResult(
        val success: Boolean,
        val message: String,
        val shouldClearData: Boolean = false
    )

    private var connection: Connection? = null
    private val dbPath: File by lazy {
        File(dataFolder, "players.db").also {
            if (!it.exists()) {
                it.parentFile?.mkdirs()
                it.createNewFile()
            }
        }
    }

    /**
     * 初始化数据库连接并创建表
     */
    fun initialize() {
        try {
            // 加载 SQLite JDBC 驱动
            Class.forName("org.sqlite.JDBC")

            // 建立数据库连接
            connection = DriverManager.getConnection("jdbc:sqlite:${dbPath.absolutePath}")

            // 创建玩家数据表
            connection?.createStatement()?.use { stmt ->
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        uid TEXT NOT NULL,
                        nickname TEXT NOT NULL,
                        email TEXT NOT NULL,
                        access_token TEXT NOT NULL,
                        avatar_url TEXT DEFAULT '',
                        user_group TEXT DEFAULT '',
                        last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())

                // 创建账号绑定表
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bindings (
                        uid TEXT PRIMARY KEY NOT NULL,
                        player_uuid TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())
            }

            Bukkit.getLogger().info("[DLRS-GAS] SQLite 数据库初始化完成：${dbPath.absolutePath}")
        } catch (e: Exception) {
            Bukkit.getLogger().severe("[DLRS-GAS] SQLite 数据库初始化失败：${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 保存或更新玩家信息
     */
    fun saveUserInfo(playerUuid: UUID, userInfo: UserInfo) {
        connection?.prepareStatement("""
            INSERT OR REPLACE INTO players (uuid, uid, nickname, email, access_token, avatar_url, user_group, last_login)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """.trimIndent())?.use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, userInfo.uid)
            stmt.setString(3, userInfo.nickname)
            stmt.setString(4, userInfo.email)
            stmt.setString(5, userInfo.accessToken)
            stmt.setString(6, userInfo.avatarUrl)
            stmt.setString(7, userInfo.userGroup)
            stmt.executeUpdate()
        }
    }

    /**
     * 获取玩家的用户信息
     */
    fun getPlayerInfo(playerUuid: UUID): UserInfo? {
        return connection?.prepareStatement("SELECT * FROM players WHERE uuid = ?")?.use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val userGroup = rs.getString("user_group") ?: ""
                    UserInfo(
                        uid = rs.getString("uid"),
                        nickname = rs.getString("nickname"),
                        email = rs.getString("email"),
                        accessToken = rs.getString("access_token"),
                        avatarUrl = rs.getString("avatar_url") ?: "",
                        userGroup = userGroup,
                        isInGroup2 = userGroup.split(",").contains("2")
                    )
                } else {
                    null
                }
            }
        }
    }

    /**
     * 检查玩家是否已登录（有存储的 access_token）
     */
    fun isLoggedIn(playerUuid: UUID): Boolean {
        return connection?.prepareStatement("SELECT access_token FROM players WHERE uuid = ?")?.use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.executeQuery().use { rs ->
                rs.next() && rs.getString("access_token") != null
            }
        } ?: false
    }

    /**
     * 清除玩家数据（登出时使用）
     */
    fun clearPlayerData(playerUuid: UUID) {
        connection?.prepareStatement("DELETE FROM players WHERE uuid = ?")?.use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.executeUpdate()
        }
    }

    /**
     * 更新玩家 access_token
     */
    fun updateAccessToken(playerUuid: UUID, accessToken: String) {
        connection?.prepareStatement("UPDATE players SET access_token = ?, last_login = CURRENT_TIMESTAMP WHERE uuid = ?")?.use { stmt ->
            stmt.setString(1, accessToken)
            stmt.setString(2, playerUuid.toString())
            stmt.executeUpdate()
        }
    }

    /**
     * 关闭数据库连接
     */
    fun shutdown() {
        try {
            connection?.close()
            Bukkit.getLogger().info("[DLRS-GAS] SQLite 数据库连接已关闭")
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[DLRS-GAS] 关闭数据库连接时出错：${e.message}")
        }
    }

    /**
     * 检查 GAS UID 是否已被绑定
     */
    fun isUidBound(uid: String): Boolean {
        return connection?.prepareStatement("SELECT uid FROM bindings WHERE uid = ?")?.use { stmt ->
            stmt.setString(1, uid)
            stmt.executeQuery().use { rs ->
                rs.next()
            }
        } ?: false
    }

    /**
     * 获取绑定了指定 UID 的玩家 UUID
     */
    fun getBoundPlayerUuid(uid: String): String? {
        return connection?.prepareStatement("SELECT player_uuid FROM bindings WHERE uid = ?")?.use { stmt ->
            stmt.setString(1, uid)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getString("player_uuid") else null
            }
        }
    }

    /**
     * 获取绑定了指定 UID 的玩家昵称
     */
    fun getBoundPlayerName(uid: String): String? {
        return connection?.prepareStatement("SELECT player_name FROM bindings WHERE uid = ?")?.use { stmt ->
            stmt.setString(1, uid)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getString("player_name") else null
            }
        }
    }

    /**
     * 获取玩家 UUID 绑定的 GAS UID
     */
    fun getBoundUid(playerUuid: String): String? {
        return connection?.prepareStatement("SELECT uid FROM bindings WHERE player_uuid = ?")?.use { stmt ->
            stmt.setString(1, playerUuid)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getString("uid") else null
            }
        }
    }

    /**
     * 绑定 GAS 账号与玩家
     */
    fun bindAccount(uid: String, playerUuid: String, playerName: String) {
        connection?.prepareStatement("""
            INSERT OR REPLACE INTO bindings (uid, player_uuid, player_name, bound_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        """.trimIndent())?.use { stmt ->
            stmt.setString(1, uid)
            stmt.setString(2, playerUuid)
            stmt.setString(3, playerName)
            stmt.executeUpdate()
        }
    }

    /**
     * 解绑 GAS 账号
     */
    fun unbindAccountByUid(uid: String): Boolean {
        return connection?.prepareStatement("DELETE FROM bindings WHERE uid = ?")?.use { stmt ->
            stmt.setString(1, uid)
            val rows = stmt.executeUpdate()
            rows > 0
        } ?: false
    }

    /**
     * 解绑玩家的所有绑定
     */
    fun unbindAccountByPlayerUuid(playerUuid: String): Boolean {
        return connection?.prepareStatement("DELETE FROM bindings WHERE player_uuid = ?")?.use { stmt ->
            stmt.setString(1, playerUuid)
            val rows = stmt.executeUpdate()
            rows > 0
        } ?: false
    }

    /**
     * 检查并处理账号绑定
     * @return BindResult 绑定结果，success 为 false 时需要踢出玩家
     */
    fun checkAndBindAccount(uid: String, playerUuid: String, playerName: String): BindResult {
        val boundPlayerUuid = getBoundPlayerUuid(uid)
        val boundUid = getBoundUid(playerUuid)

        // 情况 1: UID 未被绑定，玩家 UUID 也未被绑定 -> 新绑定
        if (boundPlayerUuid == null && boundUid == null) {
            bindAccount(uid, playerUuid, playerName)
            Bukkit.getLogger().info("[DLRS-GAS] 新绑定：UID=$uid, Player=$playerName ($playerUuid)")
            return BindResult(true, "绑定成功")
        }

        // 情况 2: UID 已被绑定到当前玩家 -> 检查昵称是否变化
        if (boundPlayerUuid == playerUuid) {
            val boundPlayerName = getBoundPlayerName(uid)
            if (boundPlayerName != playerName) {
                // 昵称变化，更新绑定
                bindAccount(uid, playerUuid, playerName)
                Bukkit.getLogger().info("[DLRS-GAS] 更新昵称：UID=$uid, 旧昵称=$boundPlayerName, 新昵称=$playerName")
            }
            return BindResult(true, "已绑定")
        }

        // 情况 3: UID 已被绑定到其他玩家 -> 踢出并解除当前玩家的本地数据
        if (boundPlayerUuid != null && boundPlayerUuid != playerUuid) {
            val boundPlayerName = getBoundPlayerName(uid)
            val kickMessage = """
                §c[DLRS-GAS] 该 GAS 账号已绑定至其他玩家
                绑定的玩家：$boundPlayerName
                你的玩家 ID: $playerName
                已清除你的本地绑定，请使用其他 GAS 账号或联系管理员
            """.trimIndent()
            // 清除当前玩家的本地登录数据，允许下次使用其他账号登录
            clearPlayerData(java.util.UUID.fromString(playerUuid))
            return BindResult(false, kickMessage)
        }

        // 情况 4: UID 未被绑定，但玩家 UUID 已绑定过其他 UID -> 允许重新绑定
        bindAccount(uid, playerUuid, playerName)
        Bukkit.getLogger().info("[DLRS-GAS] 重新绑定：UID=$uid, Player=$playerName ($playerUuid)")
        return BindResult(true, "重新绑定成功")
    }
}
