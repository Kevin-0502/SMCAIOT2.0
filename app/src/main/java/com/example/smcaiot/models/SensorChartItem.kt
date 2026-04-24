package com.example.smcaiot.models

/**
 * Agrupa todos los puntos históricos de un sensor para alimentar su gráfico de líneas.
 *
 * @param name        Nombre del sensor (ej. "Temperatura Aire")
 * @param unit        Unidad de medida (ej. "°C")
 * @param color       Color hex del nivel actual (ej. "#33d17a")
 * @param displayName Etiqueta del nivel actual (ej. "Templado"), puede ser null
 * @param latestValue Valor más reciente
 * @param entries     Lista de pares (timestamp ISO, valor) ordenados del más antiguo al más reciente
 */
data class SensorChartItem(
    val name: String,
    val unit: String,
    val color: String,
    val displayName: String?,
    val latestValue: Double,
    val entries: List<Pair<String, Double>>
)
