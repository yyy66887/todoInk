package com.todoink.app.di

import android.content.Context
import androidx.room.Room
import com.todoink.app.data.db.NotificationSnapshotDao
import com.todoink.app.data.db.TodoInkDatabase
import com.todoink.app.data.settings.ListenerStatusStore
import com.todoink.app.data.settings.RetentionSettings
import com.todoink.app.data.settings.SettingsRepository
import com.todoink.app.data.settings.SourceSettings
import com.todoink.app.data.settings.StatusRecorder
import com.todoink.app.time.AppClock
import com.todoink.app.time.SystemAppClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TodoInkDatabase =
        Room.databaseBuilder(context, TodoInkDatabase::class.java, "todoink.db")
            .addMigrations(TodoInkDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideNotificationSnapshotDao(db: TodoInkDatabase): NotificationSnapshotDao =
        db.notificationSnapshotDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideAppClock(): AppClock = SystemAppClock()

    @Provides
    @Singleton
    fun provideSourceSettings(settings: SettingsRepository): SourceSettings = settings

    @Provides
    @Singleton
    fun provideStatusRecorder(store: ListenerStatusStore): StatusRecorder = store

    @Provides
    @Singleton
    fun provideRetentionSettings(settings: SettingsRepository): RetentionSettings = settings
}
