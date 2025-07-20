package com.nuerovent.model

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.*
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val auditDao = AppDatabase.getDatabase(application).auditDao()

    private val _temperatureEntries = MutableLiveData<MutableList<Entry>>(mutableListOf())
    val temperatureEntries: LiveData<MutableList<Entry>> = _temperatureEntries

    private val _humidityEntries = MutableLiveData<MutableList<Entry>>(mutableListOf())
    val humidityEntries: LiveData<MutableList<Entry>> = _humidityEntries

    private val _pressureEntries = MutableLiveData<MutableList<Entry>>(mutableListOf())
    val pressureEntries: LiveData<MutableList<Entry>> = _pressureEntries

    private val _airQualityEntries = MutableLiveData<MutableList<Entry>>(mutableListOf())
    val airQualityEntries: LiveData<MutableList<Entry>> = _airQualityEntries

    private val _latestTemperature = MutableLiveData<Float?>()
    val latestTemperature: LiveData<Float?> = _latestTemperature

    private val _latestHumidity = MutableLiveData<Float?>()
    val latestHumidity: LiveData<Float?> = _latestHumidity

    private val _latestPressure = MutableLiveData<Float?>()
    val latestPressure: LiveData<Float?> = _latestPressure

    private val _latestAirQuality = MutableLiveData<Float?>()
    val latestAirQuality: LiveData<Float?> = _latestAirQuality

    // LiveData from Room database for audit entries
    val auditEntries: LiveData<List<AuditEntry>> = auditDao.getAllAudits()

    private var auditTempSum = 0f
    private var auditHumiditySum = 0f
    private var auditPressureSum = 0f
    private var auditAirQualitySum = 0f
    private var auditSampleCount = 0

    private var auditTimerHandler: Handler? = null
    private var auditTimerRunnable: Runnable? = null

    fun addTemperatureEntry(time: Float, value: Float) {
        val list = _temperatureEntries.value ?: mutableListOf()
        list.add(Entry(time, value))
        _temperatureEntries.postValue(list)
        _latestTemperature.postValue(value)
    }

    fun addHumidityEntry(time: Float, value: Float) {
        val list = _humidityEntries.value ?: mutableListOf()
        list.add(Entry(time, value))
        _humidityEntries.postValue(list)
        _latestHumidity.postValue(value)
    }

    fun addPressureEntry(time: Float, value: Float) {
        val list = _pressureEntries.value ?: mutableListOf()
        list.add(Entry(time, value))
        _pressureEntries.postValue(list)
        _latestPressure.postValue(value)
    }

    fun addAirQualityEntry(time: Float, value: Float) {
        val list = _airQualityEntries.value ?: mutableListOf()
        list.add(Entry(time, value))
        _airQualityEntries.postValue(list)
        _latestAirQuality.postValue(value)
    }

    fun updateAirQuality(value: Float) {
        _latestAirQuality.postValue(value)
    }

    fun addAuditSample(temp: Float, humidity: Float, pressure: Float, airQuality: Float) {
        auditTempSum += temp
        auditHumiditySum += humidity
        auditPressureSum += pressure
        auditAirQualitySum += airQuality
        auditSampleCount++
    }

    fun finalizeAudit() {
        if (auditSampleCount == 0) return

        val avgTemp = auditTempSum / auditSampleCount
        val avgHumidity = auditHumiditySum / auditSampleCount
        val avgPressure = auditPressureSum / auditSampleCount
        val avgAirQuality = auditAirQualitySum / auditSampleCount

        val now = System.currentTimeMillis()
        val cutoffTime = now - (30 * 60 * 1000) // 30 minutes ago

        val auditEntry = AuditEntry(
            timestamp = now,
            avgTemperature = avgTemp,
            avgHumidity = avgHumidity,
            avgPressure = avgPressure,
            avgAirQuality = avgAirQuality
        )

        viewModelScope.launch(Dispatchers.IO) {
            auditDao.insertAudit(auditEntry)
            auditDao.deleteOlderThan(cutoffTime)
        }

        auditTempSum = 0f
        auditHumiditySum = 0f
        auditPressureSum = 0f
        auditAirQualitySum = 0f
        auditSampleCount = 0
    }

    fun startAuditTimer() {
        if (auditTimerHandler != null) return

        auditTimerHandler = Handler(Looper.getMainLooper())
        auditTimerRunnable = object : Runnable {
            override fun run() {
                finalizeAudit()
                auditTimerHandler?.postDelayed(this, 3 * 1000) // every 3 seconds
            }
        }
        auditTimerHandler?.postDelayed(auditTimerRunnable!!, 3 * 1000)
    }

    fun clearAllData() {
        _temperatureEntries.postValue(mutableListOf())
        _humidityEntries.postValue(mutableListOf())
        _pressureEntries.postValue(mutableListOf())
        _airQualityEntries.postValue(mutableListOf())
        _latestTemperature.postValue(null)
        _latestHumidity.postValue(null)
        _latestPressure.postValue(null)
        _latestAirQuality.postValue(null)
        // auditEntries come from DB, no need to clear here
    }
}
