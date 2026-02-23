package com.ifpe.edu.br.model.repository.remote.dto.auth

import com.google.gson.annotations.SerializedName

data class ThingsBoardLoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("scope") val scope: String? = null
)