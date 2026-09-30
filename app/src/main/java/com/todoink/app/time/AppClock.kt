package com.todoink.app.time

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 墙钟时间来源。生产绑定系统时钟；测试注入可控时钟，
 * 使保留期清理等时间敏感行为可以在确定时间下验证（方案 2.5）。
 */
interface AppClock {
    fun nowMillis(): Long
}

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
