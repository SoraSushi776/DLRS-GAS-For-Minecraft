package com.sushi.dLRSGASForMinecraft.util

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * AES-256-CBC 加密工具类
 * 用于加密 DLRS-GAS 的 appToken
 */
object AESEncryptor {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "SHA-256"

    /**
     * 使用 AES-256-CBC 加密数据
     *
     * @param data 要加密的明文数据
     * @param key 加密密钥（将被 SHA256 哈希处理）
     * @return Base64 编码的加密结果
     */
    fun encrypt(data: String, key: String): String {
        // 1. 对密钥做 SHA256 哈希，得到 32 字节密钥
        val keyBytes = MessageDigest.getInstance(KEY_ALGORITHM)
            .digest(key.toByteArray(Charsets.UTF_8))

        // 2. 全 0 的 16 字节 IV
        val iv = ByteArray(16)

        // 3. 创建 AES-256-CBC 加密器
        val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        // 4. 加密
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // 5. 转 Base64 返回
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    /**
     * 使用 AES-256-CBC 解密数据
     *
     * @param data Base64 编码的加密数据
     * @param key 解密密钥（将被 SHA256 哈希处理）
     * @return 解密后的明文
     */
    fun decrypt(data: String, key: String): String {
        // 1. 对密钥做 SHA256 哈希，得到 32 字节密钥
        val keyBytes = MessageDigest.getInstance(KEY_ALGORITHM)
            .digest(key.toByteArray(Charsets.UTF_8))

        // 2. 全 0 的 16 字节 IV
        val iv = ByteArray(16)

        // 3. 创建 AES-256-CBC 解密器
        val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        // 4. 解密
        val decodedBytes = Base64.getDecoder().decode(data)
        val decryptedBytes = cipher.doFinal(decodedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }
}
