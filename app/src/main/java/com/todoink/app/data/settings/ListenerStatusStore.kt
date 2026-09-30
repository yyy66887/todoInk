package com.todoink.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.todoink.app.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 监听器运行状态。连接状态仅存在于内存；最近回调、最近保存与最近错误
 * 持久化到 DataStore，进程重建后仍可查看（方案 2.4）。
 */
@Singleton
class ListenerStatusStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
) : StatusRecorder {
    data class Status(
        val listenerConnected: Boolean = false,
        val lastCallbackAt: Long? = null,
        val lastSavedAt: Long? = null,
        val lastError: String? = null,
        val lastErrorAt: Long? = null,
    )

    private object Keys {
        val LAST_CALLBACK = longPreferencesKey("last_callback_at")
        val LAST_SAVED = longPreferencesKey("last_saved_at")
        val LAST_ERROR = stringPreferencesKey("last_error")
        val LAST_ERROR_AT = longPreferencesKey("last_error_at")
    }

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    init {
        appScope.launch {
            val prefs = context.todoInkDataStore.data.first()
            _status.value = Status(
                listenerConnected = false,
                lastCallbackAt = prefs[Keys.LAST_CALLBACK],
                lastSavedAt = prefs[Keys.LAST_SAVED],
                lastError = prefs[Keys.LAST_ERROR],
                lastErrorAt = prefs[Keys.LAST_ERROR_AT],
            )
        }
    }

    fun setListenerConnected(connected: Boolean) {
        _status.value = _status.value.copy(listenerConnected = connected)
    }

    override fun recordCallback() {
        _status.value = _status.value.copy(lastCallbackAt = System.currentTimeMillis())
    }

    override fun recordSaved() {
        val now = System.currentTimeMillis()
        _status.value = _status.value.copy(lastSavedAt = now, lastError = null, lastErrorAt = null)
        persistAsync { prefs ->
            prefs[Keys.LAST_SAVED] = now
            prefs.remove(Keys.LAST_ERROR)
            prefs.remove(Keys.LAST_ERROR_AT)
        }
    }

    override fun recordError(message: String) {
        val now = System.currentTimeMillis()
        _status.value = _status.value.copy(lastError = message, lastErrorAt = now)
        persistAsync { prefs ->
            prefs[Keys.LAST_ERROR] = message
            prefs[Keys.LAST_ERROR_AT] = now
        }
    }

    private fun persistAsync(
        update: (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        appScope.launch {
            runCatching { context.todoInkDataStore.edit(update) }
        }
    }
}
