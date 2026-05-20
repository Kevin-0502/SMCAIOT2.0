package com.example.smcaiot

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.models.EntityResponse
import com.example.smcaiot.network.AlertEntityAdapter
import com.example.smcaiot.ui.ErrorStateHelper
import com.example.smcaiot.ui.ErrorType
import kotlinx.coroutines.launch

class AlertsFragment : Fragment() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var layoutContent: View
    private lateinit var layoutErrorState: View
    private lateinit var adapter: AlertEntityAdapter

    private val viewModel: EntityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_alerts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvAlerts = view.findViewById(R.id.rvAlerts)
        progressBar = view.findViewById(R.id.progressBarAlerts)
        tvEmpty = view.findViewById(R.id.tvEmptyAlerts)
        layoutContent = view.findViewById(R.id.layoutContent)
        layoutErrorState = view.findViewById(R.id.layoutErrorState)

        adapter = AlertEntityAdapter { entity ->
            navigateToDetail(entity)
        }
        rvAlerts.adapter = adapter

        observeViewModel()
        viewModel.loadEntities()
    }

    private fun navigateToDetail(entity: EntityResponse) {
        val intent = Intent(requireContext(), EntityDetailActivity::class.java).apply {
            putExtra(EntityDetailActivity.EXTRA_ENTITY_ID, entity.id)
            putExtra(EntityDetailActivity.EXTRA_ENTITY_TYPE, entity.type)
            putExtra(EntityDetailActivity.EXTRA_ENTITY_COLOR, entity.color)
            putExtra(EntityDetailActivity.EXTRA_DEVICE_NAME, entity.deviceName)
            putExtra(EntityDetailActivity.EXTRA_ENTITY_LEVEL, entity.level)
        }
        startActivity(intent)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EntityUiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        layoutContent.visibility = View.VISIBLE
                        ErrorStateHelper.hide(layoutErrorState)
                    }
                    is EntityUiState.Success -> {
                        progressBar.visibility = View.GONE
                        layoutContent.visibility = View.VISIBLE
                        ErrorStateHelper.hide(layoutErrorState)
                    }
                    is EntityUiState.NoConnection -> {
                        progressBar.visibility = View.GONE
                        layoutContent.visibility = View.GONE
                        ErrorStateHelper.show(layoutErrorState, ErrorType.NO_CONNECTION) {
                            viewModel.refresh()
                        }
                    }
                    is EntityUiState.ApiError -> {
                        progressBar.visibility = View.GONE
                        layoutContent.visibility = View.GONE
                        ErrorStateHelper.show(
                            layoutErrorState,
                            ErrorType.API_ERROR,
                            errorCode = state.code
                        ) {
                            viewModel.refresh()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.entities.collect { entities ->
                val entitiesWithAlerts = entities.filter { entity ->
                    !entity.highestAlertName.isNullOrEmpty() ||
                    entity.variables.any { it.alert != null }
                }

                adapter.updateData(entitiesWithAlerts)

                if (entitiesWithAlerts.isEmpty() && viewModel.uiState.value is EntityUiState.Success) {
                    tvEmpty.visibility = View.VISIBLE
                    rvAlerts.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvAlerts.visibility = View.VISIBLE
                }
            }
        }
    }
}
