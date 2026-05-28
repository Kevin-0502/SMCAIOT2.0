package com.example.smcaiot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smcaiot.models.LoginRequest
import com.example.smcaiot.network.RetrofitClient
import com.example.smcaiot.network.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginActivity : AppCompatActivity() {

    private lateinit var tilIdentifier: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etIdentifier: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressLogin: ProgressBar
    private lateinit var tvLoginError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay sesión activa, ir directo a MainActivity
        SessionManager.init(this)
        if (SessionManager.isLoggedIn()) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        tilIdentifier = findViewById(R.id.tilIdentifier)
        tilPassword = findViewById(R.id.tilPassword)
        etIdentifier = findViewById(R.id.etIdentifier)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressLogin = findViewById(R.id.progressLogin)
        tvLoginError = findViewById(R.id.tvLoginError)

        btnLogin.setOnClickListener { attemptLogin() }

        // Permitir login con tecla "Done" del teclado
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }
    }

    private fun attemptLogin() {
        // Limpiar errores anteriores
        tilIdentifier.error = null
        tilPassword.error = null
        tvLoginError.visibility = View.GONE

        val identifier = etIdentifier.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""

        // Validación local
        if (identifier.isEmpty()) {
            tilIdentifier.error = "Ingresa tu correo o usuario"
            etIdentifier.requestFocus()
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = "Ingresa tu contraseña"
            etPassword.requestFocus()
            return
        }

        // Mostrar loading
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(identifier = identifier, password = password)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Guardar sesión
                        SessionManager.saveToken(body.token)
                        SessionManager.saveUserData(
                            userId = body.user.id,
                            firstName = body.user.firstName,
                            lastName = body.user.lastName,
                            username = body.user.username,
                            email = body.user.email,
                            role = body.user.role.nombre,
                            roleDescription = body.user.role.descripcion,
                            entityName = body.user.entity.name,
                            nivel = body.user.nivel,
                            avatar = body.user.avatar
                        )
                        goToMain()
                    } else {
                        showError("Respuesta inesperada del servidor")
                    }
                } else {
                    val code = response.code()
                    when (code) {
                        401 -> showError("Credenciales incorrectas")
                        403 -> showError("Tu cuenta está bloqueada")
                        404 -> showError("Usuario no encontrado")
                        429 -> showError("Demasiados intentos. Intenta más tarde")
                        in 500..599 -> showError("Error del servidor. Intenta más tarde")
                        else -> showError("Error $code: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                val isConnectionError = e is UnknownHostException ||
                        e is ConnectException ||
                        e is SocketTimeoutException ||
                        e.cause is UnknownHostException ||
                        e.cause is ConnectException ||
                        e.cause is SocketTimeoutException

                if (isConnectionError) {
                    showError("Sin conexión a internet. Revisa tu red e inténtalo de nuevo")
                } else {
                    showError("Error inesperado: ${e.localizedMessage}")
                }
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        etIdentifier.isEnabled = !loading
        etPassword.isEnabled = !loading
    }

    private fun showError(message: String) {
        tvLoginError.text = message
        tvLoginError.visibility = View.VISIBLE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
