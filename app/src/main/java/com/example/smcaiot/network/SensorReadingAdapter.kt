package com.example.smcaiot.network

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.R
import com.example.smcaiot.models.SensorValue

class SensorReadingAdapter(
    private var items: List<SensorValue> = emptyList()
) : RecyclerView.Adapter<SensorReadingAdapter.SensorViewHolder>() {

    fun updateData(newItems: List<SensorValue>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_reading, parent, false)
        return SensorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColorIndicator: View = itemView.findViewById(R.id.viewColorIndicator)
        private val tvSensorName: TextView = itemView.findViewById(R.id.tvSensorName)
        private val tvDisplayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        private val tvSensorValue: TextView = itemView.findViewById(R.id.tvSensorValue)

        fun bind(sensor: SensorValue) {
            tvSensorName.text = sensor.name ?: "—"

            val valueText = buildString {
                append(sensor.value?.let {
                    if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                } ?: "—")
                if (!sensor.unit.isNullOrEmpty()) append(" ${sensor.unit}")
            }
            tvSensorValue.text = valueText

            if (!sensor.displayName.isNullOrEmpty()) {
                tvDisplayName.text = sensor.displayName
                tvDisplayName.visibility = View.VISIBLE
            } else {
                tvDisplayName.visibility = View.GONE
            }

            try {
                val color = Color.parseColor(sensor.color ?: "#CCCCCC")
                viewColorIndicator.setBackgroundColor(color)
            } catch (e: Exception) {
                viewColorIndicator.setBackgroundColor(Color.LTGRAY)
            }
        }
    }
}
