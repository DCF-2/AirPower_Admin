package com.ifpe.edu.br.model.provisioning

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class EspProvisioningManager {

    private val TAG = "EspProvisioning"
    private val PORT = 9090 // Porta que a ESP32 vai procurar no Gateway (Celular)
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val gson = Gson()

    // Interface para comunicar o progresso de volta para a UI
    interface ProvisioningCallback {
        fun onStatusChanged(status: String) // Ex: "Aguardando ESP32..."
        fun onError(error: String)          // Ex: "Timeout"
        fun onSuccess()                     // Ex: "Dados enviados!"
    }

    suspend fun startServer(
        config: EspConfiguration,
        callback: ProvisioningCallback
    ) = withContext(Dispatchers.IO) {
        try {
            stopServer() // Garante que não tem nada rodando antes

            serverSocket = ServerSocket(PORT)
            serverSocket?.soTimeout = 90000 // 90 segundos esperando a ESP conectar
            isRunning = true

            Log.d(TAG, "Servidor Socket iniciado na porta $PORT")
            withContext(Dispatchers.Main) {
                callback.onStatusChanged("Servidor ativo na porta $PORT.\nAguardando a ESP32 conectar no Hotspot...")
            }

            // --- BLOQUEANTE: O código para aqui até a ESP conectar ---
            val clientSocket = serverSocket!!.accept()

            // Se passou daqui, a ESP conectou!
            handleClientConnection(clientSocket, config, callback)

        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout esperando ESP32")
            withContext(Dispatchers.Main) {
                callback.onError("Tempo esgotado! A ESP32 não conectou a tempo.\nVerifique se o Hotspot está ligado.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no servidor: ${e.message}")
            withContext(Dispatchers.Main) {
                callback.onError("Erro: ${e.message}")
            }
        } finally {
            stopServer()
        }
    }

    private suspend fun handleClientConnection(
        socket: Socket,
        config: EspConfiguration,
        callback: ProvisioningCallback
    ) {
        try {
            withContext(Dispatchers.Main) {
                callback.onStatusChanged("ESP32 Conectada! Enviando configurações...")
            }

            val output = PrintWriter(socket.getOutputStream(), true)
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))

            // 1. Converter o objeto de configuração para JSON String
            val jsonPayload = gson.toJson(config)
            Log.d(TAG, "Enviando JSON: $jsonPayload")

            // 2. Enviar para a ESP32
            output.println(jsonPayload)
            output.flush()

            // 3. Aguardar confirmação (ACK) da ESP32 (Opcional, mas recomendado)
            // Vamos supor que a ESP manda "OK" e fecha a conexão.
            val response = input.readLine()
            Log.d(TAG, "Resposta da ESP: $response")

            withContext(Dispatchers.Main) {
                callback.onSuccess()
                callback.onStatusChanged("Sucesso! Configuração enviada.\nA ESP32 deve reiniciar agora.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                callback.onError("Erro ao enviar dados: ${e.message}")
            }
        } finally {
            socket.close()
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fechar socket: ${e.message}")
        }
        serverSocket = null
    }
}