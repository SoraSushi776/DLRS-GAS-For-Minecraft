package com.sushi.dLRSGASForMinecraft.service

import com.sushi.dLRSGASForMinecraft.DLRSGASForMinecraft
import com.sushi.dLRSGASForMinecraft.model.UserInfo
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 双重密码服务
 * 用于保护玩家的GAS账号访问权限
 */
class DoublePasswordService(
    private val dataFolder: File
) {
    companion object {
        private const val DB_NAME = "double_password.db"
        private const val TABLE_NAME = "double_passwords"
        private val instance: DoublePasswordService by lazy { DoublePasswordService(DLRSGASForMinecraft.instance.dataFolder) }

        // 使用直接访问方式，避免重复方法
        val INSTANCE: DoublePasswordService
            get() = instance
    }

    private var connection: Connection? = null
    private val dbPath: File by lazy {
        File(dataFolder, DB_NAME).also {
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
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:${dbPath.absolutePath}")

            // 创建双重密码表
            connection?.createStatement()?.use { stmt ->
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                        player_uuid TEXT PRIMARY KEY,
                        double_password_hash TEXT NOT NULL,
                        salt TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())
            }

            Bukkit.getLogger().info("[DLRS-GAS] 双重密码服务数据库初始化完成：${dbPath.absolutePath}")
        } catch (e: Exception) {
            Bukkit.getLogger().severe("[DLRS-GAS] 双重密码服务数据库初始化失败：${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 设置双重密码
     */
    fun setDoublePassword(playerUuid: UUID, password: String): Pair<Boolean, String> {
        try {
            // 生成盐值
            val salt = generateSalt()

            // 使用SHA-256 + 盐值进行哈希
            val passwordHash = hashPassword(password, salt)

            // 保存到数据库
            connection?.prepareStatement("""
                INSERT OR REPLACE INTO $TABLE_NAME (player_uuid, double_password_hash, salt)
                VALUES (?, ?, ?)
            """.trimIndent())?.use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, passwordHash)
                stmt.setString(3, salt)
                stmt.executeUpdate()
            }

            return Pair(true, "双重密码设置成功")
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(false, "设置双重密码失败：${e.message}")
        }
    }

    /**
     * 验证双重密码
     */
    fun verifyDoublePassword(playerUuid: UUID, password: String): Boolean {
        return try {
            // 查询数据库中的盐值和哈希值
            val result = connection?.prepareStatement("""
                SELECT double_password_hash, salt FROM $TABLE_NAME WHERE player_uuid = ?
            """.trimIndent())?.use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getString("double_password_hash") to rs.getString("salt")
                    } else {
                        null
                    }
                }
            }

            result?.let { (hash, salt) ->
                // 验证密码
                hashPassword(password, salt) == hash
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 检查玩家是否已设置双重密码
     */
    fun hasDoublePassword(playerUuid: UUID): Boolean {
        return try {
            val result = connection?.prepareStatement("""
                SELECT COUNT(*) as count FROM $TABLE_NAME WHERE player_uuid = ?
            """.trimIndent())?.use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    rs.getInt("count") > 0
                }
            }
            result ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除双重密码
     */
    fun deleteDoublePassword(playerUuid: UUID): Boolean {
        return try {
            val result = connection?.prepareStatement("""
                DELETE FROM $TABLE_NAME WHERE player_uuid = ?
            """.trimIndent())?.use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeUpdate() > 0
            }
            result ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 生成盐值
     */
    private fun generateSalt(): String {
        return (1..16).joinToString("") {
            (('a'..'z') + ('A'..'Z') + ('0'..'9')).random().toString()
        }
    }

    /**
     * 使用SHA-256 + 盐值进行密码哈希
     */
    private fun hashPassword(password: String, salt: String): String {
        val message = "$password$salt"
        val bytes = message.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 处理双重密码验证
     * 在GAS登录成功后，要求玩家设置或验证双重密码
     */
    fun handleDoublePasswordVerification(player: Player, userInfo: UserInfo) {
        val playerUuid = player.uniqueId

        if (!hasDoublePassword(playerUuid)) {
            // 未设置双重密码，提示设置
            player.sendMessage("§e[DLRS-GAS] §7请设置双重密码来保护您的账号")
            player.sendMessage("§e[DLRS-GAS] §7使用 /gas double-password set <密码> 设置您的双重密码")
            player.sendMessage("§e[DLRS-GAS] §7双重密码必须是4-16位数字或字母")
            return
        }

        // 已设置双重密码，提示验证
        player.sendMessage("§e[DLRS-GAS] §7请验证您的双重密码")
        player.sendMessage("§e[DLRS-GAS] §7使用 /gas double-password verify <密码> 验证")
        player.sendMessage("§e[DLRS-GAS] §7或者使用 /login <密码> 快速验证")
    }

    /**
     * 获取双重密码格式要求
     */
    fun getDoublePasswordFormatRequirement(): String {
        return "§e[DLRS-GAS] §7双重密码格式要求：4-16位数字或字母"
    }

    /**
     * 验证双重密码格式
     */
    fun isValidDoublePasswordFormat(password: String): Boolean {
        return password.length in 4..16 && password.matches(Regex("[a-zA-Z0-9]+"))
    }

    /**
     * 关闭数据库连接
     */
    fun shutdown() {
        try {
            connection?.close()
            Bukkit.getLogger().info("[DLRS-GAS] 双重密码服务数据库连接已关闭")
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[DLRS-GAS] 关闭双重密码服务数据库连接时出错：${e.message}")
        }
    }
}