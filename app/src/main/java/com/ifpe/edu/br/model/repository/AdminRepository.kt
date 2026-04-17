package com.ifpe.edu.br.model.repository

import android.content.Context
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.provisioning.AllowedNetwork
import com.ifpe.edu.br.model.repository.persistence.AirPowerDatabase
import com.ifpe.edu.br.model.repository.persistence.manager.JWTManager
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.repository.persistence.model.AirPowerToken
import com.ifpe.edu.br.model.repository.remote.api.AdminServerManager
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.repository.remote.dto.auth.LoginRequest
import com.ifpe.edu.br.model.repository.remote.dto.auth.RegisterRequest
import com.ifpe.edu.br.model.repository.remote.dto.auth.Token
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.model.repository.remote.api.LocationPayload
import com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo

class AdminRepository private constructor(private val context: Context) {

    private val adminServerManager = AdminServerManager(context)
    private val tokenDao = AirPowerDatabase.getDataBaseInstance(context).getTokenDaoInstance()
    private val prefs = SharedPrefManager.getInstance(context)

    companion object {
        @Volatile
        private var instance: AdminRepository? = null

        fun getInstance(context: Context): AdminRepository {
            return instance ?: synchronized(this) {
                instance ?: AdminRepository(context.applicationContext).also { instance = it }
            }
        }

        // Mantém a compatibilidade caso seja chamado sem contexto em locais seguros
        fun getInstance(): AdminRepository {
            return instance ?: throw IllegalStateException("AdminRepository precisa ser inicializado com contexto primeiro.")
        }
    }

    // ==========================================
    // 1. BANCO DE DADOS LOCAL (Para o JWTManager)
    // ==========================================

    suspend fun save(token: AirPowerToken) = tokenDao.insert(token)

    suspend fun update(token: AirPowerToken) = tokenDao.update(token)

    suspend fun getTokenByConnectionId(id: Int): AirPowerToken? = tokenDao.getTokenByClient(id)

    // ==========================================
    // 2. IAM E SESSÃO (Login, Registro e Validação)
    // ==========================================

