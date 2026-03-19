package com.ifpe.edu.br.model.provisioning

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class EspProvisioningManager {

    private val TAG = "EspProvisioning"
    private val PORT = 9090
    private var serverSocket: ServerSocket? = null
    @Volatile private var isRunning = false
    private val gson = Gson()

    interface ProvisioningCallback {
        fun onStatusChanged(status: String)
        fun onError(error: String)
        fun onSuccess()
    }

    suspend fun startServer(
        config: EspConfiguration,
        expectedEspId: String,
        callback: ProvisioningCallback
    ) = withContext(Dispatchers.IO) {
        try {
            stopServer()

            // Configuração Robusta do Socket
            serverSocket = ServerSocket()
            serverSocket?.reuseAddress = true
            serverSocket?.bind(InetSocketAddress("0.0.0.0", PORT))
            serverSocket?.soTimeout = 120000 // 2 minutos para achar a placa certa
            isRunning = true

            Log.d(TAG, "Servidor ouvindo em 0.0.0.0:$PORT")
            withContext(Dispatchers.Main) {
                callback.onStatusChanged("Aguardando conexão da ESP32 (Alvo: $expectedEspId)...")
            }

            // O LOOP DA RESILIÊNCIA: Continua aceitando conexões até achar a placa certa ou dar timeout
            while (isRunning) {
                var clientSocket: Socket? = null
                try {
                    // Bloqueia até ALGUÉM conectar
                    clientSocket = serverSocket!!.accept()
                    Log.d(TAG, "Cliente conectado: ${clientSocket.inetAddress.hostAddress}")

                    // Tenta o Handshake com este cliente
                    val isCorrectEsp = handleClientHandshake(clientSocket, config, expectedEspId, callback)

                    if (isCorrectEsp) {
                        // Se for a placa certa, o handshake foi concluído com sucesso.
                        // Podemos sair do loop e desligar o servidor.
                        isRunning = false
                        break
                    } else {
                        // Se for um vizinho (intruso), avisamos na UI e o loop volta para o accept()
                        withContext(Dispatchers.Main) {
                            callback.onStatusChanged("Intruso rejeitado. Aguardando a ESP correta...")
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    throw e // Repassa o timeout para o bloco catch de fora
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Se der um erro de I/O com a placa, ignoramos e continuamos esperando
                } finally {
                    // Garante que a conexão com O CLIENTE ATUAL é fechada antes de aceitar o próximo
                    try { clientSocket?.close() } catch (e: Exception) {}
                }
            }

        } catch (e: SocketTimeoutException) {
            withContext(Dispatchers.Main) {
                callback.onError("Tempo esgotado. A ESP32 correta não conectou.")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError("Erro fatal no Servidor TCP: ${e.message}")
            }
        } finally {
            stopServer()
        }
    }

    /**
     * Retorna TRUE se a configuração foi entregue com sucesso à placa certa.
     * Retorna FALSE se a placa for incorreta (intruso).
     * Lança Exceptions para erros de rede graves.
     */
    private suspend fun handleClientHandshake(
        socket: Socket,
        config: EspConfiguration,
        expectedEspId: String,
        callback: ProvisioningCallback
    ): Boolean {
        // Define um timeout curto (5 seg) apenas para a leitura dos dados do cliente.
        // Assim, se um intruso conectar e ficar mudo, ele não trava o servidor por 2 minutos!
        socket.soTimeout = 5000

        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(socket.getOutputStream(), true)

        // --- PASSO 1: LER O ID DA ESP32 ---
        val receivedData = input.readLine() ?: throw Exception("Cliente desconectou sem enviar dados.")

        Log.d(TAG, "Recebido da placa: $receivedData")

        // --- PASSO 2: VALIDAR O ID ---
        if (!receivedData.contains(expectedEspId, ignoreCase = true)) {
            Log.d(TAG, "Rejeitando placa intrusa. Esperado: $expectedEspId, Recebido: $receivedData")

            // Avisar a ESP que ela foi rejeitada
            val rejectJson = "{\"status\":\"error\", \"message\":\"ID_REJECTED\"}"
            output.println(rejectJson)
            output.flush()
            Thread.sleep(500)

            return false // Indica ao while() que era a placa errada e ele deve continuar esperando
        }

        // --- PASSO 3: É A PLACA CERTA! ENVIAR A CONFIGURAÇÃO ---
        withContext(Dispatchers.Main) {
            callback.onStatusChanged("ID Confirmado! Enviando Configuração...")
        }

        val jsonPayload = gson.toJson(config)
        Log.d(TAG, "Enviando: $jsonPayload")

        output.println(jsonPayload)
        output.flush()

        // Dá tempo da ESP processar o JSON antes de cortarmos a conexão
        Thread.sleep(2000)

        withContext(Dispatchers.Main) {
            callback.onSuccess()
        }

        return true // Indica ao while() que terminamos com sucesso
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) { }
        serverSocket = null
    }
}