package com.ifpe.edu.br.model.repository.remote.dto.auth

import com.google.gson.annotations.SerializedName

data class Token(
    @SerializedName("token")val token: String,
    @SerializedName("refreshtoken")val refreshToken: String,
    @SerializedName("scope")val scope: String,
    @SerializedName("tbUrl")val tbUrl: String? = null
) {
    override fun toString(): String {
        return "Token(token='$token', " +
                "refreshToken='$refreshToken', " +
                "scope=$scope)"
    }
}
