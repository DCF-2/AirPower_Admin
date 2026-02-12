package com.ifpe.edu.br.model.repository.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Credenciais do dispositivo retornadas pelo ThingsBoard.
 * Para o tipo 'ACCESS_TOKEN', o token reside no campo 'credentialsId'.
 */
data class DeviceCredentials(
    @SerializedName("id") val id: DeviceId,
    @SerializedName("createdTime") val createdTime: Long,
    @SerializedName("credentialsType") val credentialsType: String,
    @SerializedName("credentialsId") val credentialsId: String?, // Aqui fica o Token para ACCESS_TOKEN type
    @SerializedName("credentialsValue") val credentialsValue: String?
)