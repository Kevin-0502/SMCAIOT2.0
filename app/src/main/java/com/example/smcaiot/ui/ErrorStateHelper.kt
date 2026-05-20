package com.example.smcaiot.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.smcaiot.R

/**
 * Tipos de error para las pantallas de estado vacío / error.
 */
enum class ErrorType {
    NO_CONNECTION,
    API_ERROR,
    NO_DATA
}

/**
 * Helper para configurar el layout_error_state.xml reutilizable.
 *
 * Uso:
 *   val errorView = view.findViewById<View>(R.id.layoutErrorState)
 *   ErrorStateHelper.show(errorView, ErrorType.NO_CONNECTION, errorCode = null) {
 *       // Acción del botón (reintentar, cambiar fechas, etc.)
 *   }
 *   ErrorStateHelper.hide(errorView)
 */
object ErrorStateHelper {

    fun show(
        errorLayout: View,
        type: ErrorType,
        errorCode: Int? = null,
        secondaryAction: (() -> Unit)? = null,
        onAction: () -> Unit
    ) {
        errorLayout.visibility = View.VISIBLE

        val viewCircle = errorLayout.findViewById<View>(R.id.viewErrorCircle)
        val ivIcon = errorLayout.findViewById<ImageView>(R.id.ivErrorIcon)
        val tvTitle = errorLayout.findViewById<TextView>(R.id.tvErrorTitle)
        val tvDescription = errorLayout.findViewById<TextView>(R.id.tvErrorDescription)
        val btnAction = errorLayout.findViewById<TextView>(R.id.btnErrorAction)
        val tvSecondary = errorLayout.findViewById<TextView>(R.id.tvErrorSecondaryAction)

        when (type) {
            ErrorType.NO_CONNECTION -> {
                viewCircle.setBackgroundResource(R.drawable.bg_circle_pink)
                ivIcon.setImageResource(R.drawable.ic_no_wifi)
                tvTitle.text = "Sin conexión"
                tvDescription.text = "No fue posible conectar con el servidor.\nRevisa tu internet e inténtalo de nuevo"
                btnAction.text = "Aceptar"
            }

            ErrorType.API_ERROR -> {
                viewCircle.setBackgroundResource(R.drawable.bg_circle_pink)
                ivIcon.setImageResource(R.drawable.ic_error_face)
                tvTitle.text = "Algo salió mal"
                val codeText = if (errorCode != null) "Código $errorCode\n" else ""
                tvDescription.text = "${codeText}Nuestro equipo ya fue notificado"
                btnAction.text = "Reintentar"
            }

            ErrorType.NO_DATA -> {
                viewCircle.setBackgroundResource(R.drawable.bg_circle_gray)
                ivIcon.setImageResource(R.drawable.ic_no_data)
                tvTitle.text = "No hay mediciones\npara este rango"
                tvDescription.text = "Intenta seleccionar un rango de fechas diferente"
                btnAction.text = "Cambiar fechas"
            }
        }

        btnAction.setOnClickListener { onAction() }

        if (secondaryAction != null) {
            tvSecondary.visibility = View.VISIBLE
            tvSecondary.setOnClickListener { secondaryAction() }
        } else {
            tvSecondary.visibility = View.GONE
        }
    }

    fun hide(errorLayout: View) {
        errorLayout.visibility = View.GONE
    }
}
