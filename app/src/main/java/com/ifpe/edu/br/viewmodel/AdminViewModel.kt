package com.ifpe.edu.br.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.Constants.Constants.THINGS_BOARD_BASE_URL
import com.ifpe.edu.br.model.provisioning.EspConfiguration
import com.ifpe.edu.br.model.provisioning.EspProvisioningManager
import com.ifpe.edu.br.model.repository.Repository
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.util.ResultWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository.getInstance()
    private val provisioningManager = EspProvisioningManager()

    // --- ESTADOS DA UI ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _devicesList = MutableStateFlow<List<ThingsBoardDevice>>(emptyList())
    val devicesList = _devicesList.asStateFlow()

    private val _provisioningStatus = MutableStateFlow("Aguardando...")
    val provisioningStatus = _provisioningStatus.asStateFlow()

    // Dispositivo selecionado ou criado para instalação
    private val _selectedDevice = MutableStateFlow<ThingsBoardDevice?>(null)

    // Mapa que vincula ID do Dispositivo -> Coordenada Real
    private val _deviceLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val deviceLocations = _deviceLocations.asStateFlow()
    val selectedDevice = _selectedDevice.asStateFlow()

    // Dados temporários da instalação
    var targetWifiSsid = ""
    var targetWifiPassword = ""
    var targetEspId = "" // ID que a ESP envia para validação
    var currentToken = ""

    // Cliente de GPS
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // --- ESTADOS PARA DETALHES E EXCLUSÃO ---
    private val _selectedDeviceTelemetry = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedDeviceTelemetry = _selectedDeviceTelemetry.asStateFlow()

    private val _selectedDeviceAttributes = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedDeviceAttributes = _selectedDeviceAttributes.asStateFlow()

    // --- 1. LISTAGEM DE DISPOSITIVOS + LOCALIZAÇÃO ---
    fun fetchDevices() {
        viewModelScope.launch {
            val result = repository.getTenantDevices()

            if (result is ResultWrapper.Success && result.value != null) {
                // A MÁGICA DE FORÇAR O COMPOSE A REDESENHAR ESTÁ AQUI:
                // Em vez de só .toList(), nós mapeamos (clonamos) a lista inteira
                val novaListaForcada = result.value.map { device ->
                    device.copy() // Força a criação de um novo objeto na memória
                }

                _devicesList.value = novaListaForcada
            }
        }
    }

    private fun fetchLocationsForDevices(devices: List<ThingsBoardDevice>) {
        viewModelScope.launch {
            // Em vez de esperar por todos, processamos um por um
            devices.forEach { device ->
                val locResult = repository.getDeviceLocation(device.id.id)
                if (locResult is ResultWrapper.Success && locResult.value != null) {
                    val (lat, lon) = locResult.value!!
                    // Filtra coordenadas zeradas ou inválidas
                    if (lat != 0.0 && lon != 0.0) {
                        // ATUALIZA A TELA IMEDIATAMENTE (Pino a Pino)
                        val currentMap = _deviceLocations.value.toMutableMap()
                        currentMap[device.id.id] = LatLng(lat, lon)
                        _deviceLocations.value = currentMap // O Compose vai desenhar o pino na hora!
                    }
                }
            }
        }
    }

    // --- 2. LÓGICA DE LOCALIZAÇÃO (REAL) ---
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

    // --- 3. CRIAÇÃO DE DISPOSITIVO ---
    fun createDevice(name: String, type: String = "ESP32", location: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Adicionamos a localização no Label ou AdditionalInfo
            val deviceDto = DeviceRegistration(name = name, type = type, label = location)
            val result = repository.registerDevice(deviceDto)

            if (result is ResultWrapper.Success) {
                _selectedDevice.value = result.value
                fetchCredentials(result.value.id.id)
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

    // --- 4. ENVIO PARA O SOCKET + GPS (LÓGICA FINAL) ---
    // Variável para armazenar o ID digitado na UI
    var targetEspIdInput: String = ""

    fun sendConfigurationToEsp() {
        if (currentToken.isEmpty()) {
            _provisioningStatus.value = "Erro: Token não gerado."
            return
        }

        // Validação simples
        if (targetEspIdInput.isBlank()) {
            _provisioningStatus.value = "Erro: Digite o ID da ESP32 para validar."
            return
        }

        // Extrai apenas o IP/Host da URL base
        val cleanHost = java.net.URI(THINGS_BOARD_BASE_URL).host ?: "10.0.0.1" // Fallback seguro

        val config = EspConfiguration(
            targetId = targetEspId,
            targetSsid = targetWifiSsid,
            targetPassword = targetWifiPassword,
            deviceToken = currentToken,
            serverUrl = cleanHost, // Envia só o IP (ex: 192.168.1.10)
            serverPort = 1883, // Porta MQTT padrão (não HTTP 8080)
            location = ""
        )

        viewModelScope.launch {
            provisioningManager.startServer(
                config = config,
                expectedEspId = targetEspIdInput,
                callback = object : EspProvisioningManager.ProvisioningCallback {
                    override fun onStatusChanged(status: String) {
                        _provisioningStatus.value = status
                    }
                    override fun onError(error: String) {
                        _provisioningStatus.value = error
                    }
                    override fun onSuccess() {
                        _provisioningStatus.value = "SUCESSO_SOCKET" // Usamos uma flag para a UI saber
                    }
                }
            )
        }
    }

    // Tornamos esta função pública para ser chamada pelo botão na UI
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
                            _provisioningStatus.value = "Erro ao enviar para ThingsBoard. Verifique sua internet."
                        }
                    }
                } else {
                    _provisioningStatus.value = "Erro: GPS sem sinal."
                }
            }.addOnFailureListener { e ->
                _provisioningStatus.value = "Erro GPS: ${e.message}"
            }
        } catch (e: SecurityException) {
            _provisioningStatus.value = "Erro: Sem permissão de GPS."
        } catch (e: Exception) {
            _provisioningStatus.value = "Erro genérico: ${e.message}"
        }
    }

    // Limpa os estados para permitir um novo provisionamento
    fun resetUIState(key: String? = null) {
        _provisioningStatus.value = "Aguardando início..."
        _selectedDevice.value = null
        _isLoading.value = false
        // Se tiver outras variáveis de estado para limpar, adicione aqui
    }

    // Função para buscar dados ao clicar no Card
    fun fetchDeviceDetails(deviceId: String) {
        // Limpa os antigos enquanto carrega
        _selectedDeviceTelemetry.value = emptyMap()
        _selectedDeviceAttributes.value = emptyMap()

        viewModelScope.launch {
            val result = repository.getDeviceDetails(deviceId)
            if (result is ResultWrapper.Success) {
                _selectedDeviceTelemetry.value = result.value.first
                _selectedDeviceAttributes.value = result.value.second
            }
        }
    }

    // Função para excluir
    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteDevice(deviceId)
            if (result is ResultWrapper.Success) {
                // Remove o dispositivo da lista atual na tela
                val currentList = _devicesList.value.toMutableList()
                currentList.removeAll { it.id.id == deviceId }
                _devicesList.value = currentList
            }
            _isLoading.value = false
        }
    }

    fun stopSocket() {
        provisioningManager.stopServer()
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}