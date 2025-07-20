package com.nuerovent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_table")
data class AuditEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val avgTemperature: Float,
    val avgHumidity: Float,
    val avgPressure: Float,
    val avgAirQuality: Float
)
