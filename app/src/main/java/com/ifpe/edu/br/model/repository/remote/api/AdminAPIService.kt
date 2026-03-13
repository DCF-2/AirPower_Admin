package com.ifpe.edu.br.model.repository.remote.api

import com.ifpe.edu.br.model.provisioning.AllowedNetwork
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.PageData
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import retrofit2.Response
import retrofit2.http.*

interface AdminAPIService {

    // 1. O FIM DO MOCK! Busca as redes Wi-Fi reais do nosso PostgreSQL
    @GET("/api/wifi/authorized")
    suspend fun getAuthorizedNetworks(): List<AllowedNetwork>

    // 2. Criar Dispositivo (Apontando para a Proxy com o cabeçalho mágico)
    @POST("/api/devices")
    suspend fun registerDevice(
        @Header("X-ThingsBoard-URL") tbUrl: String,
        @Body device: DeviceRegistration
    ): ThingsBoardDevice

    // 3. Buscar Credenciais (Via Proxy)
    @GET("/api/devices/{id}/credentials")
    suspend fun getDeviceCredentials(
        @Header("X-ThingsBoard-URL") tbUrl: String,
        @Path("id") deviceId: String
    ): DeviceCredentials

    // 4. Salvar Localização (Via Proxy)
    @POST("/api/devices/{id}/location")
    suspend fun saveDeviceLocation(
        @Header("X-ThingsBoard-URL") tbUrl: String,
        @Path("id") deviceId: String,
        @Body location: Map<String, Double>
    ): Response<Void>

    // 5. Listar Dispositivos do Tenant (Via Proxy)
    @GET("/api/devices")
    suspend fun getTenantDevices(
        @Header("X-ThingsBoard-URL") tbUrl: String,
        @Query("pageSize") pageSize: Int = 1000,
        @Query("page") page: Int = 0
    ): PageData<ThingsBoardDevice>
}