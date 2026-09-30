package com.todoink.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSnapshotDao {

    @Query("SELECT * FROM notification_snapshots ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<NotificationSnapshotEntity>>

    @Query("SELECT * FROM notification_snapshots WHERE snapshotId = :id")
    fun observeSnapshot(id: Long): Flow<NotificationSnapshotEntity?>

    @Query("SELECT COUNT(*) FROM notification_snapshots")
    fun observeCount(): Flow<Int>

    @Query(
        "SELECT * FROM notification_snapshots WHERE notificationKey = :key " +
            "ORDER BY contentVersion DESC LIMIT 1",
    )
    suspend fun findLatestByKey(key: String): NotificationSnapshotEntity?

    @Insert
    suspend fun insert(entity: NotificationSnapshotEntity): Long

    @Query("UPDATE notification_snapshots SET observeCount = observeCount + 1 WHERE snapshotId = :id")
    suspend fun incrementObserveCount(id: Long)

    @Query("DELETE FROM notification_snapshots WHERE receivedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
