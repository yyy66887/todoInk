package com.todoink.app.data.settings

/**
 * 采集管道向状态页记录事件的窄接口。
 * 抽出接口是为了让管道行为在 JVM 测试中可观察，替换为可控记录器。
 */
interface StatusRecorder {
    fun recordCallback()

    fun recordSaved()

    fun recordError(message: String)
}
