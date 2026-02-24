package com.ifpe.edu.br.model.repository.remote.dto

import com.google.gson.annotations.SerializedName

data class PageData<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("hasNext") val hasNext: Boolean
)