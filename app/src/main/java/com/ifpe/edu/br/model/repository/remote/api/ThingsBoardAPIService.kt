package com.ifpe.edu.br.model.repository.remote.api

import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Interface para comunicação direta com a API do ThingsBoard.
 * Usada para funções administrativas que o AirPowerServer não expõe.
 */
interface ThingsBoardAPIService {

    @POST("/api/device")
    suspend fun registerDevice(@Body device: DeviceRegistration): ThingsBoardDevice

    @GET("/api/device/{deviceId}/credentials")
    suspend fun getDeviceCredentials(@Path("deviceId") deviceId: String): DeviceCredentials
}