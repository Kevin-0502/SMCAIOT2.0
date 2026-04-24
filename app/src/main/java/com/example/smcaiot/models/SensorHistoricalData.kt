package com.example.smcaiot.models

data class HistoricalResponse(
    val entityId: String?,
    val attrName: String?,
    val service: String?,
    val servicePath: String?,
    val count: Int?,
    val values: List<HistoricalEntry>?
)

data class HistoricalEntry(
    val timestamp: String?,
    val value: List<SensorValue>?
)

data class SensorValue(
    val name: String?,
    val value: Double?,
    val unit: String?,
    val color: String?,
    val displayName: String?,
    val level: Int?
)
