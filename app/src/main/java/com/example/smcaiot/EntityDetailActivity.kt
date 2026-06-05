package com.example.smcaiot

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
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
import com.example.smcaiot.network.SessionManager
import com.example.smcaiot.ui.ErrorStateHelper
import com.example.smcaiot.ui.ErrorType
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

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
    private val authToken: String
        get() = SessionManager.getToken() ?: ""

    // Parámetros dinámicos para la consulta de la API
    private var currentAmount: Int = 1
    private var currentUnit: String = "month"

    // Guardado estricto de límites en milisegundos para filtrado manual en Front
    private var customStartMillis: Long? = null
    private var customEndMillis: Long? = null

    // Control de estado del filtro dinámico
    private var isCustomRange: Boolean = false
    private var customChip: Chip? = null

    // Modo de visualización de datos activo
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

        val layoutFiltros: LinearLayout = findViewById(R.id.layoutFiltros)
        val btnToggleFiltros: MaterialButton = findViewById(R.id.btnToggleFiltros)

        btnToggleFiltros.setOnClickListener {
            if (layoutFiltros.visibility == View.VISIBLE) {
                layoutFiltros.visibility = View.GONE
                btnToggleFiltros.text = "Mostrar filtros"
                btnToggleFiltros.setIconResource(android.R.drawable.arrow_down_float)
            } else {
                layoutFiltros.visibility = View.VISIBLE
                btnToggleFiltros.text = "Ocultar filtros"
                btnToggleFiltros.setIconResource(android.R.drawable.arrow_up_float)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

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
                    isCustomRange = false
                    customStartMillis = null
                    customEndMillis = null
                    currentAmount = option.amount
                    currentUnit = option.unit
                    customChip?.text = "Personalizado"
                    loadHistoricalData()
                }
            }
            chipGroupPeriod.addView(chip)
        }

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
        if (supportFragmentManager.findFragmentByTag("date_range_picker") != null) return

        val config = resources.configuration
        val originalLocale = config.locales[0]
        Locale.setDefault(Locale("es"))
        config.setLocale(Locale("es"))
        resources.updateConfiguration(config, resources.displayMetrics)

        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccionar rango de fechas")
            .setTheme(R.style.Theme_MaterialCalendar_Personalizado)
            .build()

        config.setLocale(originalLocale)
        resources.updateConfiguration(config, resources.displayMetrics)

        picker.addOnPositiveButtonClickListener { selection ->
            val startMillis = selection.first
            val endMillis = selection.second

            if (startMillis != null && endMillis != null) {
                val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es")).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }

                // Guardamos los límites reales elegidos para limpiar el desastre de la API en el Front
                customStartMillis = startMillis
                // Le sumamos un día completo en milisegundos al límite final para abarcar las 23:59:59 de ese día
                customEndMillis = endMillis + TimeUnit.DAYS.toMillis(1) - 1

                // Calculamos el total de días desde "hoy" hasta la fecha de inicio
                val todayMillis = System.currentTimeMillis()
                val diffFromToday = todayMillis - startMillis
                var totalDaysBack = TimeUnit.MILLISECONDS.toDays(diffFromToday).toInt()

                // Margen de seguridad por si seleccionaron hoy o rangos muy cortos
                if (totalDaysBack <= 0) totalDaysBack = 1

                // Pedimos una ventana lo suficientemente grande para que cubra desde esa fecha vieja
                currentAmount = totalDaysBack + 2
                currentUnit = "day"
                isCustomRange = true

                val fromDisplay = displayFormat.format(Date(startMillis))
                val toDisplay = displayFormat.format(Date(endMillis))
                customChip?.text = "$fromDisplay - $toDisplay"

                loadHistoricalData()
            }
        }

        try {
            picker.show(supportFragmentManager, "date_range_picker")
        } catch (e: Exception) {
            Log.e("EntityDetail", "Error al mostrar DateRangePicker", e)
        }
    }

    private fun loadHistoricalData() {
        progressBar.visibility = View.VISIBLE
        layoutContent.visibility = View.VISIBLE
        ErrorStateHelper.hide(layoutErrorState)

        lifecycleScope.launch {
            try {
                Log.d("EntityDetail", "Pidiendo ventana extendida al backend: amount=$currentAmount, unit=$currentUnit")

                // Pedimos el total de días acumulados desde la fecha vieja hasta hoy sin enviarle dateTo
                val response = RetrofitClient.apiService.getHistoricalSensors(
                    entityId = entityId,
                    amount = currentAmount,
                    unit = currentUnit,
                    dateFrom = null,
                    dateTo = null,
                    authorization = authToken
                )

                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val body = response.body()

                    // Procesamos los ítems filtrando localmente en el Front
                    val processedItems = if (body != null) buildChartItems(body) else emptyList()

                    if (processedItems.isNotEmpty()) {
                        layoutContent.visibility = View.VISIBLE
                        ErrorStateHelper.hide(layoutErrorState)

                        // Buscamos la última lectura real válida dentro del rango filtrado
                        tvLastTimestamp.text = "Última lectura: ${body?.values?.firstOrNull()?.timestamp ?: ""}"
                        lastChartItems = processedItems

                        chartAdapter.updatePeriod(currentUnit, currentAmount, isCustomRange)

                        if (currentViewMode == VIEW_MODE_CHART) {
                            chartAdapter.updateData(lastChartItems)
                        } else {
                            tableAdapter.updateData(lastChartItems)
                        }
                    } else {
                        // Si después de limpiar el quilombo de la API no quedó nada en esas fechas, se muestra estado vacío
                        layoutContent.visibility = View.GONE
                        ErrorStateHelper.show(layoutErrorState, ErrorType.NO_DATA) {
                            showDateRangePicker()
                        }
                    }
                } else {
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

        // Formateador ISO para parsear las marcas de tiempo que devuelve la API
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        body.values?.asReversed()?.forEach { entry ->
            val timestampStr = entry.timestamp ?: return@forEach

            // FILTRADO MANUAL: Si hay rango personalizado activo, validamos la fecha aquí
            if (isCustomRange && customStartMillis != null && customEndMillis != null) {
                try {
                    // Quitamos milisegundos adicionales o la 'Z' si viene al final para evitar fallos de parseo
                    val cleanTimestamp = timestampStr.substringBefore(".")
                    val entryDate = isoFormat.parse(cleanTimestamp)
                    val entryMillis = entryDate?.time ?: 0L

                    // Si la fecha del registro NO cae en la selección del usuario, lo ignoramos por completo
                    if (entryMillis < customStartMillis!! || entryMillis > customEndMillis!!) {
                        return@forEach
                    }
                } catch (e: Exception) {
                    Log.e("EntityDetail", "Error al evaluar rango local para timestamp: $timestampStr", e)
                }
            }

            entry.value?.forEach { sensor ->
                val name = sensor.name ?: return@forEach
                val value = sensor.value ?: return@forEach
                pointsMap.getOrPut(name) { mutableListOf() }
                    .add(timestampStr to value)
                if (!metaMap.containsKey(name)) {
                    metaMap[name] = Meta(
                        unit = sensor.unit ?: "",
                        color = sensor.color ?: "#1565C0",
                        displayName = sensor.displayName
                    )
                }
            }
        }

        return pointsMap.map { (name, points) ->
            val meta = metaMap[name]
            val latestValue = points.lastOrNull()?.second ?: 0.0
            SensorChartItem(
                name = name,
                unit = meta?.unit ?: "",
                color = meta?.color ?: "#1565C0",
                displayName = meta?.displayName,
                latestValue = latestValue,
                entries = points
            )
        }
    }
}