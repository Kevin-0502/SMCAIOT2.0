package com.example.smcaiot.network

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.R
import com.example.smcaiot.models.SensorChartItem
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SensorChartAdapter(
    private var items: List<SensorChartItem> = emptyList(),
    private var periodUnit: String = "month",
    private var periodAmount: Int = 1
) : RecyclerView.Adapter<SensorChartAdapter.ChartViewHolder>() {

    companion object {
        /** Formatos ISO que puede devolver la API */
        private val isoFormats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        )

        fun parseTimestamp(raw: String): Long? {
            for (fmt in isoFormats) {
                try {
                    return fmt.parse(raw)?.time
                } catch (_: Exception) { }
            }
            return null
        }
    }

    fun updateData(newItems: List<SensorChartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updatePeriod(unit: String, amount: Int) {
        periodUnit = unit
        periodAmount = amount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_chart, parent, false)
        return ChartViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        holder.bind(items[position], periodUnit, periodAmount)
    }

    override fun getItemCount(): Int = items.size

    class ChartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewSensorColor: View = itemView.findViewById(R.id.viewSensorColor)
        private val tvSensorName: TextView = itemView.findViewById(R.id.tvSensorName)
        private val tvDisplayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        private val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        private val lineChart: LineChart = itemView.findViewById(R.id.lineChart)

        fun bind(item: SensorChartItem, periodUnit: String, periodAmount: Int) {
            tvSensorName.text = item.name
            tvCurrentValue.text = buildString {
                val v = item.latestValue
                append(if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString())
                if (item.unit.isNotEmpty()) append(" ${item.unit}")
            }

            if (!item.displayName.isNullOrEmpty()) {
                tvDisplayName.text = item.displayName
                tvDisplayName.visibility = View.VISIBLE
            } else {
                tvDisplayName.visibility = View.GONE
            }

            val lineColor = try {
                Color.parseColor(item.color)
            } catch (e: Exception) {
                Color.parseColor("#1565C0")
            }

            try {
                viewSensorColor.setBackgroundColor(lineColor)
            } catch (e: Exception) {
                viewSensorColor.setBackgroundColor(Color.LTGRAY)
            }

            setupChart(item, lineColor, periodUnit, periodAmount)
        }

        private fun setupChart(item: SensorChartItem, lineColor: Int, periodUnit: String, periodAmount: Int) {
            // Construir las entradas usando el timestamp real como valor X (millis)
            val timestamps = mutableListOf<String>() // guardar timestamps originales para el formatter
            val chartEntries = mutableListOf<Entry>()

            item.entries.forEachIndexed { index, (timestamp, value) ->
                timestamps.add(timestamp)
                chartEntries.add(Entry(index.toFloat(), value.toFloat()))
            }

            val dataSet = LineDataSet(chartEntries, item.name).apply {
                color = lineColor
                setCircleColor(lineColor)
                circleRadius = 2f
                lineWidth = 2f
                setDrawValues(false)
                setDrawCircles(item.entries.size <= 30)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                fillColor = lineColor
                fillAlpha = 30
                setDrawFilled(true)
            }

            // Determinar formato de fecha según el periodo
            val displayFormat = getDisplayFormat(periodUnit, periodAmount)
            val formatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index < 0 || index >= timestamps.size) return ""
                    val raw = timestamps[index]
                    return formatForAxis(raw, displayFormat)
                }
            }

            // Calcular cuántas etiquetas mostrar (máx ~5-6 para que no se empalmen)
            val labelCount = when {
                timestamps.size <= 6 -> timestamps.size
                timestamps.size <= 12 -> 6
                else -> 5
            }

            lineChart.apply {
                data = LineData(dataSet)
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setNoDataText("Sin datos")

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawLabels(true)
                    valueFormatter = formatter
                    granularity = 1f
                    setLabelCount(labelCount, true)
                    textColor = Color.parseColor("#757575")
                    textSize = 9f
                    labelRotationAngle = -30f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#EEEEEE")
                    textColor = Color.parseColor("#757575")
                    textSize = 10f
                }

                axisRight.isEnabled = false

                // Dar espacio extra abajo para las etiquetas rotadas
                setExtraOffsets(4f, 4f, 4f, 12f)

                animateX(600)
                invalidate()
            }
        }

        /**
         * Elige el patrón de formato según el filtro de periodo:
         * - 24h        → "HH:mm"         (solo hora)
         * - 1 semana   → "EEE dd"        (día de semana + número)
         * - 1 mes      → "dd MMM"        (día + mes abreviado)
         * - 3+ meses   → "dd/MM/yy"      (fecha corta)
         * - custom     → "dd/MM/yy"
         */
        private fun getDisplayFormat(unit: String, amount: Int): SimpleDateFormat {
            val pattern = when (unit) {
                "hour" -> "HH:mm"
                "week" -> "EEE dd"
                "month" -> if (amount <= 1) "dd MMM" else "dd/MM/yy"
                else -> "dd/MM/yy"
            }
            return SimpleDateFormat(pattern, Locale("es"))
        }

        private fun formatForAxis(raw: String, displayFormat: SimpleDateFormat): String {
            for (fmt in isoFormats) {
                try {
                    val date = fmt.parse(raw) ?: continue
                    return displayFormat.format(date)
                } catch (_: Exception) { }
            }
            return ""
        }
    }
}
