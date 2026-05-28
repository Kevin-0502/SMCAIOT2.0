package com.example.smcaiot.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Administra la sesión del usuario: token de autenticación y datos del perfil.
 * Usa SharedPreferences para persistir entre reinicios de la app.
 */
object SessionManager {

    private const val PREFS_NAME = "smcaiot_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"
    private const val KEY_ROLE_DESC = "role_description"
    private const val KEY_ENTITY_NAME = "entity_name"
    private const val KEY_NIVEL = "nivel"
    private const val KEY_AVATAR = "avatar"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Token ────────────────────────────────────────────────────────

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    // ── Datos del usuario ────────────────────────────────────────────

    fun saveUserData(
        userId: String,
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        role: String,
        roleDescription: String,
        entityName: String,
        nivel: Int,
        avatar: String?
    ) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_ROLE, role)
            putString(KEY_ROLE_DESC, roleDescription)
            putString(KEY_ENTITY_NAME, entityName)
            putInt(KEY_NIVEL, nivel)
            putString(KEY_AVATAR, avatar)
            apply()
        }
    }

    fun getFullName(): String {
        val first = prefs.getString(KEY_FIRST_NAME, "") ?: ""
        val last = prefs.getString(KEY_LAST_NAME, "") ?: ""
        return "$first $last".trim()
    }

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""
    fun getRoleDescription(): String = prefs.getString(KEY_ROLE_DESC, "") ?: ""
    fun getEntityName(): String = prefs.getString(KEY_ENTITY_NAME, "") ?: ""
    fun getNivel(): Int = prefs.getInt(KEY_NIVEL, 0)
    fun getInitials(): String {
        val first = prefs.getString(KEY_FIRST_NAME, "") ?: ""
        val last = prefs.getString(KEY_LAST_NAME, "") ?: ""
        return buildString {
            if (first.isNotEmpty()) append(first[0].uppercaseChar())
            if (last.isNotEmpty()) append(last[0].uppercaseChar())
        }
    }

    // ── Cerrar sesión ────────────────────────────────────────────────

    fun logout() {
        prefs.edit().clear().apply()
    }
}
