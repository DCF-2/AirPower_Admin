package com.ifpe.edu.br.model.repository.remote.api

import com.google.gson.JsonObject
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.PageData
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.repository.remote.dto.auth.LoginRequest
import com.ifpe.edu.br.model.repository.remote.dto.auth.ThingsBoardLoginResponse
import com.ifpe.edu.br.model.repository.remote.dto.user.ThingsBoardUser
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Interface para comunicação direta com a API do ThingsBoard.
 * Usada para funções administrativas que o AirPowerServer não expõe.
 */
interface ThingsBoardAPIService {

    // --- AUTENTICAÇÃO DIRETA (ADMIN) ---
    @POST("/api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): ThingsBoardLoginResponse

    @GET("/api/auth/user")
    suspend fun getUser(): ThingsBoardUser

    // --- GESTÃO DE DISPOSITIVOS ---
    @POST("/api/device")
    suspend fun registerDevice(@Body device: DeviceRegistration): ThingsBoardDevice

    @GET("/api/device/{deviceId}/credentials")
    suspend fun getDeviceCredentials(@Path("deviceId") deviceId: String): DeviceCredentials

    // Busca dispositivos do Tenant (Paginado)
    @GET("/api/tenant/deviceInfos?pageSize=1000&page=0")
    suspend fun getTenantDevices(): PageData<ThingsBoardDevice>

    // Busca os últimos valores de latitude e longitude da TELEMETRIA
    @GET("/api/plugins/telemetry/DEVICE/{deviceId}/values/timeseries?keys=latitude,longitude")
    suspend fun getDeviceTelemetry(
        @Path("deviceId") deviceId: String
    ): Response<JsonObject>

    // Salvar atributos do dispositivo (usado para localização GPS)
    @POST("/api/plugins/telemetry/DEVICE/{deviceId}/timeseries/ANY")
    suspend fun saveDeviceTelemetry(
        @Path("deviceId") deviceId: String,
        @Body telemetry: JsonObject
    ): Response<Void>
}

data class TelemetryValue(
    val ts: Long,
    val value: String
)