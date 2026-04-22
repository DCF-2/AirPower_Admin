package com.ifpe.edu.br.model.repository.persistence.manager

import com.ifpe.edu.br.AirPowerApplication
import com.ifpe.edu.br.model.repository.persistence.AirPowerDatabase
import com.ifpe.edu.br.model.repository.persistence.model.AirPowerToken
import com.ifpe.edu.br.model.repository.remote.dto.auth.Token
import com.ifpe.edu.br.model.util.AirPowerLog
import org.json.JSONObject
import java.util.Base64

object JWTManager {
    private val TAG = JWTManager::class.java.simpleName

    // O SEGREDO: Em vez do AdminRepository, usamos o DAO diretamente
    // através do contexto global da nossa Application!
    private val tokenDao by lazy {
        AirPowerDatabase.getDataBaseInstance(AirPowerApplication.getContext()).getTokenDaoInstance()
    }

    suspend fun handleAuthentication(
        connectionId: Int,
        token: Token,
        authCallback: suspend (AirPowerToken) -> Unit
    ) {
        if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "handleAuthentication()")
        require(isTokenValid(token)) { "Token is not valid" }

        val persistToken = AirPowerToken(
            connectionId,
            token.token,
            token.refreshToken,
            token.scope ?: ""
        )

        val clientToken = tokenDao.getTokenByClient(connectionId)

        if (clientToken == null) {
            if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "Token NOT found. Creating!")
            tokenDao.insert(persistToken)
        } else {
            if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "Token found. Updating!")
            tokenDao.update(persistToken)
        }

        authCallback(persistToken)
    }

    suspend fun handleRefreshToken(
        connectionId: Int,
        incomingToken: Token,
        authCallback: suspend (AirPowerToken) -> Unit
    ) {
        if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "handleRefreshToken()")
        val storedToken = tokenDao.getTokenByClient(connectionId)
            ?: throw IllegalStateException("Stored Token not found")

        require(isTokenValid(getTokenFromAirPowerToken(storedToken))) { "Stored Token is not valid" }
        require(isTokenValid(incomingToken)) { "Incoming Token is not valid" }

        storedToken.apply {
            jwt = incomingToken.token
            refreshToken = incomingToken.refreshToken
            scope = incomingToken.scope ?: ""
        }

        tokenDao.update(storedToken)
        authCallback(storedToken)
    }

    suspend fun isTokenExpiredForConnection(connectionId: Int): Boolean {
        val tokenByClient = tokenDao.getTokenByClient(connectionId) ?: return true.also {
            AirPowerLog.w(TAG, "Token not found for connectionId: $connectionId")
        }

        val jwtParts = tokenByClient.jwt.split(".")
        if (jwtParts.size != 3) return true.also {
            if (AirPowerLog.ISVERBOSE)
                AirPowerLog.w(TAG, "Invalid JWT format")
        }

        return try {
            val payloadJson = decodeJwtPayload(jwtParts[1])
            val exp = payloadJson.getLong("exp")
            val now = System.currentTimeMillis() / 1000

            if (AirPowerLog.ISVERBOSE)
                AirPowerLog.d(TAG, "exp: $exp, now: $now")
            exp < now
        } catch (e: Exception) {
            AirPowerLog.e(TAG, "Error parsing JWT: ${e.message}")
            true
        }
    }

    private fun decodeJwtPayload(encodedPayload: String): JSONObject {
        val decoded = Base64.getUrlDecoder().decode(encodedPayload)
        return JSONObject(String(decoded))
    }

    suspend fun getJwtForConnectionId(connectionId: Int): String {
        if (AirPowerLog.ISVERBOSE)
            AirPowerLog.d(TAG, "getJWTForConnectionId(): $connectionId")
        return getTokenForConnectionId(connectionId)?.token ?: ""
    }

    suspend fun getTokenForConnectionId(connectionId: Int): Token? {
        if (AirPowerLog.ISVERBOSE)
            AirPowerLog.d(TAG, "getTokenForConnectionId(): $connectionId")
        val token = tokenDao.getTokenByClient(connectionId)

        return token?.let { getTokenFromAirPowerToken(it) }
            ?: run {
                if (AirPowerLog.ISVERBOSE)
                    AirPowerLog.w(TAG, "Token is null for connection: $connectionId")
                null
            }
    }

    suspend fun resetTokenForConnection(connectionId: Int) {
        tokenDao.getTokenByClient(connectionId)?.apply {
            scope = "empty"
            jwt = "empty"
            refreshToken = "empty"
            if (AirPowerLog.ISLOGABLE)
                AirPowerLog.d(TAG, "Resetting token for connectionId: $connectionId")
            tokenDao.update(this)
        } ?: AirPowerLog.w(TAG, "Token not found for connectionId: $connectionId")
    }

    private fun getTokenFromAirPowerToken(airPowerToken: AirPowerToken): Token {
        return try {
            Token(
                airPowerToken.jwt,
                airPowerToken.refreshToken,
                airPowerToken.scope
            )
        } catch (e: Exception) {
            AirPowerLog.e(TAG, "Error while building Token object: ${e.message}")
            Token("", "", "")
        }
    }

    fun isTokenValid(token: Token?): Boolean {
        return when {
            token == null -> {
                if (AirPowerLog.ISLOGABLE)
                    AirPowerLog.w(TAG, "Token is null")
                false
            }

            token.token.isNullOrBlank() || token.token.length < 50 -> {
                if (AirPowerLog.ISVERBOSE)
                    AirPowerLog.w(TAG, "JWT is NOT valid")
                false
            }

            token.refreshToken.isNullOrBlank() || token.refreshToken.length < 50 -> {
                if (AirPowerLog.ISLOGABLE)
                    AirPowerLog.w(TAG, "Refresh token is NOT valid")
                false
            }

            else -> {
                if (AirPowerLog.ISVERBOSE)
                    AirPowerLog.d(TAG, "Token is valid")
                true
            }
        }
    }
}
