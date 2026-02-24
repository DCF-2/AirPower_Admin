package com.ifpe.edu.br.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val selectedDevice = _selectedDevice.asStateFlow()

    // Dados temporários da instalação
    var targetWifiSsid = ""
    var targetWifiPassword = ""
    var targetEspId = "" // ID que a ESP envia para validação
    var currentToken = ""

    fun fetchDevices() {
        viewModelScope.launch {
            _isLoading.value = true

            // Chama o repositório
            val result = repository.getTenantDevices()

            if (result is ResultWrapper.Success) {
                _devicesList.value = result.value
            } else {
                // Opcional: Tratar erro
            }
            _isLoading.value = false
        }
    }

    // --- 2. LÓGICA DE LOCALIZAÇÃO (REAL) ---
    fun checkLocationAndFindDevice(location: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // 1. Garante que temos a lista mais atualizada
            val result = repository.getTenantDevices()

            if (result is ResultWrapper.Success) {
                _devicesList.value = result.value
                val devices = result.value

                // 2. Procura Inteligente:
                // Verifica se o 'name' OU o 'label' contêm o nome da sala (Ex: "Sala 101")
                val found = devices.find { device ->
                    device.name.contains(location, ignoreCase = true) ||
                            (device.label != null && device.label.contains(location, ignoreCase = true))
                }

                if (found != null) {
                    _selectedDevice.value = found
                    // Já busca o token desse dispositivo encontrado para agilizar
                    fetchCredentials(found.id.id)
                } else {
                    _selectedDevice.value = null // Não achou, vai liberar o botão "Criar Novo"
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

    // --- 4. ENVIO PARA O SOCKET (FINALIZAÇÃO) ---
    fun sendConfigurationToEsp() {
        if (currentToken.isEmpty()) {
            _provisioningStatus.value = "Erro: Token não gerado."
            return
        }

        // JSON reduzido conforme seu pedido: SSID, SENHA, TOKEN
        val config = EspConfiguration(
            targetSsid = targetWifiSsid,
            targetPassword = targetWifiPassword,
            deviceToken = currentToken,
            // Campos técnicos que o Manager precisa, mas a ESP pode ignorar se não usar
            serverUrl = THINGS_BOARD_BASE_URL,
            serverPort = 8080,
            location = ""
        )

        viewModelScope.launch {
            provisioningManager.startServer(
                config = config,
                callback = object : EspProvisioningManager.ProvisioningCallback {
                    override fun onStatusChanged(status: String) {
                        _provisioningStatus.value = status
                    }
                    override fun onError(error: String) {
                        _provisioningStatus.value = "Erro: $error"
                    }
                    override fun onSuccess() {
                        _provisioningStatus.value = "Configurado com Sucesso!"
                        // Aqui poderíamos resetar o fluxo
                    }
                }
            )
        }
    }

    fun stopSocket() {
        provisioningManager.stopServer()
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}