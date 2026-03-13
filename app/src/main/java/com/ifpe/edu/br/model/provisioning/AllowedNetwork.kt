package com.ifpe.edu.br.model.provisioning

import com.google.gson.annotations.SerializedName

data class AllowedNetwork(
    val ssid: String,
    val password: String, // Esta senha nunca será exibida na UI
    @SerializedName("location")
    val location: String, // Ex: "Laboratório de IoT"
    val signalLevel: Int = -100
)