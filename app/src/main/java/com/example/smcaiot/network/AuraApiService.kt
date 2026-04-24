package com.example.smcaiot.network

import com.example.smcaiot.models.EntityResponse
import com.example.smcaiot.models.HistoricalResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AuraApiService {

    @GET("v1/ngsi/entities")
    suspend fun getEntities(
        @Header("fiware-service") fiwareService: String = "sv",
        @Header("fiware-servicepath") fiwareServicePath: String = "/SanSalvador/SSE",
        @Header("Authorization") authorization: String
    ): Response<List<EntityResponse>>

    @GET("v1/ngsi/historical/{entityId}/sensors")
    suspend fun getHistoricalSensors(
        @Path("entityId", encoded = true) entityId: String,
        @Query("amount") amount: Int = 1,
        @Query("unit") unit: String = "month",
        @Header("fiware-service") fiwareService: String = "sv",
        @Header("fiware-servicepath") fiwareServicePath: String = "/SanSalvador/SSE",
        @Header("Authorization") authorization: String
    ): Response<HistoricalResponse>
}