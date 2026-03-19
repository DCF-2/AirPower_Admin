package com.ifpe.edu.br.model.provisioning

import com.google.gson.annotations.SerializedName

data class EspConfiguration(
    @SerializedName("targetId") val targetId: String,
    @SerializedName("ssid") val targetSsid: String,
    @SerializedName("senha") val targetPassword: String,
    @SerializedName("token") val deviceToken: String,
    @SerializedName("iptb") val serverUrl: String
)