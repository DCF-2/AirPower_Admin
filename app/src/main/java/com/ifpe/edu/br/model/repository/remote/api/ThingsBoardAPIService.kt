package com.ifpe.edu.br.model.repository.remote.api

import com.google.gson.JsonArray
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
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    // Busca os últimos valores de latitude e longitude da TELEMETRIA
    @GET("/api/plugins/telemetry/DEVICE/{deviceId}/values/timeseries?keys=latitude,longitude")
    suspend fun getDeviceTelemetry(
        @Path("deviceId") deviceId: String
    ): Response<JsonObject>

    // Deleta um dispositivo pelo ID
    @DELETE("/api/device/{deviceId}")
    suspend fun deleteDevice(
        @Path("deviceId") deviceId: String
    ): Response<Void>

    // Busca TODA a telemetria mais recente (sem especificar chaves)
    @GET("/api/plugins/telemetry/DEVICE/{deviceId}/values/timeseries")
    suspend fun getAllDeviceTelemetry(
        @Path("deviceId") deviceId: String
    ): Response<JsonObject>

    // Busca atributos de servidor do dispositivo
    @GET("/api/plugins/telemetry/DEVICE/{deviceId}/values/attributes/SERVER_SCOPE")
    suspend fun getDeviceAttributes(
        @Path("deviceId") deviceId: String
    ): Response<JsonArray> // O ThingsBoard costuma retornar atributos como um Array de objetos

    // Veja que adicionamos "Infos" no final da URL
    @GET("/api/tenant/deviceInfos")
    suspend fun getDevices(
        @Query("pageSize") pageSize: Int = 100,
        @Query("page") page: Int = 0
    ): Response<JsonObject>
}

data class TelemetryValue(
    val ts: Long,
    val value: String
)