package com.nuerovent.ui.audit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.nuerovent.databinding.ActivityAuditBinding
import com.nuerovent.model.StatsViewModel
import java.text.SimpleDateFormat
import java.util.*

class AuditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuditBinding
    private lateinit var statsViewModel: StatsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statsViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[StatsViewModel::class.java]

        binding.back.setOnClickListener {
            finish()
        }

        statsViewModel.auditEntries.observe(this) { entries ->
            binding.auditTable.removeAllViews()

            val headerRow = LayoutInflater.from(this).inflate(
                com.nuerovent.R.layout.item_audit_row,
                binding.auditTable,
                false
            ) as TableRow

            headerRow.findViewById<TextView>(com.nuerovent.R.id.timeText).text = "Time Range"
            headerRow.findViewById<TextView>(com.nuerovent.R.id.tempText).text = "Temp"
            headerRow.findViewById<TextView>(com.nuerovent.R.id.humText).text = "Humidity"
            headerRow.findViewById<TextView>(com.nuerovent.R.id.presText).text = "Pressure"
            headerRow.findViewById<TextView>(com.nuerovent.R.id.airText).text = "Air Quality"

            binding.auditTable.addView(headerRow)

            Log.d("AuditActivity", "Audit entries count: ${entries.size}")

            // Group entries by 5-minute windows (timeFrame is an index for each 5-min chunk)
            val grouped = entries.groupBy { it.timestamp / (5 * 60 * 1000) }

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

            // Sort the grouped keys so rows show in time order
            val sortedTimeFrames = grouped.keys.sorted()

            for (timeFrame in sortedTimeFrames) {
                val groupEntries = grouped[timeFrame] ?: continue

                // Calculate averages for this 5-minute group
                val avgTemp = groupEntries.map { it.avgTemperature }.average()
                val avgHum = groupEntries.map { it.avgHumidity }.average()
                val avgPres = groupEntries.map { it.avgPressure }.average()
                val avgAir = groupEntries.map { it.avgAirQuality }.average()

                val row = LayoutInflater.from(this).inflate(
                    com.nuerovent.R.layout.item_audit_row,
                    binding.auditTable,
                    false
                ) as TableRow

                // Calculate start and end time for the 5-minute window
                val startMillis = timeFrame * 5 * 60 * 1000
                val endMillis = startMillis + (5 * 60 * 1000)
                val timeRange = "${sdf.format(Date(startMillis))} - ${sdf.format(Date(endMillis))}"

                row.findViewById<TextView>(com.nuerovent.R.id.timeText).text = timeRange
                row.findViewById<TextView>(com.nuerovent.R.id.tempText).text = "%.1f°C".format(avgTemp)
                row.findViewById<TextView>(com.nuerovent.R.id.humText).text = "%.1f%%".format(avgHum)
                row.findViewById<TextView>(com.nuerovent.R.id.presText).text = "%.1f hPa".format(avgPres)
                row.findViewById<TextView>(com.nuerovent.R.id.airText).text = "%.1f AQI".format(avgAir)

                binding.auditTable.addView(row)
            }
        }

        statsViewModel.finalizeAudit()
    }
}
