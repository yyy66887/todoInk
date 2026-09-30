package com.todoink.app.notification.parser

import android.util.Log
import com.todoink.app.notification.model.NotificationSnapshotDraft
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 按来源选择解析器：包名精确匹配优先，否则回退到通用解析器。
 * 后续新增 App 专属解析器时在此注册，不堆叠在一个大函数里（方案 4.2）。
 */
@Singleton
class ParserRegistry @Inject constructor(
    private val genericParser: GenericParser,
) {
    // 未来注册 App 专属解析器，例如 WeChatParser、WecomParser、QQParser。
    private val parsers: List<NotificationParser> = emptyList()

    fun parse(draft: NotificationSnapshotDraft): ParseResult {
        val parser = parsers.firstOrNull { it.sourcePackage == draft.packageName }
            ?: genericParser
        return try {
            parser.parse(draft)
        } catch (e: Exception) {
            Log.w(TAG, "parser failed for ${draft.packageName}", e)
            ParseResult(
                parserVersion = parser.parserVersion,
                status = ParseStatus.UNSUPPORTED,
                availability = ContentAvailability.PARTIAL,
                messages = emptyList(),
            )
        }
    }

    private companion object {
        const val TAG = "ParserRegistry"
    }
}
