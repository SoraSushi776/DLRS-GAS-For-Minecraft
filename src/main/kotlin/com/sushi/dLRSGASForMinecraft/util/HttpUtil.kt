package com.sushi.dLRSGASForMinecraft.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONException

/**
 * HTTP请求工具类
 * 用于与DLRS API进行通信
 */
object HttpUtil {
    
    private const val USER_AGENT = "DLRS-GAS-Minecraft-Plugin/1.0"
    private const val CONNECT_TIMEOUT = 10000 // 10秒
    private const val READ_TIMEOUT = 10000 // 10秒
    
    /**
     * 发送POST请求
     * 
     * @param urlString URL地址
     * @param jsonBody JSON请求体
     * @return JSONObject响应结果
     */
    fun postJson(urlString: String, jsonBody: String): JSONObject? {
        var connection: HttpURLConnection? = null
        
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            
            // 设置请求属性
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            // 发送请求体
            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            
            // 读取响应
            val responseCode = connection.responseCode
            val inputStream = if (responseCode >= 200 && responseCode < 300) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val response = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
            
            return JSONObject(response)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * 构建JSON对象
     */
    fun createJsonObject(vararg pairs: Pair<String, Any>): JSONObject {
        val json = JSONObject()
        for ((key, value) in pairs) {
            json.put(key, value)
        }
        return json
    }
}
