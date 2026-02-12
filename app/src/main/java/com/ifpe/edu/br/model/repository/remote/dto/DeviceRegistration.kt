package com.ifpe.edu.br.model.repository.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para registrar um novo dispositivo no ThingsBoard.
 */
data class DeviceRegistration(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String = "ESP32", // Padrão definido para o projeto
    @SerializedName("label") val label: String? = null,
    @SerializedName("additionalInfo") val additionalInfo: Map<String, Any>? = null
)