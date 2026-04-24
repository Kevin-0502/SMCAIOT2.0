package com.example.smcaiot

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.models.HistoricalResponse
import com.example.smcaiot.models.SensorChartItem
import com.example.smcaiot.network.RetrofitClient
import com.example.smcaiot.network.SensorChartAdapter
import kotlinx.coroutines.launch

class EntityDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTITY_ID = "extra_entity_id"
        const val EXTRA_ENTITY_TYPE = "extra_entity_type"
        const val EXTRA_ENTITY_COLOR = "extra_entity_color"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_ENTITY_LEVEL = "extra_entity_level"
    }

    private lateinit var rvSensorReadings: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvLastTimestamp: TextView
    private lateinit var adapter: SensorChartAdapter

    private val authToken = "Cambiar token de inicio de sesion"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entity_detail)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val entityId = intent.getStringExtra(EXTRA_ENTITY_ID) ?: ""
        val entityType = intent.getStringExtra(EXTRA_ENTITY_TYPE) ?: ""
        val entityColor = intent.getStringExtra(EXTRA_ENTITY_COLOR) ?: "#CCCCCC"
        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME)

        supportActionBar?.title = deviceName?.takeIf { it.isNotEmpty() } ?: entityId

        val viewColorIndicator: View = findViewById(R.id.viewColorIndicator)
        val tvEntityId: TextView = findViewById(R.id.tvEntityId)
        val tvEntityType: TextView = findViewById(R.id.tvEntityType)
        val tvDeviceName: TextView = findViewById(R.id.tvDeviceName)
        tvLastTimestamp = findViewById(R.id.tvLastTimestamp)

        try {
            viewColorIndicator.setBackgroundColor(Color.parseColor(entityColor))
        } catch (e: Exception) {
            viewColorIndicator.setBackgroundColor(Color.LTGRAY)
        }
        tvEntityId.text = entityId
        tvEntityType.text = "Tipo: $entityType"
        if (!deviceName.isNullOrEmpty()) {
            tvDeviceName.text = "Dispositivo: $deviceName"
            tvDeviceName.visibility = View.VISIBLE
        }

        rvSensorReadings = findViewById(R.id.rvSensorReadings)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        adapter = SensorChartAdapter()
        rvSensorReadings.adapter = adapter

        loadHistoricalData(entityId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadHistoricalData(entityId: String) {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getHistoricalSensors(
                    entityId = entityId,
                    authorization = authToken
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && !body.values.isNullOrEmpty()) {
                        tvLastTimestamp.text = "Última lectura: ${body.values.first().timestamp ?: ""}"
                        adapter.updateData(buildChartItems(body))
                    } else {
                        showError("No hay datos históricos disponibles")
                    }
                } else {
                    showError("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Agrupa todos los puntos del historial por nombre de sensor.
     * La API devuelve las entradas del más reciente al más antiguo, así que
     * se invierten para que el gráfico muestre el tiempo de izquierda a derecha.
     */
    private fun buildChartItems(body: HistoricalResponse): List<SensorChartItem> {
        // Mapa: nombre del sensor → lista ordenada (antiguo→reciente) de (timestamp, valor)
        val pointsMap = linkedMapOf<String, MutableList<Pair<String, Double>>>()
        // Mapa auxiliar para guardar metadatos del sensor (usa el primer entry = más reciente)
        data class Meta(val unit: String, val color: String, val displayName: String?)
        val metaMap = mutableMapOf<String, Meta>()

        // Recorremos de atrás hacia adelante para invertir el orden (antiguo→reciente)
        body.values?.asReversed()?.forEach { entry ->
            val timestamp = entry.timestamp ?: return@forEach
            entry.value?.forEach { sensor ->
                val name = sensor.name ?: return@forEach
                val value = sensor.value ?: return@forEach
                pointsMap.getOrPut(name) { mutableListOf() }
                    .add(timestamp to value)
                // Solo guardamos metadatos una vez (del primer registro que encontremos)
                if (!metaMap.containsKey(name)) {
                    metaMap[name] = Meta(
                        unit = sensor.unit ?: "",
                        color = sensor.color ?: "#1565C0",
                        displayName = sensor.displayName
                    )
                }
            }
        }

        // El valor más reciente está en values[0]
        val latestEntry = body.values?.firstOrNull()

        return pointsMap.map { (name, points) ->
            val meta = metaMap[name]
            val latestValue = latestEntry?.value
                ?.firstOrNull { it.name == name }?.value ?: points.lastOrNull()?.second ?: 0.0
            SensorChartItem(
                name = name,
                unit = meta?.unit ?: "",
                color = meta?.color ?: "#1565C0",
                displayName = latestEntry?.value?.firstOrNull { it.name == name }?.displayName,
                latestValue = latestValue,
                entries = points
            )
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}
