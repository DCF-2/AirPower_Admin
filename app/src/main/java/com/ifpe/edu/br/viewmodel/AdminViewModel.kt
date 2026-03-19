package com.ifpe.edu.br.viewmodel

/*
* Refactored for: AirPower Admin (BFF Integration, Progressive Wizard & Telemetry)
*/

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
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
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.util.ResultWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository.getInstance(application)
    private val prefs = SharedPrefManager.getInstance(application)
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

    // Controla a tela atual do Wizard (1 a 6)
    private val _wizardStep = MutableStateFlow(1)
    val wizardStep = _wizardStep.asStateFlow()

    // --- PASSO 1: WI-FI (Smart Scan) ---
    private val _availableNetworks = MutableStateFlow<List<AllowedNetwork>>(emptyList())
    val availableNetworks = _availableNetworks.asStateFlow()

    private val _isScanningWifi = MutableStateFlow(false)
    val isScanningWifi = _isScanningWifi.asStateFlow()

    var selectedNetwork: AllowedNetwork? = null
    var targetWifiSsid = ""
    var targetWifiPassword = ""

    // --- PASSO 2: LOCALIZAÇÃO E THINGSBOARD ---
    private val _selectedDevice = MutableStateFlow<ThingsBoardDevice?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    var locationName: String = ""
    var locationLat: Double? = null
    var locationLon: Double? = null
    var locationDescription: String = "" // A variável rebelde está aqui, bem segura!
    var currentToken = ""

    // --- PASSO 4: DESCOBERTA DE ESP32 E LOCK UDP ---
    private val _isSearchingEsps = MutableStateFlow(false)
    val isSearchingEsps = _isSearchingEsps.asStateFlow()

    private val _discoveredEsps = MutableStateFlow<List<DiscoveredEsp>>(emptyList())
    val discoveredEsps = _discoveredEsps.asStateFlow()

    private var udpSocket: DatagramSocket? = null
    private var isListeningUdp = false
    private val UDP_PORT = 8888

    var targetEspId = ""

    // 🔒 TRAVA DE CONCORRÊNCIA (Impede que placas vizinhas interfiram no envio)
    private var lockedEspId: String? = null

    // --- PASSO 5 E 6: VALIDAÇÃO, TERMINAL E ENVIO ---
    private val _provisioningStatus = MutableStateFlow("Aguardando...")
    val provisioningStatus = _provisioningStatus.asStateFlow()

    private val _provisioningLogs = MutableStateFlow<List<String>>(emptyList())
    val provisioningLogs = _provisioningLogs.asStateFlow()

    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // ========================================================================
    // FUNÇÕES DO WIZARD PROGRESSIVO
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

    // --- LÓGICA DE LOCK DA ESP32 (Filtro) ---
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

    // ========================================================================
    // PASSO 1: SMART SCAN WI-FI (API + Físico)
    // ========================================================================
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

    // ========================================================================
    // PASSO 2 E 6: LOCALIZAÇÃO E THINGSBOARD
    // ========================================================================

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
                        val result = repository.saveDeviceLocation(deviceId, locationLat!!, locationLon!!, locationDescription)
                        if (result is ResultWrapper.Success) {
                            addProvisioningLog("✅ Localização salva com sucesso na Cloud!")
                        }
                    } catch (e: Exception) {
                        addProvisioningLog("⚠️ Aviso: Falha ao enviar localização (sem internet?).")
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

    // ========================================================================
    // PASSO 4: DESCOBERTA UDP (COM TRAVA/LOCK)
    // ========================================================================
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

    // ========================================================================
    // PASSO 5: VALIDAÇÃO E ENVIO DO SOCKET (TERMINAL)
    // ========================================================================
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

        // 1. Lê a URL que foi salva no Login
        val rawTbUrl = prefs.readString("LOGGED_USER_TB_URL") ?: prefs.proxyBaseUrl

        // 2. Limpa TUDO e deixa só os números do IP!
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
            serverUrl = cleanHost // A ESP32 vai ligar-se à porta 8080 ou 1883 do Proxy
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
                        // AQUI NÃO HÁ MAIS CHAMADA DE GPS. A PLACA TOMA CONTA DA VIDA DELA!
                    }
                }
            )
        }
    }

    fun stopSocket() {
        provisioningManager.stopServer()
    }

    // ========================================================================
    // OUTRAS FUNÇÕES GERAIS
    // ========================================================================
    fun fetchDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getTenantDevices()
            if (result is ResultWrapper.Success && result.value != null) {
                _devicesList.value = result.value.map { it.copy() }
            }
            _isLoading.value = false
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
}