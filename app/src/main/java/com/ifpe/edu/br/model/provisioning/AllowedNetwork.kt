package com.ifpe.edu.br.model.provisioning

data class AllowedNetwork(
    val ssid: String,
    val password: String, // Esta senha nunca será exibida na UI
    val location: String, // Ex: "Laboratório de IoT"
    val signalLevel: Int = -100
)