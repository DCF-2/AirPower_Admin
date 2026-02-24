package com.ifpe.edu.br.model.provisioning

import com.google.gson.annotations.SerializedName

/**
 * Este é o JSON exato que a ESP32 vai receber via Socket.
 */
data class EspConfiguration(
    @SerializedName("server") val serverUrl: String,       // Ex: 10.5.0.66
    @SerializedName("port") val serverPort: Int,           // Ex: 8080 (novo campo importante)
    @SerializedName("ssid") val targetSsid: String,        // Nome do Wi-Fi IoT
    @SerializedName("password") val targetPassword: String, // Senha do Wi-Fi IoT
    @SerializedName("token") val deviceToken: String,      // Token do ThingsBoard
    @SerializedName("location") val location: String       // Ex: "Laboratório 1"
)