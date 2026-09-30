package com.todoink.app.data.db

import androidx.room.TypeConverter
import com.todoink.app.notification.model.ParsedMessage
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromTextLines(lines: List<String>): String = lines.joinToString(separator = "\u001F")

    @TypeConverter
    fun toTextLines(stored: String): List<String> =
        if (stored.isEmpty()) emptyList() else stored.split('\u001F')

    /**
     * NI-003 步骤 B：消息列表以 JSON 持久化，保证换行、U+001F、中文、
     * 空元素等受控字符无损往返（G10 的 U+001F 拼接仅保留给 textLines 旧数据）。
     */
    @TypeConverter
    fun fromParsedMessages(messages: List<ParsedMessage>): String {
        if (messages.isEmpty()) return ""
        val array = JSONArray()
        messages.forEach { message ->
            val obj = JSONObject()
            message.senderName?.let { obj.put(KEY_SENDER, it) }
            obj.put(KEY_TEXT, message.text)
            message.messageTimestamp?.let { obj.put(KEY_TIMESTAMP, it) }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toParsedMessages(stored: String): List<ParsedMessage> {
        if (stored.isBlank()) return emptyList()
        val array = JSONArray(stored)
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(
                    ParsedMessage(
                        senderName = obj.optString(KEY_SENDER).takeIf { obj.has(KEY_SENDER) },
                        text = obj.getString(KEY_TEXT),
                        messageTimestamp = if (obj.has(KEY_TIMESTAMP)) obj.getLong(KEY_TIMESTAMP) else null,
                    ),
                )
            }
        }
    }

    private companion object {
        const val KEY_SENDER = "sender"
        const val KEY_TEXT = "text"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
