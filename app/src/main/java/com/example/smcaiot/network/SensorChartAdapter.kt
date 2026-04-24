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

class SensorChartAdapter(
    private var items: List<SensorChartItem> = emptyList()
) : RecyclerView.Adapter<SensorChartAdapter.ChartViewHolder>() {

    fun updateData(newItems: List<SensorChartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_chart, parent, false)
        return ChartViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ChartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewSensorColor: View = itemView.findViewById(R.id.viewSensorColor)
        private val tvSensorName: TextView = itemView.findViewById(R.id.tvSensorName)
        private val tvDisplayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        private val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        private val lineChart: LineChart = itemView.findViewById(R.id.lineChart)

        fun bind(item: SensorChartItem) {
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

            setupChart(item, lineColor)
        }

        private fun setupChart(item: SensorChartItem, lineColor: Int) {
            val chartEntries = item.entries.mapIndexed { index, (_, value) ->
                Entry(index.toFloat(), value.toFloat())
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
                    setDrawLabels(false)
                    granularity = 1f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#EEEEEE")
                    textColor = Color.parseColor("#757575")
                    textSize = 10f
                }

                axisRight.isEnabled = false
                animateX(600)
                invalidate()
            }
        }
    }
}
