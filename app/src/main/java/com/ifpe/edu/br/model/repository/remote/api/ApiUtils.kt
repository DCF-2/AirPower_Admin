package com.ifpe.edu.br.model.repository.remote.api

/*
* Refactored for: AirPower Admin
*/
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.util.AirPowerLog
import com.ifpe.edu.br.model.util.ResultWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ResultWrapper<T> {
    val TAG = "safeApiCall"
    return withContext(Dispatchers.IO) {
        try {
            ResultWrapper.Success(apiCall.invoke())
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> {
                    if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "Erro de Rede (Sem internet ou servidor offline)")
                    ResultWrapper.NetworkError
                }
                is HttpException -> {
                    val code = throwable.code()
                    val errorBody = throwable.response()?.errorBody()?.string()

                    if (AirPowerLog.ISVERBOSE) AirPowerLog.e(TAG, "Erro da API HTTP $code: $errorBody")

                    // Retorna o código HTTP (ex: 401, 403, 500) para a Activity decidir o que mostrar
                    ResultWrapper.ApiError(code)
                }
                else -> {
                    if (AirPowerLog.ISVERBOSE) AirPowerLog.e(TAG, "Erro Desconhecido: ${throwable.message}")
                    ResultWrapper.ApiError(Constants.ResponseErrorCode.UNKNOWN_INTERNAL_ERROR)
                }
            }
        }
    }
}