package com.example.smcaiot

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smcaiot.network.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header
        val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        val tvFullName: TextView = view.findViewById(R.id.tvFullName)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)

        // Info card
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val tvEntity: TextView = view.findViewById(R.id.tvEntity)
        val tvNivel: TextView = view.findViewById(R.id.tvNivel)

        val btnLogout: MaterialButton = view.findViewById(R.id.btnLogout)

        // Llenar datos desde SessionManager
        tvInitials.text = SessionManager.getInitials()
        tvFullName.text = SessionManager.getFullName()
        tvEmail.text = SessionManager.getEmail()
        tvUsername.text = SessionManager.getUsername()
        tvRole.text = SessionManager.getRoleDescription().ifEmpty { SessionManager.getRole() }
        tvEntity.text = SessionManager.getEntityName().replaceFirstChar { it.uppercaseChar() }
        tvNivel.text = SessionManager.getNivel().toString()

        // Cerrar sesión
        btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("Se cerrará tu sesión actual y tendrás que iniciar sesión de nuevo.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    SessionManager.logout()
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                .show()
        }
    }
}
