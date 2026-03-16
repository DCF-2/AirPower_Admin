package com.ifpe.edu.br.model.repository.remote.api

import com.ifpe.edu.br.model.provisioning.AllowedNetwork
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.PageData
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.repository.remote.dto.auth.LoginRequest
import com.ifpe.edu.br.model.repository.remote.dto.auth.Token
import retrofit2.Response
import retrofit2.http.*

interface AdminAPIService {

    // ==========================================
    // 1. IAM (Identidade e Autenticação)
    // ==========================================

    @POST("/api/users/register")
    suspend fun registerUser(
        @Body payload: Map<String, String>
    ): Response<String>

    @POST("/api/proxy/auth/login")
    suspend fun login(
        @Body credentials: LoginRequest
    ): Token

    // ==========================================
    // 2. WI-FI (Banco de Dados do BFF)
    // ==========================================

    @GET("/api/wifi/authorized")
    suspend fun getAuthorizedNetworks(): List<AllowedNetwork>

    // ==========================================
    // 3. GATEWAY THINGSBOARD (Proxy)
    // ==========================================

    // O cabeçalho X-User-Email informa à Proxy para qual ThingsBoard encaminhar!

    @GET("/api/proxy/tenant/devices")
    suspend fun getTenantDevices(
        @Header("X-User-Email") userEmail: String,
        @Query("pageSize") pageSize: Int = 1000,
        @Query("page") page: Int = 0
    ): PageData<ThingsBoardDevice>

    @POST("/api/proxy/device")
    suspend fun registerDevice(
        @Header("X-User-Email") userEmail: String,
        @Body device: DeviceRegistration
    ): ThingsBoardDevice

    @GET("/api/proxy/device/{id}/credentials")
    suspend fun getDeviceCredentials(
        @Header("X-User-Email") userEmail: String,
        @Path("id") deviceId: String
    ): DeviceCredentials

    @POST("/api/proxy/device/{id}/location")
    suspend fun saveDeviceLocation(
        @Header("X-User-Email") userEmail: String,
        @Path("id") deviceId: String,
        @Body location: Map<String, Double>
    ): Response<Void>
}