package com.example.smcaiot.network

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.R
import com.example.smcaiot.models.EntityResponse

class EntityAdapter(
    private var entities: List<EntityResponse> = emptyList()
) : RecyclerView.Adapter<EntityAdapter.EntityViewHolder>() {

    fun updateData(newEntities: List<EntityResponse>) {
        entities = newEntities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entity, parent, false)
        return EntityViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entities[position])
    }

    override fun getItemCount(): Int = entities.size

    class EntityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColorIndicator: View = itemView.findViewById(R.id.viewColorIndicator)
        private val tvEntityId: TextView = itemView.findViewById(R.id.tvEntityId)
        private val tvLevel: TextView = itemView.findViewById(R.id.tvLevel)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvHighestAlert: TextView = itemView.findViewById(R.id.tvHighestAlert)
        private val llVariables: LinearLayout = itemView.findViewById(R.id.llVariables)
        private val tvTimeInstant: TextView = itemView.findViewById(R.id.tvTimeInstant)

        fun bind(entity: EntityResponse) {
            tvEntityId.text = entity.id
            tvType.text = "Tipo: ${entity.type}"
            tvLevel.text = "Nivel: ${entity.level}"

            // Color indicator
            try {
                viewColorIndicator.setBackgroundColor(Color.parseColor(entity.color))
            } catch (e: Exception) {
                viewColorIndicator.setBackgroundColor(Color.GRAY)
            }

            // Device name
            if (!entity.deviceName.isNullOrEmpty()) {
                tvDeviceName.text = "Dispositivo: ${entity.deviceName}"
                tvDeviceName.visibility = View.VISIBLE
            } else {
                tvDeviceName.visibility = View.GONE
            }

            // Address
            if (!entity.address.isNullOrEmpty()) {
                tvAddress.text = entity.address
                tvAddress.visibility = View.VISIBLE
            } else {
                tvAddress.visibility = View.GONE
            }

            // Highest alert
            if (!entity.highestAlertName.isNullOrEmpty()) {
                tvHighestAlert.text = "⚠ Alerta: ${entity.highestAlertName} (${entity.highestAlertVariable})"
                tvHighestAlert.visibility = View.VISIBLE
            } else {
                tvHighestAlert.visibility = View.GONE
            }

            // Variables
            llVariables.removeAllViews()
            entity.variables.forEach { variable ->
                val tv = TextView(itemView.context).apply {
                    text = "  • ${variable.name}: ${variable.value ?: "N/A"}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#666666"))
                }
                llVariables.addView(tv)
            }

            // Timestamp
            tvTimeInstant.text = entity.timeInstant ?: ""
        }
    }
}