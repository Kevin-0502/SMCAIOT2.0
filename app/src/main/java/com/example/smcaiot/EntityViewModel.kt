package com.example.smcaiot

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smcaiot.models.EntityResponse
import com.example.smcaiot.network.RetrofitClient
import com.example.smcaiot.network.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException

class EntityViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "EntityViewModel"
    }

    private val authToken: String
        get() = SessionManager.getToken() ?: ""
    private val filterType: String = application.getString(R.string.TipoId)

    // Estado de la UI
    private val _uiState = MutableStateFlow<EntityUiState>(EntityUiState.Loading)
    val uiState: StateFlow<EntityUiState> = _uiState.asStateFlow()

    // Lista de entidades filtradas por tipo
    private val _entities = MutableStateFlow<List<EntityResponse>>(emptyList())
    val entities: StateFlow<List<EntityResponse>> = _entities.asStateFlow()

    private var loaded = false

    fun loadEntities() {
        if (loaded && _entities.value.isNotEmpty()) return

        _uiState.value = EntityUiState.Loading

        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando entidades...")
                val response = RetrofitClient.apiService.getEntities(
                    authorization = authToken
                )
                if (response.isSuccessful) {
                    val allEntities = response.body() ?: emptyList()
                    Log.d(TAG, "Entidades recibidas: ${allEntities.size}")

                    if (filterType != "") {
                        val filtered = allEntities.filter {
                            it.type.equals(filterType, ignoreCase = true)
                        }
                        Log.d(TAG, "Entidades filtradas ($filterType): ${filtered.size}")
                        _entities.value = filtered
                    } else {
                        _entities.value = allEntities
                    }

                    _uiState.value = EntityUiState.Success
                    loaded = true

                } else {
                    val code = response.code()
                    val errorMsg = "Error: $code - ${response.message()}"
                    Log.e(TAG, errorMsg)
                    _uiState.value = EntityUiState.ApiError(
                        message = errorMsg,
                        code = code
                    )
                }
            } catch (e: Exception) {
                val errorMsg = "Error de conexión: ${e.message}"
                Log.e(TAG, errorMsg, e)

                val isConnectionError = e is UnknownHostException ||
                        e is ConnectException ||
                        e is SocketTimeoutException

                if (isConnectionError) {
                    _uiState.value = EntityUiState.NoConnection(errorMsg)
                } else {
                    _uiState.value = EntityUiState.ApiError(
                        message = errorMsg,
                        code = null
                    )
                }
            }
        }
    }

    fun refresh() {
        loaded = false
        loadEntities()
    }
}

sealed class EntityUiState {
    data object Loading : EntityUiState()
    data object Success : EntityUiState()
    data class NoConnection(val message: String) : EntityUiState()
    data class ApiError(val message: String, val code: Int?) : EntityUiState()
}
