package com.nuerovent.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.nuerovent.adapter.AlertAdapter
import com.nuerovent.databinding.FragmentAlertsBinding
import com.nuerovent.model.Alert
import com.nuerovent.model.AlertViewModel


class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertAdapter: AlertAdapter
    private val alertViewModel: AlertViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeAlerts()
    }

    private fun setupRecyclerView() {
        alertAdapter = AlertAdapter(emptyList())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }
    }

    private fun observeAlerts() {
        alertViewModel.alerts.observe(viewLifecycleOwner, Observer { alerts ->
            alertAdapter.updateAlerts(alerts)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = AlertsFragment()
    }

}

