package com.ifpe.edu.br.model.repository.remote.api

import com.ifpe.edu.br.model.util.AirPowerLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class TelemetryWebSocketClient @Inject constructor() {

    private var webSocket: WebSocket? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val telemetryFlow = _telemetryFlow.asSharedFlow()

    // 🔥 A CORREÇÃO: Um OkHttpClient que ignora os erros de Certificado SSL do servidor (Laboratório)
    private val client: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Aceita qualquer IP/Hostname
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }

    fun connect(proxyBaseUrl: String, token: String, email: String, onConnected: () -> Unit) {
        if (webSocket != null && _isConnected.value) {
            onConnected()
            return
        }

        // Garante a formatação exata da URL
        val base = proxyBaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .removeSuffix("/")

        val wsUrl = "$base/ws/telemetry"

        AirPowerLog.d("WebSocket", "A tentar conectar a: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-User-Email", email)
            .build()

        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isConnected.value = true // 🔥 ATUALIZA PARA LIGADO
                AirPowerLog.d("WebSocket", "✅ Conectado ao Proxy via OkHttp!")
                this@TelemetryWebSocketClient.webSocket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _telemetryFlow.tryEmit(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false // 🔥 ATUALIZA PARA DESLIGADO
                AirPowerLog.e("WebSocket", "❌ Falha na conexão: ${t.message}")
                this@TelemetryWebSocketClient.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false // 🔥 ATUALIZA PARA DESLIGADO
                AirPowerLog.d("WebSocket", "🔌 Conexão encerrada: $reason")
                this@TelemetryWebSocketClient.webSocket = null
            }
        })
    }

    fun subscribeToDevice(deviceId: String) {
        if (_isConnected.value) {
            val subscribeCmd = """
                {
                  "tsSubCmds": [
                    {
                      "entityType": "DEVICE",
                      "entityId": "$deviceId",
                      "scope": "LATEST_TELEMETRY",
                      "cmdId": 10
                    }
                  ],
                  "historyCmds": [],
                  "attrSubCmds": []
                }
            """.trimIndent()

            webSocket?.send(subscribeCmd)
            AirPowerLog.d("WebSocket", "📡 Subscrição enviada para o Device ID: $deviceId")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Desconectado voluntariamente.")
        webSocket = null
        _isConnected.value = false
    }
}