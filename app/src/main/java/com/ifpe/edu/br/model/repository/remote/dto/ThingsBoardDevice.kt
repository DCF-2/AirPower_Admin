package com.ifpe.edu.br.model.repository.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Representação do dispositivo retornado pela API do ThingsBoard.
 */
data class ThingsBoardDevice(
    @SerializedName("id") val id: DeviceId,
    @SerializedName("createdTime") val createdTime: Long,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String?
)

data class DeviceId(
    @SerializedName("id") val id: String,
    @SerializedName("entityType") val entityType: String
)