package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IntruderLogEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.SecurityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps ORDER BY isLocked DESC, appName ASC")
    fun getAllLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    fun getActiveLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): LockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: LockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<LockedAppEntity>)

    @Update
    suspend fun update(app: LockedAppEntity)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun setLocked(packageName: String, isLocked: Boolean)

    @Query("UPDATE locked_apps SET fakeCrashEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setFakeCrash(packageName: String, enabled: Boolean)

    @Query("UPDATE locked_apps SET isLocked = :isLocked")
    suspend fun setAllLocked(isLocked: Boolean)

    @Query("SELECT COUNT(*) FROM locked_apps WHERE isLocked = 1")
    fun getLockedCount(): Flow<Int>

    @Delete
    suspend fun delete(app: LockedAppEntity)
}

@Dao
interface IntruderLogDao {
    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<IntruderLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: IntruderLogEntity): Long

    @Query("DELETE FROM intruder_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM intruder_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM intruder_logs")
    fun getLogCount(): Flow<Int>
}

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<SecurityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SecurityLogEntity): Long

    @Query("SELECT COUNT(*) FROM security_logs WHERE eventType = 'UNLOCK' AND isSuccess = 1")
    fun getSuccessfulUnlockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM security_logs WHERE eventType = 'UNLOCK' AND isSuccess = 0")
    fun getFailedUnlockCount(): Flow<Int>
}
