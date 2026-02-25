package com.ifpe.edu.br.model.provisioning

import com.google.gson.annotations.SerializedName

/**
 * Este é o JSON exato que a ESP32 vai receber via Socket.
 */
data class EspConfiguration(
    // Mapeia "targetSsid" do Kotlin para "ssid" no JSON (para bater com: res["rwf"]["ssid"] na ESP)
    // OBS: Seu código C++ lê res["rwf"]["ssid"]. Se o Gson enviar plano, a ESP não vai achar.
    // Vamos simplificar e fazer o Android enviar a estrutura que a ESP espera, ou ajustar a ESP.

    // Vou sugerir ajustar o Android para enviar o que a ESP já lê hoje, ou simplificar tudo.
    // Vamos assumir uma estrutura PLANA para facilitar a vida de ambos.

    @SerializedName("ssid") val targetSsid: String,
    @SerializedName("senha") val targetPassword: String, // A ESP lê res["rwf"]["senha"]? Vamos simplificar para "senha"
    @SerializedName("token") val deviceToken: String,
    @SerializedName("iptb") val serverUrl: String, // <--- O CAMPO QUE FALTAVA
    val serverPort: Int,
    val location: String
)
/*data class EspConfiguration(
    @SerializedName("server") val serverUrl: String,       // Ex: 10.5.0.66
    @SerializedName("port") val serverPort: Int,           // Ex: 8080 (novo campo importante)
    @SerializedName("ssid") val targetSsid: String,        // Nome do Wi-Fi IoT
    @SerializedName("password") val targetPassword: String, // Senha do Wi-Fi IoT
    @SerializedName("token") val deviceToken: String,      // Token do ThingsBoard
    @SerializedName("location") val location: String       // Ex: "Laboratório 1"
)*/