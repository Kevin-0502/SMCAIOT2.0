package com.example.smcaiot.network

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.R
import com.example.smcaiot.models.SensorChartItem
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SensorChartAdapter(
    private var items: List<SensorChartItem> = emptyList(),
    private var periodUnit: String = "month",
    private var periodAmount: Int = 1,
    private var isCustomRange: Boolean = false
) : RecyclerView.Adapter<SensorChartAdapter.ChartViewHolder>() {

    companion object {
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

    fun updatePeriod(unit: String, amount: Int, customRange: Boolean = false) {
        periodUnit = unit
        periodAmount = amount
        isCustomRange = customRange
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_chart, parent, false)
        return ChartViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        holder.bind(items[position], periodUnit, periodAmount, isCustomRange)
    }

    override fun getItemCount(): Int = items.size

    class ChartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewSensorColor: View = itemView.findViewById(R.id.viewSensorColor)
        private val tvSensorName: TextView = itemView.findViewById(R.id.tvSensorName)
        private val tvDisplayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        private val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        private val lineChart: LineChart = itemView.findViewById(R.id.lineChart)

        fun bind(item: SensorChartItem, periodUnit: String, periodAmount: Int, isCustomRange: Boolean) {
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

            setupChart(item, lineColor, periodUnit, periodAmount, isCustomRange)
        }

        private fun setupChart(item: SensorChartItem, lineColor: Int, periodUnit: String, periodAmount: Int, isCustomRange: Boolean) {
            val timestamps = mutableListOf<String>()
            val chartEntries = mutableListOf<Entry>()

            item.entries.forEachIndexed { index, (timestamp, value) ->
                timestamps.add(timestamp)
                chartEntries.add(Entry(index.toFloat(), value.toFloat()))
            }

            val dataSet = LineDataSet(chartEntries, item.name).apply {
                color = lineColor
                setCircleColor(lineColor)
                circleRadius = 3f
                lineWidth = 2f
                setDrawValues(false)
                setDrawCircles(item.entries.size <= 30)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                fillColor = lineColor
                fillAlpha = 30
                setDrawFilled(true)

                highlightLineWidth = 1.2f
                highLightColor = Color.parseColor("#B0BEC5")
                setDrawHorizontalHighlightIndicator(false)
            }

            val displayFormat = if (isCustomRange) {
                SimpleDateFormat("dd/MM/yy", Locale("es")).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            } else {
                getDisplayFormat(periodUnit, periodAmount)
            }

            val formatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (value % 1f != 0f || index < 0 || index >= timestamps.size) return ""
                    val raw = timestamps[index]
                    return formatForAxis(raw, displayFormat)
                }
            }

            val maxLabels = when {
                timestamps.size <= 5 -> timestamps.size
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

                val mv = CustomMarkerView(context, timestamps, item.unit)
                mv.chartView = this
                marker = mv

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawLabels(true)
                    valueFormatter = formatter

                    granularity = 1f
                    isGranularityEnabled = true

                    axisMinimum = 0f
                    axisMaximum = if (timestamps.size > 1) (timestamps.size - 1).toFloat() else 0f

                    setLabelCount(maxLabels, false)

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
                setExtraOffsets(4f, 4f, 4f, 16f)
                animateX(600)
                invalidate()
            }
        }

        private fun getDisplayFormat(unit: String, amount: Int): SimpleDateFormat {
            val pattern = when (unit) {
                "hour" -> "HH:mm"
                "week" -> "EEE dd"
                "month" -> if (amount <= 1) "dd MMM" else "dd/MM/yy"
                else -> "dd/MM/yy"
            }
            return SimpleDateFormat(pattern, Locale("es")).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
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

    class CustomMarkerView(
        context: Context,
        private val timestamps: List<String>,
        private val unit: String
    ) : MarkerView(context, R.layout.marker_view_chart) {

        private val tvContent: TextView = findViewById(R.id.tvMarkerContent)

        private val tooltipFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale("es")).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            if (e == null) return
            val index = e.x.toInt()

            if (index >= 0 && index < timestamps.size) {
                val rawTimestamp = timestamps[index]
                var formattedDateStr = rawTimestamp

                for (fmt in isoFormats) {
                    try {
                        val date = fmt.parse(rawTimestamp)
                        if (date != null) {
                            formattedDateStr = tooltipFormat.format(date)
                            break
                        }
                    } catch (_: Exception) {}
                }

                val valueStr = if (e.y == e.y.toLong().toFloat()) e.y.toLong().toString() else e.y.toString()

                tvContent.text = "$formattedDateStr\nValor: $valueStr $unit"
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF((-(width / 2)).toFloat(), (-height).toFloat() - 10f)
        }
    }
}