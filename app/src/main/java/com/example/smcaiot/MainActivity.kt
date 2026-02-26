package com.example.smcaiot

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.smcaiot.network.EntityAdapter
import com.example.smcaiot.network.RetrofitClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var rvEntities: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var adapter: EntityAdapter

    // Reemplazá con tu token real
    private val authToken = "Bearer TU_TOKEN_AQUI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvEntities = findViewById(R.id.rvEntities)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        adapter = EntityAdapter()
        rvEntities.adapter = adapter

        loadEntities()
    }

    private fun loadEntities() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getEntities(
                    authorization = authToken
                )
                if (response.isSuccessful) {
                    val entities = response.body() ?: emptyList()
                    adapter.updateData(entities)
                } else {
                    tvError.text = "Error: ${response.code()} - ${response.message()}"
                    tvError.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                tvError.text = "Error de conexión: ${e.message}"
                tvError.visibility = View.VISIBLE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}