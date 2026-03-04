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
    private var isRunning = false
    private val gson = Gson()

    interface ProvisioningCallback {
        fun onStatusChanged(status: String)
        fun onError(error: String)
        fun onSuccess()
    }

    suspend fun startServer(
        config: EspConfiguration,
        expectedEspId: String, // <--- NOVO: O ID que esperamos receber da ESP
        callback: ProvisioningCallback
    ) = withContext(Dispatchers.IO) {
        try {
            stopServer()

            // Configuração Robusta do Socket
            serverSocket = ServerSocket()
            serverSocket?.reuseAddress = true
            serverSocket?.bind(InetSocketAddress("0.0.0.0", PORT))
            serverSocket?.soTimeout = 120000 // 2 minutos
            isRunning = true

            Log.d(TAG, "Servidor ouvindo em 0.0.0.0:$PORT")
            withContext(Dispatchers.Main) {
                callback.onStatusChanged("Aguardando conexão da ESP32...")
            }

            // Bloqueia até a ESP conectar
            val clientSocket = serverSocket!!.accept()

            Log.d(TAG, "Cliente conectado: ${clientSocket.inetAddress.hostAddress}")

            // Inicia o Handshake
            handleClientHandshake(clientSocket, config, expectedEspId, callback)

        } catch (e: SocketTimeoutException) {
            withContext(Dispatchers.Main) {
                callback.onError("Tempo esgotado. A ESP32 não conectou.")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError("Erro no Servidor: ${e.message}")
            }
        } finally {
            stopServer()
        }
    }

    private suspend fun handleClientHandshake(
        socket: Socket,
        config: EspConfiguration, 
        expectedEspId: String,
        callback: ProvisioningCallback
    ) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = PrintWriter(socket.getOutputStream(), true)

            // --- PASSO 1: LER O ID DA ESP32 ---
            withContext(Dispatchers.Main) {
                callback.onStatusChanged("Conectado! Aguardando ID da ESP...")
            }

            val receivedData = input.readLine()

            Log.d(TAG, "Recebido da ESP: $receivedData")

            if (receivedData == null) {
                throw Exception("ESP desconectou sem enviar ID.")
            }

            // --- PASSO 2: VALIDAR O ID ---
            if (!receivedData.contains(expectedEspId, ignoreCase = true)) {
                // Avisar a ESP que ela foi rejeitada antes de fechar a porta!
                val rejectJson = "{\"status\":\"error\", \"message\":\"ID_REJECTED\"}"
                output.println(rejectJson)
                output.flush()
                Thread.sleep(500) // Dá tempo do pacote chegar na ESP

                throw Exception("ID Incorreto! Esperado: $expectedEspId, Recebido: $receivedData")
            }

            // --- PASSO 3: ENVIAR O JSON DE CONFIGURAÇÃO ---
            withContext(Dispatchers.Main) {
                callback.onStatusChanged("ID Confirmado! Enviando Configuração...")
            }

            val jsonPayload = gson.toJson(config)
            Log.d(TAG, "Enviando: $jsonPayload")

            output.println(jsonPayload)
            output.flush()

            // Dá tempo da ESP processar antes de fechar
            Thread.sleep(2000)

            withContext(Dispatchers.Main) {
                callback.onSuccess()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                callback.onError("Falha no Handshake: ${e.message}")
            }
        } finally {
            socket.close()
        }
    }
    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) { }
        serverSocket = null
    }
}