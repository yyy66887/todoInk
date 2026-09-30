package com.todoink.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.todoink.app.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "todoink_settings",
)

/** DataStore 文件名共用，供 SettingsRepository 与 ListenerStatusStore 使用。 */
internal val Context.todoInkDataStore: DataStore<Preferences>
    get() = settingsDataStore

/**
 * 来源白名单与保留期配置。
 *
 * 白名单在内存中持有缓存副本，通知回调线程可以用 [isWhitelisted] 同步判断，
 * 而不必阻塞等待 DataStore 读取（方案 2.4：回调内只做轻量判断）。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope appScope: CoroutineScope,
) : SourceSettings, RetentionSettings {
    private object Keys {
        val WHITELIST = stringSetPreferencesKey("whitelisted_packages")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
    }

    private val _whitelist = MutableStateFlow<Set<String>>(emptySet())
    override val whitelist: StateFlow<Set<String>> = _whitelist.asStateFlow()

    /** 是否发生过本地编辑；防止启动加载在首次编辑后用旧值覆盖缓存。 */
    private val hasLocalEdit = AtomicBoolean(false)

    val retentionDays: Flow<Int> = context.todoInkDataStore.data
        .map { prefs -> prefs[Keys.RETENTION_DAYS] ?: DEFAULT_RETENTION_DAYS }

    init {
        appScope.launch {
            val persisted = context.todoInkDataStore.data.first()[Keys.WHITELIST] ?: emptySet()
            if (!hasLocalEdit.get()) {
                _whitelist.value = persisted
            }
        }
    }

    override fun isWhitelisted(packageName: String): Boolean =
        _whitelist.value.contains(packageName)

    override suspend fun setWhitelisted(packageName: String, allowed: Boolean) {
        hasLocalEdit.set(true)
        context.todoInkDataStore.edit { prefs ->
            val current = prefs[Keys.WHITELIST] ?: emptySet()
            prefs[Keys.WHITELIST] =
                if (allowed) current + packageName else current - packageName
        }
        // C02：编辑成功后同步刷新缓存；返回成功即消费侧立即可见，不等待异步流传播。
        _whitelist.value = context.todoInkDataStore.data.first()[Keys.WHITELIST] ?: emptySet()
    }

    override suspend fun getRetentionDays(): Int =
        context.todoInkDataStore.data.first()[Keys.RETENTION_DAYS] ?: DEFAULT_RETENTION_DAYS

    suspend fun setRetentionDays(days: Int) {
        require(days in 1..365)
        context.todoInkDataStore.edit { prefs ->
            prefs[Keys.RETENTION_DAYS] = days
        }
    }

    companion object {
        /** 原始通知短保留期默认值（方案 2.5）。 */
        const val DEFAULT_RETENTION_DAYS = 3
    }
}
