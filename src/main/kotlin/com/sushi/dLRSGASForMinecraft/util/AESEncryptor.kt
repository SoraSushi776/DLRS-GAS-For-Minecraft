package com.sushi.dLRSGASForMinecraft.util

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * AES-256-CBC 加密工具类
 * 用于加密DLRS-GAS的appToken
 */
object AESEncryptor {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "SHA-256"
    
    /**
     * 使用AES-256-CBC加密数据
     * 
     * @param data 要加密的明文数据
     * @param key 加密密钥（将被SHA256哈希处理）
     * @return Base64编码的加密结果
     */
    fun encrypt(data: String, key: String): String {
        // 1. 对密钥做SHA256哈希，得到32字节密钥
        val keyBytes = MessageDigest.getInstance(KEY_ALGORITHM)
            .digest(key.toByteArray(Charsets.UTF_8))
        
        // 2. 全0的16字节IV
        val iv = ByteArray(16)
        
        // 3. 创建AES-256-CBC加密器
        val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        
        // 4. 加密
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        // 5. 转Base64返回
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }
}
