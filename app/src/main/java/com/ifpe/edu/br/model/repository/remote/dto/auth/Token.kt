package com.ifpe.edu.br.model.repository.remote.dto.auth

import com.google.gson.annotations.SerializedName

data class Token(
    @SerializedName("token") val token: String,


    @SerializedName("refreshToken") val refreshToken: String,

    // Como o servidor não está enviando scope, deixamos nullable com valor padrão null
    @SerializedName("scope") val scope: String? = null,

    @SerializedName("tbUrl") val tbUrl: String? = null
) {
    override fun toString(): String {
        return "Token(token='$token', " +
                "refreshToken='$refreshToken', " +
                "scope=$scope, " +
                "tbUrl=$tbUrl)"
    }
}