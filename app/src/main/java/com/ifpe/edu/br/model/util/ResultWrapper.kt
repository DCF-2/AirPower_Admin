package com.ifpe.edu.br.model.util

import com.ifpe.edu.br.model.repository.remote.dto.error.ErrorCode


/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
sealed class ResultWrapper<out T> {
    data class Success<out T>(val value: T) : ResultWrapper<T>()
    data object Empty: ResultWrapper<Nothing>()
    data class ApiError(val errorCode: ErrorCode) : ResultWrapper<Nothing>()
    data class GenericError(val code: Int, val message: String): ResultWrapper<Nothing>()
    data object NetworkError : ResultWrapper<Nothing>()
}
