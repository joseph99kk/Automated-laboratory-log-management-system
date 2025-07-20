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
        ControlItem("Control Mode", false, " Manual / Auto"),
        ControlItem("Extractor Fan", false, "Manual mode"),
        ControlItem("Humidifier", false, "Active"),
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
                "Extractor Fan" -> toggleFan(position, isChecked)
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
                    pushUpdatedConditionsToESP()  // Update ESP32 with new values
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

    private fun toggleControlMode(isAuto: Boolean) {
        val espIp = "192.168.4.1"
        val mode = if (isAuto) "auto" else "manual"
        val url = "http://$espIp/mode?type=$mode"
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

    private fun toggleFan(position: Int, turnOn: Boolean) {
        val espIp = "192.168.4.1"
        val endpoint = when (controlItems[position].label) {
            "Extractor Fan" -> "fan2"
            else -> return
        }

        val url = "http://$espIp/$endpoint?state=${if (turnOn) "on" else "off"}"
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

    companion object {
        @JvmStatic
        fun newInstance() = ControlFragment()
    }
}
