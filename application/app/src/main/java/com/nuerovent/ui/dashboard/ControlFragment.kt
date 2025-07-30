package com.nuerovent.ui.dashboard

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nuerovent.adapter.ControlAdapter
import com.nuerovent.adapter.DefaultConditionAdapter
import com.nuerovent.databinding.FragmentControlBinding
import com.nuerovent.model.ControlItem
import com.nuerovent.model.DefaultConditionItem
import okhttp3.*
import java.io.IOException

class ControlFragment : Fragment() {

    private lateinit var binding: FragmentControlBinding
    private lateinit var adapter: ControlAdapter
    private val client = OkHttpClient()

    private val controlItems = mutableListOf(
        ControlItem("Control Mode", false, " Auto / Manual"),
        ControlItem("Radiator Fan", false, "Manual mode"),
        ControlItem("Heating Unit", false, "Scheduled"),
        ControlItem("Cooling Unit", false, "Idle")
    )

    private val defaultConditions = mutableListOf(
        DefaultConditionItem("Target Temperature", "22°C"),
        DefaultConditionItem("Target Humidity", "50%"),
        DefaultConditionItem("Target Pressure", "1013hPa"),
        DefaultConditionItem("Air Quality Threshold", "Low")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentControlBinding.inflate(inflater, container, false)
        setupRecyclerViews()
        return binding.root
    }

    private fun setupRecyclerViews() {
        adapter = ControlAdapter(controlItems) { position, isChecked ->
            controlItems[position].isChecked = isChecked
            when (controlItems[position].label) {
                "Control Mode" -> toggleControlMode(isChecked)
                "Radiator Fan" -> if (controlItems[0].isChecked) toggleFan(isChecked) else resetToggle(position)
                "Heating Unit" -> if (controlItems[0].isChecked) toggleHeating(isChecked) else resetToggle(position)
                "Cooling Unit" -> if (controlItems[0].isChecked) toggleCooling(isChecked) else resetToggle(position)
            }
        }

        binding.controlRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.controlRecyclerView.adapter = adapter

        val defaultAdapter = DefaultConditionAdapter(defaultConditions) { position, action ->
            when (action) {
                "set_temp" -> showInputDialog(position, "Set Temperature", "°C")
                "set_humidity" -> showInputDialog(position, "Set Humidity", "%")
                "set_pressure" -> showInputDialog(position, "Set Pressure", "hPa")
            }
        }

        binding.defaultConditionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.defaultConditionRecyclerView.adapter = defaultAdapter
    }

    private fun resetToggle(position: Int) {
        requireActivity().runOnUiThread {
            // If manual mode is off, prevent toggling individual devices by resetting UI switch to false
            controlItems[position].isChecked = false
            adapter.notifyItemChanged(position)
        }
    }

    private fun showInputDialog(position: Int, title: String, unit: String) {
        val context = requireContext()
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter value"
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newValue = input.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    defaultConditions[position] = DefaultConditionItem(
                        defaultConditions[position].title,
                        "$newValue$unit"
                    )
                    binding.defaultConditionRecyclerView.adapter?.notifyItemChanged(position)
                    pushUpdatedConditionsToESP()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun pushUpdatedConditionsToESP() {
        val espIp = "192.168.4.1"

        val temp = defaultConditions[0].subtext.filter { it.isDigit() || it == '.' }
        val hum = defaultConditions[1].subtext.filter { it.isDigit() || it == '.' }
        val pres = defaultConditions[2].subtext.filter { it.isDigit() || it == '.' }

        val url = "http://$espIp/set_conditions?temp=$temp&humidity=$hum&pressure=$pres"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    private fun toggleControlMode(isManual: Boolean) {
        val espIp = "192.168.4.1"
        val mode = if (isManual) "manual" else "auto"
        val url = "http://$espIp/mode?type=$mode"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                requireActivity().runOnUiThread {
                    if (isManual) {
                        // Turn OFF all devices on manual mode activation
                        turnOffAllDevices()
                    } else {
                        // Reset all device toggles off when auto mode is enabled
                        resetAllDeviceToggles()
                    }
                }
            }
        })
    }

    private fun turnOffAllDevices() {
        val espIp = "192.168.4.1"
        val urls = listOf(
            "http://$espIp/fan2?state=off",
            "http://$espIp/heater?state=off",
            "http://$espIp/cooler?state=off"
        )
        for (url in urls) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = e.printStackTrace()
                override fun onResponse(call: Call, response: Response) = response.close()
            })
        }
        // Reset UI toggles for devices to off on UI thread
        requireActivity().runOnUiThread {
            resetAllDeviceToggles()
        }
    }

    private fun resetAllDeviceToggles() {
        controlItems[1].isChecked = false // Extractor Fan
        controlItems[2].isChecked = false // Heating Unit
        controlItems[3].isChecked = false // Cooling Unit
        adapter.notifyItemRangeChanged(1, 3)
    }

    private fun toggleFan(turnOn: Boolean) {
        val espIp = "192.168.4.1"
        val url = "http://$espIp/fan2?state=${if (turnOn) "on" else "off"}"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = e.printStackTrace()
            override fun onResponse(call: Call, response: Response) = response.close()
        })
    }

    private fun toggleHeating(turnOn: Boolean) {
        val espIp = "192.168.4.1"
        val url = "http://$espIp/heater?state=${if (turnOn) "on" else "off"}"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = e.printStackTrace()
            override fun onResponse(call: Call, response: Response) = response.close()
        })
    }

    private fun toggleCooling(turnOn: Boolean) {
        val espIp = "192.168.4.1"
        val url = "http://$espIp/cooler?state=${if (turnOn) "on" else "off"}"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = e.printStackTrace()
            override fun onResponse(call: Call, response: Response) = response.close()
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = ControlFragment()
    }
}
