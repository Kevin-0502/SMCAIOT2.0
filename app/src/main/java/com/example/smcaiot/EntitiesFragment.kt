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
import com.example.smcaiot.network.EntityAdapter
import kotlinx.coroutines.launch

class EntitiesFragment : Fragment() {

    private lateinit var rvEntities: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var adapter: EntityAdapter

    private val viewModel: EntityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_entities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvEntities = view.findViewById(R.id.rvEntities)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        adapter = EntityAdapter { entity ->
            val intent = Intent(requireContext(), EntityDetailActivity::class.java).apply {
                putExtra(EntityDetailActivity.EXTRA_ENTITY_ID, entity.id)
                putExtra(EntityDetailActivity.EXTRA_ENTITY_TYPE, entity.type)
                putExtra(EntityDetailActivity.EXTRA_ENTITY_COLOR, entity.color)
                putExtra(EntityDetailActivity.EXTRA_DEVICE_NAME, entity.deviceName)
                putExtra(EntityDetailActivity.EXTRA_ENTITY_LEVEL, entity.level)
            }
            startActivity(intent)
        }
        rvEntities.adapter = adapter

        observeViewModel()

        // Cargar entidades (si ya se cargaron, no repite la petición)
        viewModel.loadEntities()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EntityUiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        tvError.visibility = View.GONE
                    }
                    is EntityUiState.Success -> {
                        progressBar.visibility = View.GONE
                        tvError.visibility = View.GONE
                    }
                    is EntityUiState.Error -> {
                        progressBar.visibility = View.GONE
                        tvError.text = state.message
                        tvError.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.entities.collect { entities ->
                adapter.updateData(entities)
            }
        }
    }
}
