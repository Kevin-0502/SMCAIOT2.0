package com.example.smcaiot.network

import com.example.smcaiot.models.EntityResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface AuraApiService {

    @GET("v1/ngsi/entities")
    suspend fun getEntities(
        @Header("fiware-service") fiwareService: String = "sv",
        @Header("fiware-servicepath") fiwareServicePath: String = "/SanSalvador/SSE",
        @Header("Authorization") authorization: String
    ): Response<List<EntityResponse>>
}