package com.ifpe.edu.br.model.repository
/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
import android.content.Context
import android.content.res.Resources.NotFoundException
import com.google.gson.JsonObject
import com.ifpe.edu.br.core.api.ConnectionManager
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.provisioning.AllowedNetwork
import com.ifpe.edu.br.model.repository.persistence.AirPowerDatabase
import com.ifpe.edu.br.model.repository.persistence.manager.JWTManager
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.repository.persistence.model.AirPowerToken
import com.ifpe.edu.br.model.repository.persistence.model.AirPowerUser
import com.ifpe.edu.br.model.repository.persistence.model.toThingsBoardUser
import com.ifpe.edu.br.model.repository.remote.api.AirPowerServerConnectionContractImpl
import com.ifpe.edu.br.model.repository.remote.api.AirPowerServerManager
import com.ifpe.edu.br.model.repository.remote.api.ThingsBoardServerManager
import com.ifpe.edu.br.model.repository.remote.dto.AirPowerNotificationItem
import com.ifpe.edu.br.model.repository.remote.dto.AlarmInfo
import com.ifpe.edu.br.model.repository.remote.dto.AllMetricsWrapper
import com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo
import com.ifpe.edu.br.model.repository.remote.dto.DeviceConsumption
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.DeviceSummary
import com.ifpe.edu.br.model.repository.remote.dto.DevicesStatusSummary
import com.ifpe.edu.br.model.repository.remote.dto.Id
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggregationRequest
import com.ifpe.edu.br.model.repository.remote.dto.auth.AuthUser
import com.ifpe.edu.br.model.repository.remote.dto.auth.LoginRequest
import com.ifpe.edu.br.model.repository.remote.dto.auth.Token
import com.ifpe.edu.br.model.repository.remote.dto.error.ErrorCode
import com.ifpe.edu.br.model.repository.remote.dto.user.ThingsBoardUser
import com.ifpe.edu.br.model.util.AirPowerLog
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.view.manager.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class Repository private constructor(context: Context) {
    private val db = AirPowerDatabase.getDataBaseInstance(context)
    private val tokenDao = db.getTokenDaoInstance()
    private val userDao = db.getUserDaoInstance()
    private val spManager = SharedPrefManager.getInstance(context)
    private val airPowerServerConnection =
        ConnectionManager.getInstance().getConnectionById(AirPowerServerConnectionContractImpl)
    private val airPowerServerMgr = AirPowerServerManager(airPowerServerConnection)
    private val notificationHelper = NotificationHelper(context)

    private val _devicesSummary = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val devicesSummary: StateFlow<List<DeviceSummary>> get() = _devicesSummary

    private val _alarmInfo = MutableStateFlow<List<AlarmInfo>>(emptyList())
    val alarmInfo: StateFlow<List<AlarmInfo>> = _alarmInfo.asStateFlow()

    private val _allDevicesMetricsWrapper = MutableStateFlow(getEmptyAllDevicesMetricsWrapper())
    val allDevicesMetricsWrapper: StateFlow<AllMetricsWrapper> =
        _allDevicesMetricsWrapper.asStateFlow()

    private val _dashBoardsMetricsWrapper =
        MutableStateFlow(listOf(getEmptyAllDevicesMetricsWrapper()))
    val dashBoardsMetricsWrapper: StateFlow<List<AllMetricsWrapper>> =
        _dashBoardsMetricsWrapper.asStateFlow()

    private val _notification = MutableStateFlow(getEmptyNotification())
    private val notification: StateFlow<List<AirPowerNotificationItem>> =
        _notification.asStateFlow()

    private val _dashboards = MutableStateFlow(getEmptyDashboards())
    private val dashboards: StateFlow<List<DashboardInfo>> = _dashboards.asStateFlow()

    private val thingsBoardManager = ThingsBoardServerManager()

    private var cachedAuthority: String = ""

    companion object {
        @Volatile
        private var instance: Repository? = null
        fun build(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        if (AirPowerLog.ISLOGABLE)
                            AirPowerLog.d(TAG, "build()")
                        instance = Repository(context)
                    }
                }
            }
        }

        fun getInstance(): Repository {
            return instance
                ?: throw IllegalStateException("AirPowerRepository not initialized. Call build() first.")
        }

        private val TAG = Repository::class.simpleName

    }

    suspend fun authenticate(
        user: AuthUser,
    ): ResultWrapper<Token> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "authenticate()")
        return airPowerServerMgr.authenticate(user)
    }

    suspend fun retrieveDeviceSummaryForCurrentUser(): ResultWrapper<List<DeviceSummary>> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "retrieveDeviceSummaryForCurrentUser()")
        val user = getCurrentUser()
        if (user != null) {
            if (AirPowerLog.ISVERBOSE)
                AirPowerLog.d(TAG, "Current user is valid")
            val devicesSummaryResponseWrapper =
                airPowerServerMgr.getDeviceSummariesForUser(user.toThingsBoardUser())
            if (devicesSummaryResponseWrapper is ResultWrapper.Success) {
                _devicesSummary.value = devicesSummaryResponseWrapper.value
            }
            return devicesSummaryResponseWrapper
        } else {
            return ResultWrapper.ApiError(ErrorCode.AP_REFRESH_TOKEN_EXPIRED)
        }
    }

    suspend fun retrieveAlarmInfo(): ResultWrapper<List<AlarmInfo>> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "retrieveDeviceSummaryForCurrentUser()")
        val resultWrapper = airPowerServerMgr.getAlarmsForCurrentUser()
        if (resultWrapper is ResultWrapper.Success) {
            if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "updating alarms data")
            AirPowerLog.d("alarms:", "resultado: ${resultWrapper.value}")
            _alarmInfo.value = resultWrapper.value
        }
        return resultWrapper
    }

    suspend fun updateSession(): ResultWrapper<Token> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "updateSession()")
        return airPowerServerMgr.refreshToken()
    }

    suspend fun retrieveCurrentUser(): ResultWrapper<ThingsBoardUser> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "retrieveCurrentUser()")
        val currentUserResult = airPowerServerMgr.getCurrentUser()
        if (currentUserResult is ResultWrapper.Success) {
            val storedUserSet = userDao.findAll()
            var storedUser: AirPowerUser? = null
            if (storedUserSet.isNotEmpty()) {
                storedUser = storedUserSet[0]
            }
            val incomingUser = currentUserResult.value.toAirPowerUser()
            if (storedUser == null) {
                userDao.insert(incomingUser)
                if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "save user:${incomingUser}")
            } else {
                if (storedUser.id == incomingUser.id) {
                    userDao.update(incomingUser)
                } else {
                    userDao.deleteAll()
                    userDao.insert(incomingUser)
                    if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "save user:${incomingUser}")
                }
            }
        }
        return currentUserResult
    }

    suspend fun retrieveAllDeviceAggregatedDataWrapper(
        request: AggregationRequest
    ): ResultWrapper<AggDataWrapperResponse> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "retrieveAllDeviceAggregatedDataWrapper()")
        return airPowerServerMgr.getDeviceAggregatedDataWrapper(request)
    }

    suspend fun isSessionExpired(): Boolean {
        val connectionId = AirPowerServerConnectionContractImpl.getConnectionId()
        val isExpired = connectionId.let { JWTManager.isTokenExpiredForConnection(it) }
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "isSessionExpired(): $isExpired")
        return isExpired
    }

    suspend fun logout() {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "logout()")
        val connectionId = AirPowerServerConnectionContractImpl.getConnectionId()
        JWTManager.resetTokenForConnection(connectionId)
        userDao.deleteAll()
    }

    suspend fun getTokenByConnectionId(connection: Int): AirPowerToken? {
        return withContext(Dispatchers.IO) {
            if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "getTokenByConnectionId: $connection")
            tokenDao.getTokenByClient(connection)
        }
    }

    private suspend fun save(user: ThingsBoardUser) {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "save: $user")
        require(isUserValid(user)) { "ThingsBoardUser is invalid" }

        withContext(Dispatchers.IO) {
            try {
                val airPowerUser = user.toAirPowerUser()
                userDao.insert(airPowerUser)
                if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "save user:${airPowerUser}")
            } catch (e: Exception) {
                throw IllegalStateException("Error persisting user in DB: ${e.message}")
            }
        }
    }

    private suspend fun delete(user: AirPowerUser) {
        withContext(Dispatchers.IO) {
            try {
                val persistUser = userDao.getUserById(user.id)
                persistUser?.let { userDao.delete(it) }
            } catch (e: Exception) {
                throw IllegalStateException("Error deleting user in DB: ${e.message}")
            }
        }
    }

    suspend fun save(token: AirPowerToken) {
        if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "save() token: $token")
        require(!token.jwt.isNullOrEmpty() && !token.refreshToken.isNullOrEmpty() && token.client != null) {
            "Token info is null or empty!"
        }

        withContext(Dispatchers.IO) {
            val existingToken = tokenDao.getTokenByClient(token.client)
            if (existingToken != null) {
                AirPowerLog.e(TAG, "save(): ERROR: Token exists for client! $existingToken")
                return@withContext
            }
            tokenDao.insert(token)
        }
    }

    suspend fun update(token: AirPowerToken) {
        if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "update() token: $token")
        require(!token.jwt.isNullOrEmpty() && !token.refreshToken.isNullOrEmpty() && token.client != null) {
            "Token info is null or empty!"
        }

        withContext(Dispatchers.IO) {
            val existingToken = tokenDao.getTokenByClient(token.client)
            existingToken?.let {
                it.jwt = token.jwt
                it.refreshToken = token.refreshToken
                it.scope = token.scope
                tokenDao.update(it)
            } ?: AirPowerLog.e(TAG, "update(): Can't update token. Token does not exist")
        }
    }

    fun writeString(key: String, value: String) {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "writeString key:$key value:$value")
        spManager.writeString(key, value)
    }

    fun readString(key: String): String {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "readString key:$key")
        return spManager.readString(key)
    }

    private fun isUserValid(user: ThingsBoardUser): Boolean {
        return when {
            user.authority.isEmpty() -> {
                AirPowerLog.d(TAG, "User authority is null or empty")
                false
            }

            user.customerId.id == null -> {
                AirPowerLog.d(TAG, "Customer ID is null or empty")
                false
            }

            else -> true
        }
    }

    private fun ThingsBoardUser.toAirPowerUser(): AirPowerUser {
        return AirPowerUser(
            id = id.id.toString(),
            authority = authority,
            email = email,
            customerId = customerId.id.toString(),
            tenantId = tenantId.id.toString(),
            firstName = firstName,
            lastName = lastName,
            name = name,
            phone = phone
        )
    }

    /**
     * Recupera o usuário atualmente logado do banco de dados local.
     *
     * Este método consulta a tabela de usuários e retorna o primeiro usuário encontrado.
     * A lógica assume que no máximo um usuário pode estar logado e persistido no
     * banco de dados por vez.
     *
     * @return Uma instância de [AirPowerUser] se um usuário for encontrado no banco de dados,
     *         ou `null` se nenhum usuário estiver logado (ou seja, o banco de dados de usuários está vazio).
     */
    private fun getCurrentUser(): AirPowerUser? {
        val allUsers = userDao.findAll()
        return if (allUsers.isEmpty()) {
            null
        } else {
            allUsers[0]
        }
    }

    fun isUserLoggedIn(): Boolean {
        return userDao.findAll().size == 1
    }

    fun getDeviceById(id: String): DeviceSummary {
        devicesSummary.value?.forEach { devicesSummary ->
            if (AirPowerLog.ISLOGABLE)
                AirPowerLog.e(TAG, "[$TAG]: devicesSummary:$devicesSummary   id: $id")
            if (devicesSummary.id.toString() == id) {
                return devicesSummary
            }
        }
        if (AirPowerLog.ISLOGABLE)
            AirPowerLog.e(TAG, "[$TAG]: Exception: -> device not found")
        throw NotFoundException("[$TAG]: Exception: -> device not found")
    }

    suspend fun fetchAllDashboardsMetricsWrapper(): ResultWrapper<List<AllMetricsWrapper>> {
        if (AirPowerLog.ISLOGABLE)
            AirPowerLog.d(TAG, "fetchAllDashboardsMetricsWrapper()")
        val resultWrapper =
            airPowerServerMgr.getDevicesMetricsWrapper(Constants.MetricsGroup.DASHBOARDS)
        if (resultWrapper is ResultWrapper.Success) {
            _dashBoardsMetricsWrapper.value = resultWrapper.value
        }
        return resultWrapper
    }

    private fun getEmptyAllDevicesMetricsWrapper(): AllMetricsWrapper {
        return AllMetricsWrapper(
            totalConsumption = "",
            devicesCount = 0,
            label = "",
            deviceConsumptionSet = listOf(
                DeviceConsumption("", 0.0)
            ),
            statusSummaries = listOf(
                DevicesStatusSummary("", 0)
            )
        )
    }

    fun getNotifications(): StateFlow<List<AirPowerNotificationItem>> {
        return notification
    }

    private fun getEmptyNotification(): List<AirPowerNotificationItem> {
        return emptyList()
    }

    private fun getEmptyDashboards(): List<DashboardInfo> {
        return emptyList()
    }

    suspend fun retrieveNotifications(): ResultWrapper<List<AirPowerNotificationItem>> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "retrieveNotifications()")
        val resultWrapper = airPowerServerMgr.getNotificationsForCurrentUser()
        if (resultWrapper is ResultWrapper.Success) {
            if (AirPowerLog.ISVERBOSE)
                AirPowerLog.d(
                    TAG,
                    "Updating notifications data with ${resultWrapper.value.size} items."
                )
            val oldNotifications = getNotifications().value
            val newNotifications = resultWrapper.value
            if (oldNotifications.isNotEmpty() && newNotifications.size > oldNotifications.size) {
                val notificationsToPost =
                    newNotifications.filter { it !in oldNotifications }
                notificationsToPost.forEach { notificationItem ->
                    notificationHelper.showNotification(notificationItem)
                }
            }
            _notification.value = resultWrapper.value
        }
        return resultWrapper
    }

    suspend fun markNotificationAsRead(notificationId: Id): ResultWrapper<Boolean> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "markNotificationAsRead()")
        val resultWrapper = airPowerServerMgr.markNotificationAsRead(notificationId)
        if (resultWrapper is ResultWrapper.Success) {
            if (AirPowerLog.ISVERBOSE)
                AirPowerLog.d(TAG, "Updating notifications state")
            _notification.value = _notification.value.map { notification ->
                if (notification.id.id == notificationId.id) {
                    notification.copy(status = Constants.NotificationState.READ)
                } else {
                    notification
                }
            }
        }
        return resultWrapper
    }

    fun getDashBoards(): StateFlow<List<DashboardInfo>> {
        return dashboards
    }

    suspend fun retrieveDashBoardsForCurrentUser(): ResultWrapper<List<DashboardInfo>> {
        if (AirPowerLog.ISLOGABLE)
            AirPowerLog.d(TAG, "retrieveDashBoardsForCurrentUser()")
        val storedUser = getCurrentUser()
        if (storedUser == null) {
            return ResultWrapper.NetworkError
        }
        val resultWrapper = airPowerServerMgr.getDashBoardsForUser(storedUser.id)
        if (resultWrapper is ResultWrapper.Success) {
            if (AirPowerLog.ISVERBOSE)
                AirPowerLog.d(TAG, "getting dashboards: ${resultWrapper.value.size} items.")
            _dashboards.value = resultWrapper.value
        }
        return resultWrapper
    }

    @Deprecated("Marked to be removed")
    suspend fun getDeviceIdsFromDashboards(dashboardId: String): ResultWrapper<List<String>> {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "getDeviceIdsFromDashboard()")
        val resultWrapper = airPowerServerMgr.getDeviceIdsFromDashboard(dashboardId)
        return resultWrapper
    }

    fun removeReadNotification(notificationId: Id) {
        if (AirPowerLog.ISLOGABLE)
            AirPowerLog.d(TAG, "Removing notification with id: ${notificationId.id}")
        _notification.value = _notification.value.filter { notification ->
            notification.id.id != notificationId.id
        }
    }

    suspend fun registerDevice(device: DeviceRegistration): ResultWrapper<ThingsBoardDevice> {
        return try {
            val tokenObject = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_AIR_POWER_SERVER)
            val tokenString = tokenObject?.token

            if (tokenString.isNullOrEmpty()) {
                return ResultWrapper.ApiError(ErrorCode.AP_JWT_EXPIRED)
            }

            val service = thingsBoardManager.getService(tokenString)
            val result = service.registerDevice(device)
            ResultWrapper.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun getDeviceCredentials(deviceId: String): ResultWrapper<DeviceCredentials> {
        return try {
            val tokenObject = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_AIR_POWER_SERVER)
            val tokenString = tokenObject?.token

            if (tokenString.isNullOrEmpty()) {
                return ResultWrapper.ApiError(ErrorCode.AP_JWT_EXPIRED)
            }

            val service = thingsBoardManager.getService(tokenString)
            val result = service.getDeviceCredentials(deviceId)
            ResultWrapper.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun authenticateDirect(user: AuthUser): ResultWrapper<Token> {
        return try {
            val service = thingsBoardManager.getService(null)

            // Agora LoginRequest existe
            val request = LoginRequest(user.username, user.password)
            val response = service.login(request)

            val token = Token(
                token = response.token,
                refreshToken = response.refreshToken,
                scope = response.scope ?: "TENANT_ADMIN" // Fallback seguro
            )

            JWTManager.handleAuthentication(
                Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD,
                token
            ) { }

            // Salva também no ID 2 para garantir compatibilidade
            JWTManager.handleAuthentication(
                Constants.ServerConnectionIds.CONNECTION_ID_AIR_POWER_SERVER,
                token
            ) { }

            ResultWrapper.Success(token)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is retrofit2.HttpException && e.code() == 401) {
                ResultWrapper.ApiError(ErrorCode.TB_INVALID_CREDENTIALS)
            } else {
                ResultWrapper.NetworkError
            }
        }
    }

    suspend fun retrieveCurrentUserDirect(): ResultWrapper<AirPowerUser> {
        return try {
            val tokenObject = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)
            val tokenString = tokenObject?.token

            if (tokenString.isNullOrEmpty()) {
                return ResultWrapper.ApiError(ErrorCode.AP_JWT_EXPIRED)
            }

            val service = thingsBoardManager.getService(tokenString)
            val tbUser = service.getUser()

            val localUser = AirPowerUser(
                id = tbUser.id.id.toString(), // Converte UUID para String
                authority = tbUser.authority,
                email = tbUser.email,
                firstName = tbUser.firstName,
                lastName = tbUser.lastName,
                name = tbUser.name,
                // Verifica nulidade e converte UUID para String
                customerId = tbUser.customerId?.id?.toString() ?: "",
                tenantId = tbUser.tenantId?.id?.toString() ?: ""
            )

            // Salva no banco
            userDao.insert(localUser)

            // Atualiza a variável local
            cachedAuthority = localUser.authority

            ResultWrapper.Success(localUser)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    suspend fun getTenantDevices(): ResultWrapper<List<ThingsBoardDevice>> {
        return try {
            val token = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)?.token
            val service = thingsBoardManager.getService(token)

            // Chama a API real
            val pageData = service.getTenantDevices()

            // Retorna apenas a lista de dados
            ResultWrapper.Success(pageData.data)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }
    suspend fun saveDeviceLocation(deviceId: String, lat: Double, lng: Double): ResultWrapper<Boolean> {
        return try {
            val token = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)?.token
            val service = thingsBoardManager.getService(token)

            // Cria o JSON manualmente para garantir o formato correto
            // Ex: {"latitude": -8.05, "longitude": -34.88}
            val jsonObject = JsonObject()
            jsonObject.addProperty("latitude", lat)
            jsonObject.addProperty("longitude", lng)

            val response = service.saveDeviceTelemetry(deviceId, jsonObject)

            if (response.isSuccessful) {
                ResultWrapper.Success(true)
            } else {
                // Log para debug se falhar de novo
                AirPowerLog.e("Repository", "Erro ao salvar GPS: ${response.code()} - ${response.errorBody()?.string()}")
                ResultWrapper.GenericError(response.code(), "Erro ao salvar local")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultWrapper.NetworkError
        }
    }

    // Função para buscar Latitude e Longitude de um dispositivo específico
    suspend fun getDeviceLocation(deviceId: String): ResultWrapper<Pair<Double, Double>?> {
        return try {
            val token = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)?.token
            val service = thingsBoardManager.getService(token)

            val response = service.getDeviceTelemetry(deviceId)

            if (response.isSuccessful) {
                val telemetryMap = response.body()

                if (telemetryMap != null) {
                    try {
                        val latElement = telemetryMap.get("latitude")
                        val lonElement = telemetryMap.get("longitude")

                        if (latElement != null && latElement.isJsonArray && latElement.asJsonArray.size() > 0 &&
                            lonElement != null && lonElement.isJsonArray && lonElement.asJsonArray.size() > 0) {

                            // Pegamos o objeto de valor bruto primeiro
                            val latVal = latElement.asJsonArray.get(0).asJsonObject.get("value")
                            val lonVal = lonElement.asJsonArray.get(0).asJsonObject.get("value")

                            // VERIFICAÇÃO DE SEGURANÇA: Só tenta converter se NÃO for nulo!
                            if (!latVal.isJsonNull && !lonVal.isJsonNull) {
                                val latStr = latVal.asString
                                val lonStr = lonVal.asString

                                val lat = latStr.toDoubleOrNull()
                                val lon = lonStr.toDoubleOrNull()

                                if (lat != null && lon != null) {
                                    return ResultWrapper.Success(Pair(lat, lon)) // SUCESSO! Tem pino!
                                }
                            }
                        }
                        // Se for nulo ou vazio, ignora em silêncio
                        return ResultWrapper.Success(null)

                    } catch (e: Exception) {
                        AirPowerLog.e("Repository", "Erro ao processar JSON: ${e.message}")
                        return ResultWrapper.Success(null)
                    }
                } else {
                    return ResultWrapper.Success(null)
                }
            } else {
                return ResultWrapper.Success(null)
            }
        } catch (e: Exception) {
            AirPowerLog.e("Repository", "Erro na requisição de localização: ${e.message}")
            return ResultWrapper.Success(null)
        }
    }

    suspend fun deleteDevice(deviceId: String): ResultWrapper<Boolean> {
        return try {
            val token = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)?.token
            val service = thingsBoardManager.getService(token)

            val response = service.deleteDevice(deviceId)
            if (response.isSuccessful) {
                ResultWrapper.Success(true)
            } else {
                AirPowerLog.e("Repository", "Erro ao deletar: ${response.code()}")
                ResultWrapper.GenericError(response.code(), "Erro ao excluir dispositivo")
            }
        } catch (e: Exception) {
            AirPowerLog.e("Repository", "Erro na exclusão: ${e.message}")
            ResultWrapper.NetworkError
        }
    }
    // Retorna um Pair: O primeiro mapa é a Telemetria, o segundo são os Atributos
    suspend fun getDeviceDetails(deviceId: String): ResultWrapper<Pair<Map<String, String>, Map<String, String>>> {
        return try {
            val token = JWTManager.getTokenForConnectionId(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)?.token
            val service = thingsBoardManager.getService(token)

            val telemetryMap = mutableMapOf<String, String>()
            val attributesMap = mutableMapOf<String, String>()

            // 1. Busca Telemetria
            val teleResponse = service.getAllDeviceTelemetry(deviceId)
            if (teleResponse.isSuccessful) {
                val body = teleResponse.body()
                if (body != null) {
                    body.keySet().forEach { key ->
                        val array = body.getAsJsonArray(key)
                        if (array != null && array.size() > 0) {
                            val value = array.get(0).asJsonObject.get("value").asString
                            telemetryMap[key] = value
                        }
                    }
                }
            }

            // 2. Busca Atributos
            val attrResponse = service.getDeviceAttributes(deviceId)
            if (attrResponse.isSuccessful) {
                val body = attrResponse.body()
                if (body != null && body.isJsonArray) {
                    body.asJsonArray.forEach { element ->
                        val obj = element.asJsonObject
                        val key = obj.get("key").asString
                        val value = obj.get("value").asString
                        attributesMap[key] = value
                    }
                }
            }

            ResultWrapper.Success(Pair(telemetryMap, attributesMap))

        } catch (e: Exception) {
            AirPowerLog.e("Repository", "Erro ao buscar detalhes: ${e.message}")
            ResultWrapper.Success(Pair(emptyMap(), emptyMap())) // Retorna vazio em vez de crashar
        }
    }

    fun getCachedUserAuthority(): String {
        return cachedAuthority
    }

    // Busca a autoridade diretamente do banco de dados (ideal para a SplashScreen)
    fun getLoggedUserAuthoritySync(): String? {
        val allUsers = userDao.findAll()
        return if (allUsers.isNotEmpty()) {
            allUsers[0].authority
        } else {
            null
        }
    }

    // MOCK DA FUTURA API DE REDES WI-FI
    suspend fun getPreRegisteredNetworks(): List<AllowedNetwork> {
        // Simula o tempo de resposta da internet
        kotlinx.coroutines.delay(1000)

        // Mude os SSIDs abaixo para o nome real da rede da sua casa/laboratório para conseguir testar!
        return listOf(
            AllowedNetwork("GPSERS_IoT2", "A2024@GPSERS", "Rede 2 Laboratório Dexter/GPSERS"),
            AllowedNetwork("AlunosGPSERS2024", "GPSERSSTUDENTS2024", "Rede Estudantes 2024"),
            AllowedNetwork("Capricche-Industria", "Cr@cker@2023", "Rede Capricche"),
            AllowedNetwork("GPSERS_IoT", "A2024@GPSERS", "Rede 1 Laboratório Dexter/GPSERS"),
            AllowedNetwork("AlunosGPSERS2023", "GPSERSSTUDENTS2023", "Rede Estudantes 2023"),
            AllowedNetwork("IFPE/RECIFE", "ifpe@082021", "Rede Publica IFPE")
        )
    }
}