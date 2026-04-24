package com.example.smcaiot.network

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.smcaiot.R
import com.example.smcaiot.models.EntityResponse
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class EntityInfoWindowAdapter(
    private val inflater: LayoutInflater
) : GoogleMap.InfoWindowAdapter {

    /**
     * Retorna null para que Google Maps use getInfoContents() en su lugar,
     * manteniendo el frame/burbuja por defecto pero con contenido personalizado.
     */
    override fun getInfoWindow(marker: Marker): View? = null

    override fun getInfoContents(marker: Marker): View {
        val view = inflater.inflate(R.layout.custom_info_window, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvInfoTitle)
        val tvSnippet = view.findViewById<TextView>(R.id.tvInfoSnippet)
        val tvAction = view.findViewById<TextView>(R.id.tvInfoAction)

        val entity = marker.tag as? EntityResponse

        tvTitle.text = entity?.deviceName ?: entity?.id ?: marker.title ?: "Dispositivo"

        // Construir info del snippet
        val snippetText = buildString {
            entity?.let { e ->
                append("Tipo: ${e.type}")
                if (e.level > 0) {
                    append("\nNivel de alerta: ${e.level}")
                }
                e.highestAlertName?.let {
                    if (it.isNotEmpty()) append("\nAlerta: $it")
                }
            }
        }

        if (snippetText.isNotEmpty()) {
            tvSnippet.text = snippetText
            tvSnippet.visibility = View.VISIBLE
        } else {
            tvSnippet.visibility = View.GONE
        }

        // El botón "Ver detalles" siempre visible
        tvAction.visibility = View.VISIBLE

        return view
    }
}
