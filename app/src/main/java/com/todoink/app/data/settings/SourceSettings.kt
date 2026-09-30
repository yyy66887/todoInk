package com.todoink.app.data.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * 来源白名单的同步读取与更新。
 *
 * 通知回调线程用 [isWhitelisted] 同步判断（C01：不能为等待 DataStore 阻塞）；
 * [setWhitelisted] 返回成功时内存缓存必须已更新（C02：关闭成功即消费侧可见）。
 */
interface SourceSettings {
    val whitelist: StateFlow<Set<String>>

    fun isWhitelisted(packageName: String): Boolean

    suspend fun setWhitelisted(packageName: String, allowed: Boolean)
}
