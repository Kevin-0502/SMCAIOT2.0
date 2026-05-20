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
import com.example.smcaiot.network.SensorTableAdapter
import com.example.smcaiot.ui.ErrorStateHelper
import com.example.smcaiot.ui.ErrorType
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EntityDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTITY_ID = "extra_entity_id"
        const val EXTRA_ENTITY_TYPE = "extra_entity_type"
        const val EXTRA_ENTITY_COLOR = "extra_entity_color"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_ENTITY_LEVEL = "extra_entity_level"
        const val VIEW_MODE_CHART = "chart"
        const val VIEW_MODE_TABLE = "table"
    }

    private lateinit var rvSensorReadings: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutContent: View
    private lateinit var layoutErrorState: View
    private lateinit var tvLastTimestamp: TextView
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chipGroupViewMode: ChipGroup
    private lateinit var chartAdapter: SensorChartAdapter
    private lateinit var tableAdapter: SensorTableAdapter

    private var entityId: String = ""
    private val authToken = "Cambiar token de inicio de sesion"

    // Periodo predefinido (siempre tiene valores válidos)
    private var currentAmount: Int = 1
    private var currentUnit: String = "month"

    // Rango personalizado de fechas
    private var isCustomRange: Boolean = false
    private var customDateFrom: String? = null
    private var customDateTo: String? = null
    private var customChip: Chip? = null

    // Modo de vista actual
    private var currentViewMode: String = VIEW_MODE_CHART
    private var lastChartItems: List<SensorChartItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entity_detail)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        entityId = intent.getStringExtra(EXTRA_ENTITY_ID) ?: ""
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
        layoutContent = findViewById(R.id.layoutContent)
        layoutErrorState = findViewById(R.id.layoutErrorState)
        chipGroupPeriod = findViewById(R.id.chipGroupPeriod)
        chipGroupViewMode = findViewById(R.id.chipGroupViewMode)

        chartAdapter = SensorChartAdapter()
        tableAdapter = SensorTableAdapter()
        applyViewMode()

        setupViewModeChips()
        setupPeriodChips()
        loadHistoricalData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // ── Vista: Gráficas / Tabla ──────────────────────────────────────

    private fun setupViewModeChips() {
        data class ViewModeOption(val label: String, val mode: String)
        val options = listOf(
            ViewModeOption("Gráficas", VIEW_MODE_CHART),
            ViewModeOption("Tabla", VIEW_MODE_TABLE)
        )

        chipGroupViewMode.removeAllViews()

        options.forEach { option ->
            val chip = Chip(this).apply {
                text = option.label
                isCheckable = true
                isCheckedIconVisible = false
                isChecked = (option.mode == currentViewMode)
                setOnClickListener {
                    if (option.mode != currentViewMode) {
                        currentViewMode = option.mode
                        applyViewMode()
                        if (lastChartItems.isNotEmpty()) {
                            if (currentViewMode == VIEW_MODE_CHART) {
                                chartAdapter.updateData(lastChartItems)
                            } else {
                                tableAdapter.updateData(lastChartItems)
                            }
                        }
                    }
                }
            }
            chipGroupViewMode.addView(chip)
        }

        chipGroupViewMode.isSingleSelection = true
    }

    private fun applyViewMode() {
        rvSensorReadings.adapter = if (currentViewMode == VIEW_MODE_CHART) {
            chartAdapter
        } else {
            tableAdapter
        }
    }

    // ── Periodo: chips predefinidos + personalizado ──────────────────

    private fun setupPeriodChips() {
        data class PeriodOption(val label: String, val amount: Int, val unit: String)
        val presetOptions = listOf(
            PeriodOption("24 horas", 24, "hour"),
            PeriodOption("1 semana", 1, "week"),
            PeriodOption("1 mes", 1, "month"),
            PeriodOption("3 meses", 3, "month")
        )

        chipGroupPeriod.removeAllViews()

        presetOptions.forEach { option ->
            val chip = Chip(this).apply {
                text = option.label
                isCheckable = true
                isCheckedIconVisible = false
                isChecked = (option.amount == currentAmount && option.unit == currentUnit && !isCustomRange)
                setOnClickListener {
                    // Cambiar a periodo predefinido
                    isCustomRange = false
                    customDateFrom = null
                    customDateTo = null
                    currentAmount = option.amount
                    currentUnit = option.unit
                    customChip?.text = "Personalizado"
                    loadHistoricalData()
                }
            }
            chipGroupPeriod.addView(chip)
        }

        // Chip "Personalizado" que abre el DateRangePicker
        customChip = Chip(this).apply {
            text = "Personalizado"
            isCheckable = true
            isCheckedIconVisible = false
            isChecked = false
            setOnClickListener {
                showDateRangePicker()
            }
        }
        chipGroupPeriod.addView(customChip)

        chipGroupPeriod.isSingleSelection = true
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccionar rango de fechas")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startMillis = selection.first
            val endMillis = selection.second

            if (startMillis != null && endMillis != null) {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")

                val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                customDateFrom = isoFormat.format(Date(startMillis))
                // Fin del día para incluir todo el último día seleccionado
                customDateTo = isoFormat.format(Date(endMillis + 86_399_999L))

                // Activar modo rango personalizado
                isCustomRange = true

                // Actualizar texto del chip con las fechas seleccionadas
                val fromDisplay = displayFormat.format(Date(startMillis))
                val toDisplay = displayFormat.format(Date(endMillis))
                customChip?.text = "$fromDisplay - $toDisplay"

                loadHistoricalData()
            }
        }

        picker.show(supportFragmentManager, "date_range_picker")
    }

    // ── Carga de datos ──────────────────────────────────────────────

    private fun loadHistoricalData() {
        progressBar.visibility = View.VISIBLE
        layoutContent.visibility = View.VISIBLE
        ErrorStateHelper.hide(layoutErrorState)

        lifecycleScope.launch {
            try {
                val response = if (isCustomRange && customDateFrom != null && customDateTo != null) {
                    RetrofitClient.apiService.getHistoricalSensors(
                        entityId = entityId,
                        amount = null,
                        unit = null,
                        dateFrom = customDateFrom,
                        dateTo = customDateTo,
                        authorization = authToken
                    )
                } else {
                    RetrofitClient.apiService.getHistoricalSensors(
                        entityId = entityId,
                        amount = currentAmount,
                        unit = currentUnit,
                        authorization = authToken
                    )
                }

                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && !body.values.isNullOrEmpty()) {
                        layoutContent.visibility = View.VISIBLE
                        ErrorStateHelper.hide(layoutErrorState)
                        tvLastTimestamp.text = "Última lectura: ${body.values.first().timestamp ?: ""}"
                        lastChartItems = buildChartItems(body)
                        if (currentViewMode == VIEW_MODE_CHART) {
                            chartAdapter.updateData(lastChartItems)
                        } else {
                            tableAdapter.updateData(lastChartItems)
                        }
                    } else {
                        // Sin datos para el rango seleccionado
                        layoutContent.visibility = View.GONE
                        ErrorStateHelper.show(layoutErrorState, ErrorType.NO_DATA) {
                            showDateRangePicker()
                        }
                    }
                } else {
                    // Error de la API (400, 500, etc.)
                    layoutContent.visibility = View.GONE
                    ErrorStateHelper.show(
                        layoutErrorState,
                        ErrorType.API_ERROR,
                        errorCode = response.code()
                    ) {
                        loadHistoricalData()
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE

                val isConnectionError = e is UnknownHostException ||
                        e is ConnectException ||
                        e is SocketTimeoutException ||
                        e.cause is UnknownHostException ||
                        e.cause is ConnectException ||
                        e.cause is SocketTimeoutException

                layoutContent.visibility = View.GONE
                if (isConnectionError) {
                    ErrorStateHelper.show(layoutErrorState, ErrorType.NO_CONNECTION) {
                        loadHistoricalData()
                    }
                } else {
                    ErrorStateHelper.show(
                        layoutErrorState,
                        ErrorType.API_ERROR
                    ) {
                        loadHistoricalData()
                    }
                }
            }
        }
    }

    private fun buildChartItems(body: HistoricalResponse): List<SensorChartItem> {
        val pointsMap = linkedMapOf<String, MutableList<Pair<String, Double>>>()
        data class Meta(val unit: String, val color: String, val displayName: String?)
        val metaMap = mutableMapOf<String, Meta>()

        body.values?.asReversed()?.forEach { entry ->
            val timestamp = entry.timestamp ?: return@forEach
            entry.value?.forEach { sensor ->
                val name = sensor.name ?: return@forEach
                val value = sensor.value ?: return@forEach
                pointsMap.getOrPut(name) { mutableListOf() }
                    .add(timestamp to value)
                if (!metaMap.containsKey(name)) {
                    metaMap[name] = Meta(
                        unit = sensor.unit ?: "",
                        color = sensor.color ?: "#1565C0",
                        displayName = sensor.displayName
                    )
                }
            }
        }

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

}
