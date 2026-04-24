package com.example.smcaiot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.smcaiot.models.EntityResponse
import com.example.smcaiot.network.EntityInfoWindowAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MapsFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MapsFragment"
        private val UDB_LOCATION = LatLng(13.7159, -89.1556)
        private const val DEFAULT_ZOOM = 15f
    }

    private var googleMap: GoogleMap? = null
    private var markersPlaced = false

    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private val viewModel: EntityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBarMap)
        tvError = view.findViewById(R.id.tvMapError)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        observeViewModel()
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d(TAG, "onMapReady")
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(UDB_LOCATION, DEFAULT_ZOOM))

        // InfoWindow personalizado con botón "Ver detalles"
        map.setInfoWindowAdapter(EntityInfoWindowAdapter(layoutInflater))

        // Al tocar el InfoWindow, navegar al detalle
        map.setOnInfoWindowClickListener { marker ->
            val entity = marker.tag as? EntityResponse ?: return@setOnInfoWindowClickListener
            val intent = Intent(requireContext(), EntityDetailActivity::class.java).apply {
                putExtra(EntityDetailActivity.EXTRA_ENTITY_ID, entity.id)
                putExtra(EntityDetailActivity.EXTRA_ENTITY_TYPE, entity.type)
                putExtra(EntityDetailActivity.EXTRA_ENTITY_COLOR, entity.color)
                putExtra(EntityDetailActivity.EXTRA_DEVICE_NAME, entity.deviceName)
                putExtra(EntityDetailActivity.EXTRA_ENTITY_LEVEL, entity.level)
            }
            startActivity(intent)
        }

        // Si ya hay entidades cargadas, colocar marcadores
        val currentEntities = viewModel.entities.value
        if (currentEntities.isNotEmpty()) {
            placeMarkers(currentEntities)
        }

        // Disparar carga si aún no se ha hecho
        viewModel.loadEntities()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && !markersPlaced) {
            val entities = viewModel.entities.value
            if (entities.isNotEmpty() && googleMap != null) {
                placeMarkers(entities)
            }
        }
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
                if (entities.isNotEmpty() && googleMap != null) {
                    placeMarkers(entities)
                }
            }
        }
    }

    private fun placeMarkers(entities: List<EntityResponse>) {
        val map = googleMap ?: return

        map.clear()
        markersPlaced = false

        val boundsBuilder = LatLngBounds.Builder()
        var validCount = 0

        for (entity in entities) {
            val coordinates = entity.location?.value?.coordinates
            if (coordinates == null || coordinates.size < 2) continue

            val lat = coordinates[0]
            val lng = coordinates[1]

            if (lat == 0.0 && lng == 0.0) continue

            val position = LatLng(lat, lng)
            validCount++
            boundsBuilder.include(position)

            val markerColor = getMarkerHue(entity.color)

            val snippet = buildString {
                entity.deviceName?.let { append("Dispositivo: $it") }
                entity.address?.let {
                    if (isNotEmpty()) append("\n")
                    append("Dirección: $it")
                }
                if (entity.level > 0) {
                    if (isNotEmpty()) append("\n")
                    append("Nivel de alerta: ${entity.level}")
                }
                entity.highestAlertName?.let {
                    if (it.isNotEmpty()) {
                        if (isNotEmpty()) append("\n")
                        append("Alerta: $it")
                    }
                }
            }

            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(entity.deviceName ?: entity.id)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
            marker?.tag = entity
        }

        Log.d(TAG, "Marcadores colocados: $validCount de ${entities.size}")

        markersPlaced = validCount > 0
    }

    private fun getMarkerHue(hexColor: String): Float {
        return try {
            val color = Color.parseColor(hexColor)
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[0]
        } catch (e: Exception) {
            BitmapDescriptorFactory.HUE_RED
        }
    }
}
