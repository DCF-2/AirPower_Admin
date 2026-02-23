package com.ifpe.edu.br.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ifpe.edu.br.model.Constants.Constants.THINGS_BOARD_BASE_URL
import com.ifpe.edu.br.model.provisioning.EspConfiguration
import com.ifpe.edu.br.model.provisioning.EspProvisioningManager
import com.ifpe.edu.br.model.repository.Repository
import com.ifpe.edu.br.model.repository.remote.dto.DeviceCredentials
import com.ifpe.edu.br.model.repository.remote.dto.DeviceRegistration
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.model.util.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository.getInstance()

    // Estados da UI
    private val _deviceCreationState = MutableStateFlow<ResultWrapper<ThingsBoardDevice>?>(null)
    val deviceCreationState = _deviceCreationState.asStateFlow()

    private val _credentialsState = MutableStateFlow<ResultWrapper<DeviceCredentials>?>(null)
    val credentialsState = _credentialsState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Dados para envio à ESP32 (Armazenados temporariamente aqui)
    var targetWifiSsid: String = ""
    var targetWifiPassword: String = ""
    var generatedDeviceToken: String = ""

    private val provisioningManager = EspProvisioningManager()

    // Estados do Provisionamento
    private val _provisioningStatus = MutableStateFlow("Parado")
    val provisioningStatus = _provisioningStatus.asStateFlow()

    fun createDevice(name: String, type: String, label: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val deviceDto = DeviceRegistration(name = name, type = type, label = label)

            // 1. Criar Dispositivo
            val result = repository.registerDevice(deviceDto)
            _deviceCreationState.value = result

            if (result is ResultWrapper.Success) {
                // 2. Se criou, busca as credenciais (Token) imediatamente
                fetchCredentials(result.value.id.id)
            } else {
                _isLoading.value = false
            }
        }
    }

    private fun fetchCredentials(deviceId: String) {
        viewModelScope.launch {
            val result = repository.getDeviceCredentials(deviceId)
            _credentialsState.value = result

            if (result is ResultWrapper.Success) {
                // O Token fica em 'credentialsId' para dispositivos do tipo ACCESS_TOKEN (padrão TB)
                generatedDeviceToken = result.value.credentialsId ?: ""
            }
            _isLoading.value = false
        }
    }

    fun resetState() {
        _deviceCreationState.value = null
        _credentialsState.value = null
        _isLoading.value = false
        generatedDeviceToken = ""
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun startProvisioning(targetDeviceId: String?) { // targetDeviceId é opcional (campo de texto "Nome do Dispositivo" que criamos antes pode servir como filtro)
        if (generatedDeviceToken.isEmpty()) {
            _provisioningStatus.value = "Erro: Nenhum token gerado."
            return
        }

        val config = EspConfiguration(
            serverUrl = THINGS_BOARD_BASE_URL, // Usa a constante (http://10.5.0.66:8080)
            targetSsid = targetWifiSsid,
            targetPassword = targetWifiPassword,
            deviceToken = generatedDeviceToken
        )

        viewModelScope.launch {
            _provisioningStatus.value = "Iniciando servidor..."

            provisioningManager.startServer(
                expectedDeviceId = null, // Pode passar 'targetDeviceId' se quiser validar estritamente
                configToSend = config,
                callback = object : EspProvisioningManager.ProvisioningCallback {
                    override fun onStatusChanged(status: String) {
                        _provisioningStatus.value = status
                    }

                    override fun onDeviceConnected(deviceId: String) {
                        _provisioningStatus.value = "Conectado: $deviceId. Enviando dados..."
                    }

                    override fun onError(error: String) {
                        _provisioningStatus.value = "Erro: $error"
                    }

                    override fun onFinished() {
                        _provisioningStatus.value = "Sucesso! ESP32 Reiniciando..."
                    }
                }
            )
        }
    }

    fun stopProvisioning() {
        provisioningManager.stopServer()
        _provisioningStatus.value = "Parado"
    }

    override fun onCleared() {
        super.onCleared()
        stopProvisioning()
    }

}