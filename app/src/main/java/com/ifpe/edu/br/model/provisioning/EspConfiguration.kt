package com.ifpe.edu.br.model.provisioning

import com.google.gson.annotations.SerializedName

/**
 * Este é o JSON exato que a ESP32 vai receber via Socket.
 */
data class EspConfiguration(
    // Adicionamos o targetId para dupla validação na ESP32!
    @SerializedName("targetId") val targetId: String,

    @SerializedName("ssid") val targetSsid: String,
    @SerializedName("senha") val targetPassword: String,
    @SerializedName("token") val deviceToken: String,
    @SerializedName("iptb") val serverUrl: String,
    val serverPort: Int,
    val location: String
)