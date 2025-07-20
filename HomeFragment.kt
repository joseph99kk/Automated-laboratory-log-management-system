package com.nuerovent.ui.dashboard

import ItemAdapter
import com.nuerovent.model.StatsViewModel
import android.graphics.Rect
import android.os.Bundle
import com.nuerovent.model.AlertViewModel
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.nuerovent.R
import com.nuerovent.databinding.FragmentHomeBinding
import com.nuerovent.model.Alert
import com.nuerovent.model.HomeItem
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val client = OkHttpClient()

    private val statsViewModel: StatsViewModel by activityViewModels()
    private val alertViewModel: AlertViewModel by activityViewModels()

    private lateinit var itemViewAdapter: ItemAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var auditStarted = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        binding.initViews()
        return binding.root
    }

    private fun FragmentHomeBinding.initViews() {
        itemViewAdapter = ItemAdapter()
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = itemViewAdapter
        recyclerView.addItemDecoration(GridSpacingItemDecoration(2))

        itemViewAdapter.setDataList(
            listOf(
                HomeItem(R.string.temperature, "--°C", R.drawable.temp_image),
                HomeItem(R.string.humidity, "--%", R.drawable.ic_humidity),
                HomeItem(R.string.pressure, "-- hPa", R.drawable.ic_pressure),
                HomeItem(R.string.air_quality, "-- AQI", R.drawable.ic_air_quality)
            )
        )

        startUpdatingSensorData()
    }

    private fun startUpdatingSensorData() {
        handler.post(object : Runnable {
            override fun run() {
                fetchSensorData()
                handler.postDelayed(this, 3000)
            }
        })

        if (!auditStarted) {
            statsViewModel.startAuditTimer()
            auditStarted = true
        }
    }

    private fun fetchSensorData() {
        val request = Request.Builder()
            .url("http://192.168.4.1/data")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                updateWithError()
                handler.post {
                    if (isAdded) {
                        alertViewModel.clearAlerts()
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                try {
                    val json = JSONObject(responseData ?: "{}")
                    val temp = json.optDouble("temperature", -1.0)
                    val hum = json.optDouble("humidity", -1.0)
                    val pres = json.optDouble("pressure", -1.0)
                    val airQ = json.optDouble("airQuality", -1.0)

                    val temperatureStr = if (temp >= 0) "$temp°C" else "--°C"
                    val humidityStr = if (hum >= 0) "$hum%" else "--%"
                    val pressureStr = if (pres >= 0) "$pres hPa" else "-- hPa"
                    val airQualityStr = if (airQ >= 0) "$airQ AQI" else "-- AQI"

                    val alerts = mutableListOf<Alert>()

                    val timestamp = (System.currentTimeMillis() / 1000f) % 86400

                    if (temp >= 0) {
                        statsViewModel.addTemperatureEntry(timestamp, temp.toFloat())
                        if (temp > TEMP_HIGH) {
                            alerts.add(Alert(R.drawable.new_temp, "Temperature Too High",
                                "Current temperature: $temp°C exceeds $TEMP_HIGH°C", "Just now"))
                        } else if (temp < TEMP_LOW) {
                            alerts.add(Alert(R.drawable.new_temp, "Temperature Too Low",
                                "Current temperature: $temp°C below $TEMP_LOW°C", "Just now"))
                        }
                    }

                    if (hum >= 0) {
                        statsViewModel.addHumidityEntry(timestamp, hum.toFloat())
                        if (hum > HUMIDITY_HIGH) {
                            alerts.add(Alert(R.drawable.new_humidity, "Humidity Too High",
                                "Current humidity: $hum% exceeds $HUMIDITY_HIGH%", "Just now"))
                        } else if (hum < HUMIDITY_LOW) {
                            alerts.add(Alert(R.drawable.new_humidity, "Humidity Too Low",
                                "Current humidity: $hum% below $HUMIDITY_LOW%", "Just now"))
                        }
                    }

                    if (pres >= 0) {
                        statsViewModel.addPressureEntry(timestamp, pres.toFloat())
                        if (pres > PRESSURE_HIGH) {
                            alerts.add(Alert(R.drawable.new_pressure, "Pressure Too High",
                                "Current pressure: $pres hPa exceeds $PRESSURE_HIGH hPa", "Just now"))
                        } else if (pres < PRESSURE_LOW) {
                            alerts.add(Alert(R.drawable.new_pressure, "Pressure Too Low",
                                "Current pressure: $pres hPa below $PRESSURE_LOW hPa", "Just now"))
                        }
                    }

                    if (airQ >= 0) {
                        statsViewModel.addAirQualityEntry(timestamp, airQ.toFloat())

                        if (airQ > AIR_QUALITY_HIGH) {
                            alerts.add(Alert(R.drawable.new_humidity, "Poor Air Quality",
                                "Air Quality Index: $airQ exceeds $AIR_QUALITY_HIGH", "Just now"))
                        }
                    }

                    if (temp >= 0 && hum >= 0 && pres >= 0 && airQ >= 0) {
                        statsViewModel.addAuditSample(
                            temp.toFloat(),
                            hum.toFloat(),
                            pres.toFloat(),
                            airQ.toFloat()
                        )
                    }

                    handler.post {
                        if (isAdded) {
                            alertViewModel.clearAlerts()
                            for (alert in alerts) {
                                alertViewModel.addAlertWithTimeout(alert)
                            }

                            itemViewAdapter.setDataList(
                                listOf(
                                    HomeItem(R.string.temperature, temperatureStr, R.drawable.temp_image),
                                    HomeItem(R.string.humidity, humidityStr, R.drawable.ic_humidity),
                                    HomeItem(R.string.pressure, pressureStr, R.drawable.ic_pressure),
                                    HomeItem(R.string.air_quality, airQualityStr, R.drawable.ic_air_quality)
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    updateWithError()
                    handler.post {
                        if (isAdded) {
                            alertViewModel.clearAlerts()
                        }
                    }
                }
            }
        })
    }

    private fun updateWithError() {
        handler.post {
            if (isAdded) {
                itemViewAdapter.setDataList(
                    listOf(
                        HomeItem(R.string.temperature, "--°C", R.drawable.temp_image),
                        HomeItem(R.string.humidity, "--%", R.drawable.ic_humidity),
                        HomeItem(R.string.pressure, "-- hPa", R.drawable.ic_pressure),
                        HomeItem(R.string.air_quality, "-- AQI", R.drawable.ic_air_quality)
                    )
                )
            }
        }
    }

    class GridSpacingItemDecoration(private val spanCount: Int) :
        androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
        private val spacing = 40
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: androidx.recyclerview.widget.RecyclerView,
            state: androidx.recyclerview.widget.RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }

    companion object {
        private const val TEMP_HIGH = 30.0
        private const val TEMP_LOW = 15.0
        private const val HUMIDITY_HIGH = 70.0
        private const val HUMIDITY_LOW = 30.0
        private const val PRESSURE_HIGH = 1030.0
        private const val PRESSURE_LOW = 980.0
        private const val AIR_QUALITY_HIGH = 100.0

        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}
