package com.ifpe.edu.br.model.repository.remote.dto.auth

import com.google.gson.annotations.SerializedName
data class LoginRequest(
    @SerializedName("email") val email: String, // Mudou de username para email
    @SerializedName("password") val password: String
)