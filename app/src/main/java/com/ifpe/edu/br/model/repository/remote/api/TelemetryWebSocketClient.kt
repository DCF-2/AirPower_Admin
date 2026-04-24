package com.ifpe.edu.br.model.repository.remote.api

import com.ifpe.edu.br.model.util.AirPowerLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private var isWebSocketOpen = false

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
            .pingInterval(10, TimeUnit.SECONDS) // Baixei o ping para 10s para evitar a queda do Nginx
            .build()
    }

    fun connect(proxyBaseUrl: String, token: String, email: String, onConnected: () -> Unit) {
        if (webSocket != null && isWebSocketOpen) {
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

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isWebSocketOpen = true
                AirPowerLog.d("WebSocket", "✅ Conectado à Ponte SpringBoot (Proxy) com sucesso!")
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (AirPowerLog.ISVERBOSE) {
                    AirPowerLog.d("WebSocket", "📥 Telemetria recebida: $text")
                }
                _telemetryFlow.tryEmit(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isWebSocketOpen = false
                AirPowerLog.e("WebSocket", "❌ Falha na conexão WebSocket: ${t.message} - Code: ${response?.code}")
                this@TelemetryWebSocketClient.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isWebSocketOpen = false
                AirPowerLog.d("WebSocket", "🔌 Conexão WebSocket encerrada: $reason")
                this@TelemetryWebSocketClient.webSocket = null
            }
        })
    }

    fun subscribeToDevice(deviceId: String) {
        if (isWebSocketOpen) {
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
        isWebSocketOpen = false
    }
}