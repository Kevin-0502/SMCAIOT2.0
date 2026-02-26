package com.example.smcaiot.models

// models/EntityResponse.kt

data class EntityResponse(
    val id: String,
    val type: String,
    val location: Location?,
    val address: String?,
    val color: String,
    val level: Int,
    val highestAlertName: String?,
    val highestAlertVariable: String?,
    val variables: List<Variable>,
    val timeInstant: String?,
    val deviceName: String?,
    val isSensorActuador: String?,
    val linkGrafana: String?,
    val tracker: Any?,
    val commands: List<Any>,
    val trackingIcon: String?,
    val fiwareService: String?,
    val fiwareServicePath: String?
)

data class Location(
    val type: String,
    val value: GeoJsonPoint,
    val metadata: Any?
)

data class GeoJsonPoint(
    val type: String,
    val coordinates: List<Double>
)

data class Variable(
    val name: String,
    val icon: String?,
    val value: String?,
    val alert: Alert?,
    val minRange: Double?,
    val maxRange: Double?
)

data class Alert(
    val name: String,
    val color: String,
    val level: Int
)
