package com.nuerovent.ui.dashboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nuerovent.databinding.FragmentStatsBinding
import com.nuerovent.model.StatsViewModel

class StatsFragment : Fragment() {

    private lateinit var binding: FragmentStatsBinding
    private val statsViewModel: StatsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentStatsBinding.inflate(inflater)
        initViews()
        observeData()
        return binding.root
    }

    private fun initViews() {
        setupChart(binding.lineChartTemp, emptyList(), "Temperature (°C)", Color.RED, 15f, 35f)
        setupChart(binding.lineChartHumidity, emptyList(), "Humidity (%)", Color.BLUE, 20f, 100f)
        setupChart(binding.lineChartPressure, emptyList(), "Pressure (hPa)", Color.MAGENTA, 970f, 1050f)
        setupChart(binding.lineChartAirQuality, emptyList(), "Air Quality Index", Color.GREEN, 0f, 200f) // Added AQ chart
    }

    private fun observeData() {
        statsViewModel.temperatureEntries.observe(viewLifecycleOwner) { entries ->
            updateChart(binding.lineChartTemp, entries)
        }

        statsViewModel.humidityEntries.observe(viewLifecycleOwner) { entries ->
            updateChart(binding.lineChartHumidity, entries)
        }

        statsViewModel.pressureEntries.observe(viewLifecycleOwner) { entries ->
            updateChart(binding.lineChartPressure, entries)
        }

        statsViewModel.airQualityEntries.observe(viewLifecycleOwner) { entries ->  // Observe air quality entries
            updateChart(binding.lineChartAirQuality, entries)
        }
    }

    private fun updateChart(chart: LineChart, entries: List<Entry>) {
        if (entries.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No data available")
            chart.invalidate()
            return
        }
        val dataSet = LineDataSet(entries, chart.description.text ?: "").apply {
            color = getColorForChart(chart)
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(color)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 50
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun getColorForChart(chart: LineChart): Int {
        return when (chart) {
            binding.lineChartTemp -> Color.RED
            binding.lineChartHumidity -> Color.BLUE
            binding.lineChartPressure -> Color.MAGENTA
            binding.lineChartAirQuality -> Color.GREEN // Added AQ color
            else -> Color.BLACK
        }
    }

    private fun setupChart(
        chart: LineChart,
        entries: List<Entry>,
        label: String,
        color: Int,
        yMin: Float,
        yMax: Float
    ) {
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(color)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 50
        }

        chart.data = LineData(dataSet)
        chart.setNoDataText("No data available")
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelCount = 6
        xAxis.setDrawLabels(true)
        xAxis.valueFormatter = TimeAxisFormatter()
        xAxis.setLabelCount(6, true)

        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = yMin
        leftAxis.axisMaximum = yMax

        chart.animateY(1000)
        chart.invalidate()
    }

    class TimeAxisFormatter : ValueFormatter() {
        @SuppressLint("DefaultLocale")
        override fun getFormattedValue(value: Float): String {
            val hour = value.toInt()
            return when (hour) {
                0, 5, 10, 15, 20, 24 -> String.format("%02d:00", hour)
                else -> ""
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = StatsFragment()
    }
}
