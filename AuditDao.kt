package com.nuerovent.model

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(entry: AuditEntry)

    @Query("SELECT * FROM audit_table ORDER BY timestamp DESC")
    fun getAllAudits(): LiveData<List<AuditEntry>>

    @Query("DELETE FROM audit_table")
    suspend fun clearAllAudits()

    @Query("DELETE FROM audit_table WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

