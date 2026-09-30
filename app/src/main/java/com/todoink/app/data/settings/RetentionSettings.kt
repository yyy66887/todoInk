package com.todoink.app.data.settings

/** 保留期配置的窄接口，供持久化边界与清理逻辑读取，便于测试替换。 */
interface RetentionSettings {
    suspend fun getRetentionDays(): Int
}
