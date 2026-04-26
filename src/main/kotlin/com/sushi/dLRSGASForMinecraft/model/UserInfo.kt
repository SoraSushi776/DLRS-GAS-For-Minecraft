package com.sushi.dLRSGASForMinecraft.model

/**
 * 用户信息数据模型
 */
data class UserInfo(
    val uid: String,              // 用户唯一标识
    val nickname: String,         // 用户昵称
    val email: String,            // 用户邮箱
    val accessToken: String,      // 访问令牌
    val avatarUrl: String,        // 头像 URL
    val userGroup: String,        // 用户组（逗号分隔，如 "1,2,3"）
    val isInGroup2: Boolean       // 是否在用户组 2 中
) {
    /**
     * 获取用户组列表
     */
    fun getUserGroups(): List<String> {
        return if (userGroup.isNotBlank()) {
            userGroup.split(",").map { it.trim() }
        } else {
            emptyList()
        }
    }
    
    /**
     * 检查是否在指定用户组中
     */
    fun isInGroup(groupId: String): Boolean {
        return getUserGroups().contains(groupId)
    }
}
