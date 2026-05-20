package com.example.smcaiot.network

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.R
import com.example.smcaiot.models.SensorChartItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SensorTableAdapter(
    private var items: List<SensorChartItem> = emptyList()
) : RecyclerView.Adapter<SensorTableAdapter.TableViewHolder>() {

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

        /** Formato de salida legible */
        private val displayFormat = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())

        fun formatTimestamp(raw: String): String {
            for (fmt in isoFormats) {
                try {
                    val date = fmt.parse(raw) ?: continue
                    return displayFormat.format(date)
                } catch (_: Exception) { }
            }
            return raw // devolver tal cual si no se puede parsear
        }
    }

    fun updateData(newItems: List<SensorChartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_table, parent, false)
        return TableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewSensorColor: View = itemView.findViewById(R.id.viewSensorColor)
        private val tvSensorName: TextView = itemView.findViewById(R.id.tvSensorName)
        private val tvDisplayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        private val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        private val tvMinValue: TextView = itemView.findViewById(R.id.tvMinValue)
        private val tvAvgValue: TextView = itemView.findViewById(R.id.tvAvgValue)
        private val tvMaxValue: TextView = itemView.findViewById(R.id.tvMaxValue)
        private val llTableRows: LinearLayout = itemView.findViewById(R.id.llTableRows)

        fun bind(item: SensorChartItem) {
            tvSensorName.text = item.name
            tvCurrentValue.text = buildString {
                append(formatNumber(item.latestValue))
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

            // ── Resumen estadístico ──
            if (item.entries.isNotEmpty()) {
                val values = item.entries.map { it.second }
                val min = values.min()
                val max = values.max()
                val avg = values.average()

                val suffix = if (item.unit.isNotEmpty()) " ${item.unit}" else ""
                tvMinValue.text = "${formatNumber(min)}$suffix"
                tvAvgValue.text = "${formatNumber(avg)}$suffix"
                tvMaxValue.text = "${formatNumber(max)}$suffix"
            }

            // ── Filas de la tabla (más recientes primero) ──
            llTableRows.removeAllViews()

            val entriesToShow = item.entries.reversed().take(50)

            entriesToShow.forEachIndexed { index, (timestamp, value) ->
                val row = LinearLayout(itemView.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                    if (index % 2 == 0) {
                        setBackgroundColor(Color.parseColor("#FAFAFA"))
                    }
                }

                val tvTimestamp = TextView(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = formatTimestamp(timestamp)
                    textSize = 11f
                    setTextColor(Color.parseColor("#555555"))
                    maxLines = 1
                }

                val tvValue = TextView(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = "${formatNumber(value)} ${item.unit}"
                    textSize = 11f
                    setTextColor(Color.parseColor("#212121"))
                    gravity = android.view.Gravity.END
                }

                row.addView(tvTimestamp)
                row.addView(tvValue)
                llTableRows.addView(row)
            }

            if (item.entries.size > 50) {
                val tvMore = TextView(itemView.context).apply {
                    text = "... y ${item.entries.size - 50} registros más"
                    textSize = 11f
                    setTextColor(Color.parseColor("#999999"))
                    setPadding(8, 12, 8, 4)
                    gravity = android.view.Gravity.CENTER
                }
                llTableRows.addView(tvMore)
            }
        }

        /** Formatea un número: sin decimales si es entero, o con 2 decimales si tiene parte fraccionaria */
        private fun formatNumber(value: Double): String {
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format(Locale.getDefault(), "%.2f", value)
            }
        }
    }
}
