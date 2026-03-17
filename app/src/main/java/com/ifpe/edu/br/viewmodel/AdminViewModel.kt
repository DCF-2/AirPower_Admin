package com.ifpe.edu.br.viewmodel

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

    // --- ESTADOS DA UI ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _devicesList = MutableStateFlow<List<ThingsBoardDevice>>(emptyList())
    val devicesList = _devicesList.asStateFlow()

    private val _provisioningStatus = MutableStateFlow("Aguardando...")
    val provisioningStatus = _provisioningStatus.asStateFlow()

    private val _selectedDevice = MutableStateFlow<ThingsBoardDevice?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    private val _deviceLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val deviceLocations = _deviceLocations.asStateFlow()

    // Dados temporários da instalação
    var targetWifiSsid = ""
    var targetWifiPassword = ""
    var targetEspId = ""
    var currentToken = ""
    var targetEspIdInput: String = ""

    // Cliente de GPS
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // --- DESCOBERTA DE ESP32 ---
    private val _showEspSelection = MutableStateFlow(false)
    val showEspSelection = _showEspSelection.asStateFlow()

    private val _isSearchingEsps = MutableStateFlow(false)
    val isSearchingEsps = _isSearchingEsps.asStateFlow()

    private val _discoveredEsps = MutableStateFlow<List<DiscoveredEsp>>(emptyList())
    val discoveredEsps = _discoveredEsps.asStateFlow()

    private var udpSocket: DatagramSocket? = null
    private var isListeningUdp = false
    private val UDP_PORT = 8888

    // --- VARREDURA WI-FI ---
    private val _availableNetworks = MutableStateFlow<List<AllowedNetwork>>(emptyList())
    val availableNetworks = _availableNetworks.asStateFlow()

    private val _isScanningWifi = MutableStateFlow(false)
    val isScanningWifi = _isScanningWifi.asStateFlow()

    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

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

                    _provisioningStatus.value = if (matchedNetworks.isEmpty()) "Nenhuma rede registrada próxima." else "Redes encontradas! Escolha uma."
                } else {
                    _provisioningStatus.value = "Erro ao buscar redes do servidor."
                }

            } catch (e: SecurityException) {
                _provisioningStatus.value = "Erro de segurança ao ler Wi-Fi."
            } finally {
                _isScanningWifi.value = false
            }
        }
    }

    fun selectNetworkAndProceed(network: AllowedNetwork) {
        this.targetWifiSsid = network.ssid
        this.targetWifiPassword = network.password
        _provisioningStatus.value = "Rede ${network.ssid} selecionada."
    }

    fun openEspSelectionModal() {
        _showEspSelection.value = true
        startUdpListener()
    }

    fun closeEspSelectionModal() {
        _showEspSelection.value = false
        stopUdpListener()
    }

    private fun startUdpListener() {
        _isSearchingEsps.value = true
        isListeningUdp = true
        _discoveredEsps.value = emptyList()

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

    private fun stopUdpListener() {
        isListeningUdp = false
        udpSocket?.close()
        _isSearchingEsps.value = false
    }

    fun proceedToNetworkModule(esp: DiscoveredEsp) {
        closeEspSelectionModal()
        this.targetEspId = esp.id
        this.targetEspIdInput = esp.id
        _provisioningStatus.value = "ESP selecionada. Aguardando escolha do Wi-Fi..."
    }

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

    fun createDevice(name: String, type: String = "ESP32", location: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val deviceDto = DeviceRegistration(name = name, type = type, label = location)
            val result = repository.registerDevice(deviceDto)

            if (result is ResultWrapper.Success) {
                _selectedDevice.value = result.value
                fetchCredentials(result.value.id.id)
                openEspSelectionModal()
            } else {
                _provisioningStatus.value = "Erro ao criar dispositivo no Servidor."
            }
            _isLoading.value = false
        }
    }

    private fun fetchCredentials(deviceId: String) {
        viewModelScope.launch {
            val result = repository.getDeviceCredentials(deviceId)
            if (result is ResultWrapper.Success) {
                currentToken = result.value.credentialsId ?: ""
            }
        }
    }

    fun sendConfigurationToEsp() {
        if (currentToken.isEmpty()) {
            _provisioningStatus.value = "Erro: Token não gerado."
            return
        }

        if (targetEspIdInput.isBlank()) {
            _provisioningStatus.value = "Erro: Digite o ID da ESP32 para validar."
            return
        }

        // Lê a URL do Proxy dinâmica!
        val proxyUrl = prefs.proxyBaseUrl
        val cleanHost = java.net.URI(proxyUrl.ifEmpty { "http://10.0.0.1" }).host ?: "10.0.0.1"

        val config = EspConfiguration(
            targetId = targetEspId,
            targetSsid = targetWifiSsid,
            targetPassword = targetWifiPassword,
            deviceToken = currentToken,
            serverUrl = cleanHost,
            serverPort = 1883,
            location = ""
        )

        viewModelScope.launch {
            provisioningManager.startServer(
                config = config,
                expectedEspId = targetEspIdInput,
                callback = object : EspProvisioningManager.ProvisioningCallback {
                    override fun onStatusChanged(status: String) { _provisioningStatus.value = status }
                    override fun onError(error: String) { _provisioningStatus.value = error }
                    override fun onSuccess() { _provisioningStatus.value = "SUCESSO_SOCKET" }
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun saveLocationToThingsBoard() {
        val deviceId = _selectedDevice.value?.id?.id

        if (deviceId == null) {
            _provisioningStatus.value = "Erro: Dispositivo não selecionado."
            return
        }

        _provisioningStatus.value = "Obtendo GPS..."

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    _provisioningStatus.value = "Enviando para nuvem..."
                    viewModelScope.launch {
                        val result = repository.saveDeviceLocation(deviceId, location.latitude, location.longitude)
                        if (result is ResultWrapper.Success) {
                            _provisioningStatus.value = "FINALIZADO: Configurado e Localizado!"
                        } else {
                            _provisioningStatus.value = "Erro ao enviar localização."
                        }
                    }
                } else {
                    _provisioningStatus.value = "Erro: GPS sem sinal."
                }
            }
        } catch (e: SecurityException) {
            _provisioningStatus.value = "Erro: Sem permissão de GPS."
        }
    }

    fun resetUIState(key: String? = null) {
        _provisioningStatus.value = "Aguardando início..."
        _selectedDevice.value = null
        _isLoading.value = false
    }

    fun stopSocket() {
        provisioningManager.stopServer()
    }

    fun logout() {
        viewModelScope.launch {
            JWTManager.resetTokenForConnection(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)
        }
    }

    // --- LÓGICA DE LOCALIZAÇÃO (Módulo 1 do Wizard) ---
    fun checkLocationAndFindDevice(location: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Busca dados frescos do servidor
            val result = repository.getTenantDevices()

            if (result is ResultWrapper.Success) {
                val devices = result.value
                _devicesList.value = devices // Atualiza o cache local

                // Procura na lista real se o nome ou label bate com a sala digitada
                val found = devices.find { device ->
                    device.name.contains(location, ignoreCase = true) ||
                            (device.label != null && device.label.contains(location, ignoreCase = true))
                }

                if (found != null) {
                    _selectedDevice.value = found
                    fetchCredentials(found.id.id)
                } else {
                    _selectedDevice.value = null
                }
            }
            _isLoading.value = false
        }
    }

    // --- TESTE DE PISCAR LED (UDP) ---
    fun testBlinkLed(ipAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Envia o comando UDP real para a placa específica
                val socket = java.net.DatagramSocket()
                val message = "{\"comando\":\"blink\"}".toByteArray()
                val address = java.net.InetAddress.getByName(ipAddress)
                val packet = java.net.DatagramPacket(message, message.size, address, UDP_PORT)
                socket.send(packet)
                socket.close()

                // 2. Faz o efeito visual na UI
                withContext(Dispatchers.Main) {
                    val currentList = _discoveredEsps.value.toMutableList()
                    val index = currentList.indexOfFirst { it.ip == ipAddress }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(isBlinking = true)
                        _discoveredEsps.value = currentList

                        delay(3000) // Mantém a lâmpada acesa no App por 3 seg

                        currentList[index] = currentList[index].copy(isBlinking = false)
                        _discoveredEsps.value = currentList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- REGISTRO DE NOVO ADMIN ---
    fun registerNewAdmin(name: String, email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            // O DTO já injeta o "TENANT_ADMIN" por padrão
            val request = com.ifpe.edu.br.model.repository.remote.dto.auth.RegisterRequest(
                name = name,
                email = email,
                password = pass
            )

            val result = repository.registerUser(request)
            _isLoading.value = false

            if (result is ResultWrapper.Success) {
                // Sucesso! Dispara o callback para mostrar o aviso na tela
                onResult(true, "Cadastro realizado com sucesso!\nAguarde a aprovação do Super Administrador para fazer login.")
            } else if (result is ResultWrapper.ApiError) {
                // Tratamento elegante de erro do card
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