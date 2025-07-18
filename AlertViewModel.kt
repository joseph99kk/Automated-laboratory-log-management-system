package com.nuerovent.model

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AlertViewModel : ViewModel() {

    private val _alerts = MutableLiveData<List<Alert>>(emptyList())
    val alerts: LiveData<List<Alert>> = _alerts

    private val handler = Handler(Looper.getMainLooper())

    fun addAlertWithTimeout(alert: Alert, timeout: Long = 10_000L) {
        val currentList = _alerts.value ?: emptyList()
        _alerts.value = currentList + alert

        handler.postDelayed({
            removeAlert(alert)
        }, timeout)
    }

    private fun removeAlert(alert: Alert) {
        val currentList = _alerts.value ?: return
        _alerts.value = currentList.filterNot { it == alert }
    }

    fun clearAlerts() {
        _alerts.value = emptyList()
    }
}
