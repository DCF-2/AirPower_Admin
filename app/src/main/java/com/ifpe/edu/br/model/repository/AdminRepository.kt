package com.ifpe.edu.br.model.repository

import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.provisioning.AllowedNetwork
import com.ifpe.edu.br.model.repository.persistence.manager.JWTManager
import com.ifpe.edu.br.model.repository.remote.api.AdminServerManager
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.repository.remote.dto.error.ErrorCode
import com.ifpe.edu.br.model.util.ResultWrapper

class AdminRepository private constructor() {

    private val adminServerManager = AdminServerManager()

    // O URL do ThingsBoard que a Proxy precisa para saber para onde enviar os dados
    private val tbUrl = Constants.Constants.THINGS_BOARD_BASE_URL

    companion object {
        @Volatile
        private var instance: AdminRepository? = null

        fun getInstance(): AdminRepository {
            return instance ?: synchronized(this) {
                instance ?: AdminRepository().also { instance = it }
            }
        }
    }

    // 1. O FIM DO MOCK! 🚀
    suspend fun getAuthorizedNetworks(): ResultWrapper<List<AllowedNetwork>> {
        return try {
            // A rota de redes não precisa de token obrigatoriamente
            val service = adminServerManager.getService(null)
            val networks = service.getAuthorizedNetworks()
            ResultWrapper.Success(networks)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun registerDevice(device: DeviceRegistration): ResultWrapper<ThingsBoardDevice> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(ErrorCode.AP_JWT_EXPIRED)
            val service = adminServerManager.getService(token)

            val result = service.registerDevice(tbUrl, device)
            ResultWrapper.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun getDeviceCredentials(deviceId: String): ResultWrapper<DeviceCredentials> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(ErrorCode.AP_JWT_EXPIRED)
            val service = adminServerManager.getService(token)

            val result = service.getDeviceCredentials(tbUrl, deviceId)
            ResultWrapper.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun saveDeviceLocation(deviceId: String, lat: Double, lng: Double): ResultWrapper<Boolean> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(ErrorCode.AP_JWT_EXPIRED)
            val service = adminServerManager.getService(token)

            // Cria um map simples para enviar o JSON {"latitude": X, "longitude": Y}
            val locationMap = mapOf("latitude" to lat, "longitude" to lng)
            val response = service.saveDeviceLocation(tbUrl, deviceId, locationMap)

            if (response.isSuccessful) {
                ResultWrapper.Success(true)
            } else {
                ResultWrapper.GenericError(response.code(), "Erro ao salvar local")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun getTenantDevices(): ResultWrapper<List<ThingsBoardDevice>> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(ErrorCode.AP_JWT_EXPIRED)
            val service = adminServerManager.getService(token)

            val pageData = service.getTenantDevices(tbUrl)
            ResultWrapper.Success(pageData.data)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    private suspend fun getToken(): String? {
        return JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)?.token
    }
}