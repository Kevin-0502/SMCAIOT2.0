package com.example.smcaiot.network

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.R
import com.example.smcaiot.models.EntityResponse

class AlertEntityAdapter(
    private var entities: List<EntityResponse> = emptyList(),
    private val onItemClick: (EntityResponse) -> Unit = {}
) : RecyclerView.Adapter<AlertEntityAdapter.AlertViewHolder>() {

    fun updateData(newEntities: List<EntityResponse>) {
        entities = newEntities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert_entity, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val entity = entities[position]
        holder.bind(entity)
        holder.itemView.setOnClickListener { onItemClick(entity) }
    }

    override fun getItemCount(): Int = entities.size

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val llVariableBadges: LinearLayout = itemView.findViewById(R.id.llVariableBadges)
        private val tvAlertDeviceName: TextView = itemView.findViewById(R.id.tvAlertDeviceName)
        private val llAlertBadge: LinearLayout = itemView.findViewById(R.id.llAlertBadge)
        private val viewAlertDot: View = itemView.findViewById(R.id.viewAlertDot)
        private val tvAlertName: TextView = itemView.findViewById(R.id.tvAlertName)

        fun bind(entity: EntityResponse) {
            tvAlertDeviceName.text = entity.deviceName ?: entity.id

            // Construir badges de variables con alertas
            buildVariableBadges(entity)

            // Badge de alerta principal
            val alertName = entity.highestAlertName
            if (!alertName.isNullOrEmpty()) {
                llAlertBadge.visibility = View.VISIBLE

                val alertColor = try {
                    Color.parseColor(entity.color)
                } catch (e: Exception) {
                    Color.parseColor("#FFA726")
                }

                // Dot de color
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(alertColor)
                }
                viewAlertDot.background = dotBg

                // Badge de texto con fondo suave
                val badgeBg = GradientDrawable().apply {
                    cornerRadius = dpToPx(12f)
                    setColor(adjustAlpha(alertColor, 0.18f))
                }
                tvAlertName.background = badgeBg
                tvAlertName.text = alertName
                tvAlertName.setTextColor(darkenColor(alertColor, 0.35f))
            } else {
                llAlertBadge.visibility = View.GONE
            }
        }

        private fun buildVariableBadges(entity: EntityResponse) {
            val variablesWithAlerts = entity.variables.filter { it.alert != null }

            // Agrupar en filas de 2 badges
            val badgeRows = variablesWithAlerts.chunked(2)

            val neededRows = badgeRows.size
            val existingRows = llVariableBadges.childCount

            var rowIndex = 0
            for (chunk in badgeRows) {
                val rowLayout: LinearLayout
                if (rowIndex < existingRows) {
                    rowLayout = llVariableBadges.getChildAt(rowIndex) as LinearLayout
                    rowLayout.visibility = View.VISIBLE
                } else {
                    rowLayout = LinearLayout(itemView.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = dpToPx(3f).toInt() }
                    }
                    llVariableBadges.addView(rowLayout)
                }

                val neededBadges = chunk.size
                val existingBadges = rowLayout.childCount

                chunk.forEachIndexed { badgeIndex, variable ->
                    val alert = variable.alert ?: return@forEachIndexed

                    val badgeView: LinearLayout
                    if (badgeIndex < existingBadges) {
                        badgeView = rowLayout.getChildAt(badgeIndex) as LinearLayout
                        badgeView.visibility = View.VISIBLE
                    } else {
                        badgeView = createBadgeView()
                        rowLayout.addView(badgeView)
                    }

                    bindBadge(badgeView, alert.color, alert.level)
                }

                // Ocultar badges sobrantes
                for (i in neededBadges until rowLayout.childCount) {
                    rowLayout.getChildAt(i).visibility = View.GONE
                }

                rowIndex++
            }

            // Ocultar filas sobrantes
            for (i in neededRows until llVariableBadges.childCount) {
                llVariableBadges.getChildAt(i).visibility = View.GONE
            }
        }

        private fun createBadgeView(): LinearLayout {
            return LinearLayout(itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(36f).toInt(),
                    dpToPx(28f).toInt()
                ).apply { marginEnd = dpToPx(3f).toInt() }
            }
        }

        private fun bindBadge(badge: LinearLayout, colorHex: String, level: Int) {
            val badgeColor = try {
                Color.parseColor(colorHex)
            } catch (e: Exception) {
                Color.parseColor("#FFA726")
            }

            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(6f)
                setColor(badgeColor)
            }
            badge.background = bg

            // Mostrar barras de nivel dentro del badge
            badge.removeAllViews()
            val barCount = level.coerceIn(1, 3)
            for (i in 0 until barCount) {
                val bar = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(4f).toInt(),
                        dpToPx(8f + (i * 4f)).toInt()
                    ).apply { marginEnd = dpToPx(2f).toInt() }

                    val barBg = GradientDrawable().apply {
                        cornerRadius = dpToPx(2f)
                        setColor(darkenColor(badgeColor, 0.25f))
                    }
                    background = barBg
                }
                badge.addView(bar)
            }
        }

        private fun dpToPx(dp: Float): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                itemView.context.resources.displayMetrics
            )
        }

        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = (255 * factor).toInt()
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }

        private fun darkenColor(color: Int, factor: Float): Int {
            val r = (Color.red(color) * (1 - factor)).toInt()
            val g = (Color.green(color) * (1 - factor)).toInt()
            val b = (Color.blue(color) * (1 - factor)).toInt()
            return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
        }
    }
}
