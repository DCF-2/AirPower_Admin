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
    private val PORT = 9090 // Porta que a ESP32 vai procurar (ajuste se necessário)
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val gson = Gson()

    // Callback para informar o ViewModel sobre o progresso
    interface ProvisioningCallback {
        fun onStatusChanged(status: String)
        fun onDeviceConnected(deviceId: String)
        fun onError(error: String)
        fun onFinished()
    }

    suspend fun startServer(
        expectedDeviceId: String?, // Se nulo, aceita qualquer um
        configToSend: EspConfiguration,
        callback: ProvisioningCallback
    ) = withContext(Dispatchers.IO) {
        try {
            stopServer() // Fecha anterior se houver
            isRunning = true
            serverSocket = ServerSocket(PORT)
            serverSocket?.soTimeout = 60000 // 60 segundos esperando conexão

            withContext(Dispatchers.Main) {
                callback.onStatusChanged("Aguardando conexão da ESP32 na porta $PORT...")
            }

            Log.d(TAG, "Server started on port $PORT")

            // Aguarda conexão (Bloqueante)
            val clientSocket = serverSocket!!.accept()
            handleClient(clientSocket, expectedDeviceId, configToSend, callback)

        } catch (e: SocketTimeoutException) {
            withContext(Dispatchers.Main) { callback.onError("Tempo esgotado. Nenhuma ESP32 conectou.") }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { callback.onError("Erro no servidor: ${e.message}") }
        } finally {
            stopServer()
        }
    }

    private suspend fun handleClient(
        socket: Socket,
        expectedDeviceId: String?,
        config: EspConfiguration,
        callback: ProvisioningCallback
    ) {
        try {
            socket.soTimeout = 10000 // 10s para troca de mensagens
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = PrintWriter(socket.getOutputStream(), true)

            // 1. Ler ID da ESP32
            // Protocolo: ESP envia ID -> Android valida -> Android envia JSON
            val receivedId = input.readLine()?.trim()

            Log.d(TAG, "Recebido da ESP: $receivedId")

            if (receivedId.isNullOrEmpty()) {
                withContext(Dispatchers.Main) { callback.onError("ESP32 desconectou sem enviar ID.") }
                return
            }

            withContext(Dispatchers.Main) { callback.onDeviceConnected(receivedId) }

            // Validação (Opcional: se o admin digitou um ID específico na tela anterior)
            if (!expectedDeviceId.isNullOrEmpty() && receivedId != expectedDeviceId) {
                withContext(Dispatchers.Main) { callback.onError("ID Incompatível! Esperado: $expectedDeviceId, Recebido: $receivedId") }
                // Opcional: Mandar comando de erro para ESP?
                return
            }

            // 2. Enviar JSON de Configuração
            val jsonConfig = gson.toJson(config)
            output.println(jsonConfig)
            output.flush()

            withContext(Dispatchers.Main) { callback.onStatusChanged("Configuração enviada! Aguardando confirmação...") }

            // 3. Aguardar desconexão ou 'OK' da ESP
            // Se a ESP fechar a conexão, consideramos sucesso
            try {
                while (input.readLine() != null) {
                    // Apenas lendo até fechar
                }
            } catch (e: Exception) {
                // Socket fechado é esperado
            }

            withContext(Dispatchers.Main) {
                callback.onStatusChanged("ESP32 Configurada com sucesso!")
                callback.onFinished()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { callback.onError("Erro na comunicação: ${e.message}") }
        } finally {
            socket.close()
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignora
        }
        serverSocket = null
    }
}