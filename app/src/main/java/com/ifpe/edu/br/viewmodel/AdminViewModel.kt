package com.ifpe.edu.br.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.provisioning.AllowedNetwork
import com.ifpe.edu.br.model.provisioning.DiscoveredEsp
import com.ifpe.edu.br.model.provisioning.EspConfiguration
import com.ifpe.edu.br.model.provisioning.EspProvisioningManager
import com.ifpe.edu.br.model.repository.AdminRepository
import com.ifpe.edu.br.model.repository.persistence.manager.JWTManager
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.util.ResultWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    application: Application,
    val repository: AdminRepository,
    private val prefs: SharedPrefManager,
) : AndroidViewModel(application) {

    private val provisioningManager = EspProvisioningManager()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // --- ESTADOS GERAIS DA UI ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _devicesList = MutableStateFlow<List<ThingsBoardDevice>>(emptyList())
    val devicesList = _devicesList.asStateFlow()

    // --- ESTADOS DO MAPA ---
    private val _deviceLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val deviceLocations = _deviceLocations.asStateFlow()

    // ========================================================================
    // MÁQUINA DE ESTADOS: PROGRESSIVE WIZARD (SETUP)
    // ========================================================================

    private val _wizardStep = MutableStateFlow(1)
    val wizardStep = _wizardStep.asStateFlow()

    private val _availableNetworks = MutableStateFlow<List<AllowedNetwork>>(emptyList())
    val availableNetworks = _availableNetworks.asStateFlow()

    private val _isScanningWifi = MutableStateFlow(false)
    val isScanningWifi = _isScanningWifi.asStateFlow()

    var selectedNetwork: AllowedNetwork? = null
    var targetWifiSsid = ""
    var targetWifiPassword = ""

    private val _selectedDevice = MutableStateFlow<ThingsBoardDevice?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    var locationName: String = ""
    var locationLat: Double? = null
    var locationLon: Double? = null
    var locationDescription: String = ""
    var currentToken = ""

    private val _isSearchingEsps = MutableStateFlow(false)
    val isSearchingEsps = _isSearchingEsps.asStateFlow()

    private val _discoveredEsps = MutableStateFlow<List<DiscoveredEsp>>(emptyList())
    val discoveredEsps = _discoveredEsps.asStateFlow()

    private var udpSocket: DatagramSocket? = null
    private var isListeningUdp = false
    private val UDP_PORT = 8888

    var targetEspId = ""
    private var lockedEspId: String? = null

    private val _provisioningStatus = MutableStateFlow("Aguardando...")
    val provisioningStatus = _provisioningStatus.asStateFlow()

    private val _provisioningLogs = MutableStateFlow<List<String>>(emptyList())
    val provisioningLogs = _provisioningLogs.asStateFlow()

    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _dashboardsList = MutableStateFlow<List<DashboardInfo>>(emptyList())
    val dashboardsList = _dashboardsList.asStateFlow()

    // ========================================================================
    // ESTADOS E FUNÇÕES DO DASHBOARD NATIVO (WEBSOCKETS)
    // ========================================================================

    private val _currentDeviceTelemetry = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentDeviceTelemetry = _currentDeviceTelemetry.asStateFlow()

    private val _devicePowerState = MutableStateFlow(false)
    val devicePowerState = _devicePowerState.asStateFlow()

    private val _telemetryHistory = MutableStateFlow<Map<String, List<Float>>>(emptyMap())
    val telemetryHistory = _telemetryHistory.asStateFlow()

    private val MAX_HISTORY_POINTS = 20

    var connectionError by mutableStateOf(false)
        private set

    var isPollingActive by mutableStateOf(false)
        private set

    init {
        // 🔥 LIGAÇÃO AO WEBSOCKET (O Observador Passivo)
        viewModelScope.launch {
            repository.realTimeTelemetryFlow.collect { jsonString ->
                handleIncomingTelemetry(jsonString)
            }
        }
    }

    /**
     * Esta função processa o texto JSON bruto que o ThingsBoard nos dispara em tempo real.
     */
    private fun handleIncomingTelemetry(jsonString: String) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("data")) return // Ignora mensagens de sistema do TB

            val dataObj = root.getJSONObject("data")
            val currentCardMap = _currentDeviceTelemetry.value.toMutableMap()
            val currentHistory = _telemetryHistory.value.toMutableMap()

            val keys = dataObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val valueArray = dataObj.getJSONArray(key)

                if (valueArray.length() > 0) {
                    // CRÍTICO: ThingsBoard usa Array de Array no WebSocket! Ex: [ [1682390123, "24.5"] ]
                    val dataPoint = valueArray.getJSONArray(0)
                    val rawValueString = dataPoint.getString(1) // Índice 0 é tempo, Índice 1 é valor
                    val rawValueFloat = rawValueString.toFloatOrNull()

                    // Atualiza Estado/Power
                    if (key.equals("state", true) || key.equals("power", true)) {
                        _devicePowerState.value = rawValueString == "true" || rawValueString == "1"
                        currentCardMap["Estado"] = if (_devicePowerState.value) "Ligado" else "Desligado"
                    } else {
                        // Formata a primeira letra para maiúscula
                        val formattedName = key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        currentCardMap[formattedName] = rawValueString

                        // Atualiza o Gráfico Animado
                        if (rawValueFloat != null) {
                            val pointList = currentHistory[formattedName]?.toMutableList() ?: mutableListOf()
                            pointList.add(rawValueFloat)
                            if (pointList.size > MAX_HISTORY_POINTS) pointList.removeAt(0)
                            currentHistory[formattedName] = pointList
                        }
                    }
                }
            }

            // Avisa a UI para redesenhar a tela com os novos dados!
            _currentDeviceTelemetry.value = currentCardMap
            _telemetryHistory.value = currentHistory
            connectionError = false

        } catch (e: Exception) {
            com.ifpe.edu.br.model.util.AirPowerLog.e("ViewModel", "Erro ao processar JSON do TB: ${e.message}")
        }
    }

    // ==========================================
    // MÉTODOS DE CONTROLE (RPC) E OTIMISMO
    // ==========================================
    suspend fun sendDeviceCommand(deviceId: String, method: String, params: Any): Boolean {
        return try {
            val result = repository.sendRpcCommand(deviceId, method, params)
            if (result is ResultWrapper.Success) {
                connectionError = false
                true
            } else {
                connectionError = true
                false
            }
        } catch (e: Exception) {
            connectionError = true
            false
        }
    }

    // Mantemos o Polling APENAS para a Lista de Devices (pois isso não gasta muita bateria)
    fun fetchDevicesDataPeriodically() {
        viewModelScope.launch {
            while (isActive) {
                isPollingActive = true
                val response = repository.getTenantDevices()
                isPollingActive = false

                if (response is ResultWrapper.Success) {
                    connectionError = false
                    _devicesList.value = response.value
                } else {
                    connectionError = true
                }
                delay(5000)
            }
        }
    }

    // Quando clica no Dispositivo, INICIA A CONEXÃO WEBSOCKET
    fun selectDeviceForDashboard(device: ThingsBoardDevice) {
        _selectedDevice.value = device

        // Em vez do antigo fetchDeviceTelemetry(), ativamos a "magia"!
        viewModelScope.launch {
            repository.startRealTimeTelemetry(device.id.id)
        }
    }

    // Mantemos o mesmo nome antigo para a Tela do Dashboard não quebrar,
    // mas agora esta função ativa a magia do WebSocket em tempo real!
    fun fetchDeviceTelemetry(deviceId: String) {
        com.ifpe.edu.br.model.util.AirPowerLog.d("ViewModel", "⚠️ A TELA MANDOU LIGAR O WEBSOCKET PARA O ID: $deviceId")
        viewModelScope.launch {
            repository.startRealTimeTelemetry(deviceId)
        }
    }

    fun sendRpcCommand(deviceId: String, method: String, params: Any) {
        viewModelScope.launch {
            if (method == "setPower" && params is Boolean) {
                _devicePowerState.value = params
            }

            addProvisioningLog("Enviando RPC: $method -> $params")
            val result = repository.sendRpcCommand(deviceId, method, params)

            if (result !is ResultWrapper.Success) {
                addProvisioningLog("Erro ao enviar comando para a placa.")
            }
        }
    }

    // Mata a conexão se o utilizador fechar a App ou o ViewModel morrer
    override fun onCleared() {
        super.onCleared()
        repository.stopRealTimeTelemetry()
    }


    // ========================================================================
    // FUNÇÕES DO WIZARD PROGRESSIVO (Inalteradas)
    // ========================================================================

    fun setWizardStep(step: Int) {
        _wizardStep.value = step
    }

    fun nextStep() {
        if (_wizardStep.value < 6) _wizardStep.value++
    }

    fun previousStep() {
        if (_wizardStep.value > 1) _wizardStep.value--
    }

    fun resetWizard() {
        _wizardStep.value = 1
        selectedNetwork = null
        targetWifiSsid = ""
        targetWifiPassword = ""
        locationName = ""
        locationLat = null
        locationLon = null
        locationDescription = ""
        targetEspId = ""
        currentToken = ""
        unlockEsp32()
        _provisioningLogs.value = emptyList()
        _selectedDevice.value = null
        _availableNetworks.value = emptyList()
        _provisioningStatus.value = "Aguardando início..."
    }

    fun addProvisioningLog(message: String) {
        val currentLogs = _provisioningLogs.value.toMutableList()
        currentLogs.add("> $message")
        _provisioningLogs.value = currentLogs
    }

    fun lockEsp32(espId: String) {
        lockedEspId = espId
        this.targetEspId = espId
        addProvisioningLog("🔒 ESP32 selecionada. Trava UDP ativada para o ID: $espId")
        stopUdpListener()
    }

    fun unlockEsp32() {
        lockedEspId = null
        addProvisioningLog("🔓 Trava UDP libertada.")
    }

    fun scanForAuthorizedNetworks() {
        viewModelScope.launch {
            _isScanningWifi.value = true
            _provisioningStatus.value = "Buscando redes autorizadas..."

            val hasPermission = ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                _provisioningStatus.value = "Erro: Permissão de localização necessária."
                _isScanningWifi.value = false
                return@launch
            }

            try {
                val result = repository.getAuthorizedNetworks()
                wifiManager.startScan()
                delay(1500)
                val scanResults = wifiManager.scanResults
                val matchedNetworks = mutableListOf<AllowedNetwork>()

                if (result is ResultWrapper.Success) {
                    val allowedNetworks = result.value
                    for (allowed in allowedNetworks) {
                        val foundInAir = scanResults.find { it.SSID == allowed.ssid }
                        if (foundInAir != null) {
                            matchedNetworks.add(allowed.copy(signalLevel = foundInAir.level))
                        }
                    }
                    matchedNetworks.sortByDescending { it.signalLevel }
                    _availableNetworks.value = matchedNetworks

                    _provisioningStatus.value = if (matchedNetworks.isEmpty()) "Nenhuma rede da API encontrada próxima." else "Redes autorizadas encontradas!"
                } else {
                    _provisioningStatus.value = "Erro ao buscar redes do servidor Proxy."
                }
            } catch (e: SecurityException) {
                _provisioningStatus.value = "Erro de segurança ao ler Wi-Fi físico."
            } finally {
                _isScanningWifi.value = false
            }
        }
    }

    fun selectNetworkAndProceed(network: AllowedNetwork) {
        this.selectedNetwork = network
        this.targetWifiSsid = network.ssid
        this.targetWifiPassword = network.password
        addProvisioningLog("📡 Wi-Fi definido: ${network.ssid}")
        nextStep()
    }

    @SuppressLint("MissingPermission")
    fun captureLocationInBackground() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    locationLat = location.latitude
                    locationLon = location.longitude
                    addProvisioningLog("📍 GPS Capturado: Lat $locationLat, Lon $locationLon")
                }
            }
        } catch (e: SecurityException) {
            addProvisioningLog("⚠️ Permissão de GPS negada. Localização será ignorada.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveLocationToThingsBoardSilently() {
        val deviceId = _selectedDevice.value?.id?.id ?: return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                locationLat = location.latitude
                locationLon = location.longitude

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        addProvisioningLog("📡 A enviar GPS atual para a Cloud...")
                        repository.saveDeviceLocation(deviceId, locationLat!!, locationLon!!, locationDescription)
                    } catch (e: Exception) {
                        addProvisioningLog("⚠️ Aviso: Falha ao enviar localização.")
                    }
                }
            }
        }
    }

    fun checkLocationAndFindDevice(locationStr: String) {
        this.locationName = locationStr
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getTenantDevices()

            if (result is ResultWrapper.Success) {
                val devices = result.value
                val found = devices.find { it.name.contains(locationStr, ignoreCase = true) || (it.label?.contains(locationStr, ignoreCase = true) == true) }

                if (found != null) {
                    _selectedDevice.value = found
                    fetchCredentials(found.id.id)
                    locationDescription = found.label ?: ""
                    saveLocationToThingsBoardSilently()
                    addProvisioningLog("✅ Dispositivo existente encontrado: ${found.name}")
                } else {
                    _selectedDevice.value = null
                    addProvisioningLog("⚠️ Nenhum dispositivo encontrado para '$locationStr'. Necessário criar novo.")
                }
            }
            _isLoading.value = false
        }
    }

    fun createDevice(name: String, type: String = "ESP32") {
        viewModelScope.launch {
            _isLoading.value = true
            addProvisioningLog("Criando dispositivo '$name' no ThingsBoard...")

            val deviceDto = DeviceRegistration(name = name, type = type, label = locationName)
            val result = repository.registerDevice(deviceDto)

            if (result is ResultWrapper.Success) {
                _selectedDevice.value = result.value
                fetchCredentials(result.value.id.id)
                locationDescription = locationName
                saveLocationToThingsBoardSilently()
                addProvisioningLog("✅ Dispositivo criado com sucesso! ID: ${result.value.id.id}")
                nextStep()
            } else {
                addProvisioningLog("❌ Erro ao criar dispositivo.")
            }
            _isLoading.value = false
        }
    }

    private fun fetchCredentials(deviceId: String) {
        viewModelScope.launch {
            val result = repository.getDeviceCredentials(deviceId)
            if (result is ResultWrapper.Success) {
                currentToken = result.value.credentialsId ?: ""
                addProvisioningLog("🔑 Credenciais ThingsBoard vinculadas com sucesso.")
            }
        }
    }

    fun saveLocationToThingsBoard() {
        val deviceId = _selectedDevice.value?.id?.id ?: return
        if (locationLat == null || locationLon == null) {
            _provisioningStatus.value = "Erro: Coordenadas de GPS inválidas."
            return
        }

        viewModelScope.launch {
            _provisioningStatus.value = "A enviar telemetria para a Cloud..."

            val result = repository.saveDeviceLocation(
                deviceId,
                locationLat!!,
                locationLon!!,
                locationDescription
            )

            if (result is ResultWrapper.Success) {
                _provisioningStatus.value = "TELEMETRIA_SALVA"
            } else {
                _provisioningStatus.value = "Erro ao salvar na Cloud. Verifique sua Internet."
            }
        }
    }

    fun startUdpListener() {
        _isSearchingEsps.value = true
        isListeningUdp = true
        _discoveredEsps.value = emptyList()
        addProvisioningLog("📡 Procurando ESP32s no Hotspot (Porta 8888)...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                udpSocket = DatagramSocket(UDP_PORT)
                udpSocket?.soTimeout = 2000

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isListeningUdp) {
                    try {
                        udpSocket?.receive(packet)
                        val message = String(packet.data, 0, packet.length).trim()
                        val senderIp = packet.address.hostAddress

                        if (message.startsWith("{") && senderIp != null) {
                            val json = JSONObject(message)
                            val espId = json.optString("id", "")

                            if (espId.isNotEmpty()) {
                                if (lockedEspId != null) {
                                    continue
                                }

                                withContext(Dispatchers.Main) {
                                    val currentList = _discoveredEsps.value.toMutableList()
                                    if (currentList.none { it.ip == senderIp }) {
                                        currentList.add(DiscoveredEsp(id = espId, ip = senderIp))
                                        _discoveredEsps.value = currentList
                                    }
                                }
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        continue
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                udpSocket?.close()
                withContext(Dispatchers.Main) { _isSearchingEsps.value = false }
            }
        }
    }

    fun stopUdpListener() {
        isListeningUdp = false
        udpSocket?.close()
        _isSearchingEsps.value = false
    }

    fun testBlinkLed(ipAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                val message = "{\"comando\":\"blink\"}".toByteArray()
                val address = InetAddress.getByName(ipAddress)
                val packet = DatagramPacket(message, message.size, address, UDP_PORT)
                socket.send(packet)
                socket.close()

                withContext(Dispatchers.Main) {
                    val currentList = _discoveredEsps.value.toMutableList()
                    val index = currentList.indexOfFirst { it.ip == ipAddress }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(isBlinking = true)
                        _discoveredEsps.value = currentList
                        delay(3000)
                        currentList[index] = currentList[index].copy(isBlinking = false)
                        _discoveredEsps.value = currentList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendConfigurationToEsp() {
        if (currentToken.isEmpty()) {
            addProvisioningLog("❌ Erro fatal: Token TB não gerado.")
            return
        }

        if (lockedEspId.isNullOrBlank()) {
            addProvisioningLog("❌ Erro fatal: Nenhuma ESP32 está travada (Lock) para receber.")
            return
        }

        addProvisioningLog("🚀 Iniciando provisionamento para ESP: $lockedEspId")

        val rawTbUrl = prefs.readString("LOGGED_USER_TB_URL") ?: prefs.proxyBaseUrl
        val cleanHost = rawTbUrl
            .replace("http://", "")
            .replace("https://", "")
            .substringBefore(":")
            .trim()

        val config = EspConfiguration(
            targetId = lockedEspId!!,
            targetSsid = targetWifiSsid,
            targetPassword = targetWifiPassword,
            deviceToken = currentToken,
            serverUrl = cleanHost
        )

        viewModelScope.launch {
            provisioningManager.startServer(
                config = config,
                expectedEspId = lockedEspId!!,
                callback = object : EspProvisioningManager.ProvisioningCallback {
                    override fun onStatusChanged(status: String) {
                        _provisioningStatus.value = status
                        addProvisioningLog(status)
                    }
                    override fun onError(error: String) {
                        _provisioningStatus.value = error
                        addProvisioningLog("❌ ERRO SOCKET: $error")
                        unlockEsp32()
                    }
                    override fun onSuccess() {
                        _provisioningStatus.value = "SUCESSO_SOCKET"
                        addProvisioningLog("✅ PROVISIONAMENTO CONCLUÍDO COM SUCESSO!")
                    }
                }
            )
        }
    }

    fun stopSocket() {
        provisioningManager.stopServer()
    }

    fun fetchDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getTenantDevices()
            if (result is ResultWrapper.Success && result.value != null) {
                val devices = result.value.map { it.copy() }
                _devicesList.value = devices
                fetchDeviceLocations(devices)
            }
            _isLoading.value = false
        }
    }

    private fun fetchDeviceLocations(devices: List<ThingsBoardDevice>) {
        viewModelScope.launch {
            val locationsMap = mutableMapOf<String, LatLng>()

            for (device in devices) {
                val deviceId = device.id.id

                val telemetryResult = repository.getLatestTelemetry(deviceId, listOf("latitude", "longitude"))

                if (telemetryResult is ResultWrapper.Success) {
                    try {
                        val latList = telemetryResult.value["latitude"]
                        val lngList = telemetryResult.value["longitude"]

                        val latStr = latList?.firstOrNull()?.get("value")?.toString()
                        val lngStr = lngList?.firstOrNull()?.get("value")?.toString()

                        val lat = latStr?.toDoubleOrNull()
                        val lng = lngStr?.toDoubleOrNull()

                        if (lat != null && lng != null) {
                            locationsMap[deviceId] = LatLng(lat, lng)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            _deviceLocations.value = locationsMap
        }
    }

    fun logout() {
        viewModelScope.launch {
            JWTManager.resetTokenForConnection(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)
        }
    }

    fun registerNewAdmin(name: String, email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = com.ifpe.edu.br.model.repository.remote.dto.auth.RegisterRequest(
                name = name, email = email, password = pass
            )
            val result = repository.registerUser(request)
            _isLoading.value = false

            if (result is ResultWrapper.Success) {
                onResult(true, "Cadastro realizado com sucesso!\nAguarde a aprovação do Super Administrador para fazer login.")
            } else if (result is ResultWrapper.ApiError) {
                val errorMsg = when (result.code) {
                    409 -> "Este e-mail já está cadastrado no sistema."
                    400 -> "Dados inválidos. Verifique as informações."
                    else -> "Erro no servidor proxy (${result.code})."
                }
                onResult(false, errorMsg)
            } else {
                onResult(false, "Falha de conexão. Verifique se o servidor Proxy está configurado e online.")
            }
        }
    }

    fun fetchDashboards() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getTenantDashboards()
            if (result is ResultWrapper.Success) {
                _dashboardsList.value = result.value
            }
            _isLoading.value = false
        }
    }

    fun getCleanTbUrl(): String {
        val rawTbUrl = prefs.readString("LOGGED_USER_TB_URL") ?: prefs.proxyBaseUrl
        return rawTbUrl.replace("http://", "").replace("https://", "").substringBefore(":").trim()
    }

    fun getFullTbUrl(): String {
        val rawUrl = prefs.readString("LOGGED_USER_TB_URL") ?: prefs.proxyBaseUrl
        return if (!rawUrl.startsWith("http")) "http://$rawUrl" else rawUrl
    }
}