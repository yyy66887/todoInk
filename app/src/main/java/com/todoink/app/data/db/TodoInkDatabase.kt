package com.todoink.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NotificationSnapshotEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TodoInkDatabase : RoomDatabase() {
    abstract fun notificationSnapshotDao(): NotificationSnapshotDao

    companion object {
        /**
         * v1 → v2（NI-003 步骤 B）：新增汇总标记与消息结构列。
         * 历史记录没有保存的消息正文不回补，置空由读取模型标记历史信息不足。
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notification_snapshots " +
                        "ADD COLUMN isGroupSummaryMessage INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE notification_snapshots " +
                        "ADD COLUMN parsedMessages TEXT NOT NULL DEFAULT ''",
                )
            }
        }
    }
}