    suspend fun login(credentials: LoginRequest): ResultWrapper<Token> {
        return try {
            val service = adminServerManager.getService(null)
            val tokenResponse = service.login(credentials)

            // Salva dados auxiliares na sessão
            prefs.writeString("LOGGED_USER_EMAIL", credentials.email)
            prefs.writeString("LOGIN_TIMESTAMP", System.currentTimeMillis().toString())
            tokenResponse.tbUrl?.let { prefs.writeString("LOGGED_USER_TB_URL", it)}

            val connectionId = Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD

            val existingToken = getTokenByConnectionId(connectionId)

            if (existingToken != null) {
                existingToken.jwt = tokenResponse.token
                existingToken.refreshToken = tokenResponse.refreshToken
                update(existingToken)
                com.ifpe.edu.br.model.util.AirPowerLog.d("AdminRepository", "Token atualizado fisicamente no DB!")
            } else {
                val newToken = AirPowerToken(
                    connectionId,
                    tokenResponse.token,
                    tokenResponse.refreshToken,
                    tokenResponse.scope ?: "TENANT_ADMIN"
                )
                save(newToken)
                com.ifpe.edu.br.model.util.AirPowerLog.d("AdminRepository", "Novo Token salvo fisicamente no DB!")
            }

            // Mantemos o handleAuthentication apenas para atualizar variáveis em memória (se o JWTManager precisar)
            try {
                JWTManager.handleAuthentication(connectionId, tokenResponse) { }
            } catch (e: Exception) {
                // Ignoramos erros do JWTManager, o banco já está seguro.
            }

            ResultWrapper.Success(tokenResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    //  VERIFICA SE A SESSÃO DE 3 HORAS AINDA É VÁLIDA <---
    suspend fun isSessionValid(): Boolean {
        // Pega a hora que o usuário logou
        val loginTimeStr = prefs.readString("LOGIN_TIMESTAMP") ?: "0"
        val loginTime = loginTimeStr.toLongOrNull() ?: 0L

        // Define o limite de 3 horas (em milissegundos)
        val threeHoursInMillis = 3 * 60 * 60 * 1000L

        // A sessão é válida se o tempo atual MENOS o tempo do login for menor que 3 horas
        val isTimeValid = (System.currentTimeMillis() - loginTime) < threeHoursInMillis

        // E, claro, verifica se o Token não foi apagado do banco de dados
        val tokenExists = getToken() != null

        return isTimeValid && tokenExists
    }

    // --- REGISTRO DE USUÁRIO  ---
    suspend fun registerUser(request: RegisterRequest): ResultWrapper<Unit> {
        return try {
            val service = adminServerManager.getService(null)
            val response = service.registerUser(request)

            if (response.isSuccessful) {
                ResultWrapper.Success(Unit)
            } else {
                ResultWrapper.ApiError(response.code(), response.message())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    // ==========================================
    // 3. WIFI E DEVICES (BFF)
    // ==========================================

    suspend fun getAuthorizedNetworks(): ResultWrapper<List<AllowedNetwork>> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val service = adminServerManager.getService(token)

            val networks = service.getAuthorizedNetworks()
            ResultWrapper.Success(networks)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun registerDevice(device: DeviceRegistration): ResultWrapper<ThingsBoardDevice> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val email = prefs.readString("LOGGED_USER_EMAIL") ?: ""

            val service = adminServerManager.getService(token)
            val result = service.registerDevice(email, device)
            ResultWrapper.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun getDeviceCredentials(deviceId: String): ResultWrapper<DeviceCredentials> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val email = prefs.readString("LOGGED_USER_EMAIL") ?: ""

            val service = adminServerManager.getService(token)
            val result = service.getDeviceCredentials(email, deviceId)
            ResultWrapper.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun saveDeviceLocation(deviceId: String, lat: Double, lng: Double, description: String): ResultWrapper<Boolean> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val email = prefs.readString("LOGGED_USER_EMAIL") ?: ""

            val service = adminServerManager.getService(token)
            
            val payload = LocationPayload(lat, lng, description)

            val response = service.saveDeviceLocation(email, deviceId, payload)

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
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val email = prefs.readString("LOGGED_USER_EMAIL") ?: ""

            val service = adminServerManager.getService(token)
            val pageData = service.getTenantDevices(email)

            ResultWrapper.Success(pageData.data)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    private suspend fun getToken(): String? {
        return JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)?.token
    }

    // ==========================================
    // 4. MAPA E TELEMETRIA (Novo!)
    // ==========================================

    suspend fun getLatestTelemetry(deviceId: String, keys: List<String>): ResultWrapper<Map<String, List<Map<String, Any>>>> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val email = prefs.readString("LOGGED_USER_EMAIL") ?: ""
            val service = adminServerManager.getService(token)

            // Se a lista vier vazia, não mandamos o parâmetro de chaves (ou mandamos vazio, dependendo de como o SpringBoot espera)
            val keysString = if (keys.isEmpty()) "" else keys.joinToString(",")

            val response = service.getLatestTelemetry(email, deviceId, keysString)
            ResultWrapper.Success(response)

        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    // ==========================================
    // 5. CONTROLE REMOTO (RPC)
    // ==========================================
    suspend fun sendRpcCommand(deviceId: String, method: String, params: Any): ResultWrapper<Boolean> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val email = prefs.readString("LOGGED_USER_EMAIL") ?: ""
            val service = adminServerManager.getService(token)

            // Monta o Payload no padrão do ThingsBoard {"method": "setPower", "params": true}
            val payload = mapOf(
                "method" to method,
                "params" to params
            )

            val response = service.sendRpcCommand(email, deviceId, payload)

            if (response.isSuccessful) {
                ResultWrapper.Success(true)
            } else {
                ResultWrapper.GenericError(response.code(), "Falha ao enviar comando RPC")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun getTenantDashboards(): ResultWrapper<List<DashboardInfo>> {
        return try {
            val token = getToken() ?: return ResultWrapper.ApiError(Constants.ResponseErrorCode.AP_JWT_EXPIRED)
            val email = prefs.readString("LOGGED_USER_EMAIL") ?: ""

            val service = adminServerManager.getService(token)
            val pageData = service.getTenantDashboards(email)

            ResultWrapper.Success(pageData.data)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }
}