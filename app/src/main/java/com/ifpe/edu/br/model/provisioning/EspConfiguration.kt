package com.ifpe.edu.br.model.provisioning

import com.google.gson.annotations.SerializedName

/**
 * JSON enviado para a ESP32 via Socket.
 */
data class EspConfiguration(
    @SerializedName("server") val serverUrl: String,     // URL do ThingsBoard
    @SerializedName("ssid") val targetSsid: String,      // Nova rede para a ESP conectar
    @SerializedName("password") val targetPassword: String, // Senha da nova rede
    @SerializedName("token") val deviceToken: String     // Token gerado na Task 4
)