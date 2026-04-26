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
}
