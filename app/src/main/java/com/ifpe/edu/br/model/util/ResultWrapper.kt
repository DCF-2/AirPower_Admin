package com.ifpe.edu.br.model.util

sealed class ResultWrapper<out T> {
    data class Success<out T>(val value: T) : ResultWrapper<T>()

    data object Empty: ResultWrapper<Nothing>()
    data class ApiError(val code: Int? = null, val error: String? = null) : ResultWrapper<Nothing>()

    data class GenericError(val code: Int, val message: String): ResultWrapper<Nothing>()

    data object NetworkError : ResultWrapper<Nothing>()
}