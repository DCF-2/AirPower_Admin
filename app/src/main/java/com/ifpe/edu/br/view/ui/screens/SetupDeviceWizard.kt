package com.ifpe.edu.br.view.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ifpe.edu.br.common.components.CustomInputText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.viewmodel.AdminViewModel

@Composable
fun SetupDeviceWizard(viewModel: AdminViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val status by viewModel.provisioningStatus.collectAsState()
    val context = LocalContext.current

    // Estados do Wizard
    var step by remember { mutableStateOf(0) }
    var locationInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var espId by remember { mutableStateOf("") } // ID da ESP no Hotspot

    // Verifica se a etapa do socket terminou com sucesso (flag definida no ViewModel)
    val isSocketSuccess = status == "SUCESSO_SOCKET" || status.contains("FINALIZADO")

    // Controle do Alerta de Ativação
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Launcher de Permissão de GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Se deu permissão, abre o diálogo de confirmação
            showConfirmationDialog = true
        }
    }

    // --- ALERTA DE CONFIRMAÇÃO ---
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Atenção Necessária") },
            text = {
                Text("1. Ative o Hotspot do celular agora.\n" +
                        "2. Certifique-se que a ESP32 está ligada.\n" +
                        "3. O SSID do Hotspot deve ser: airp\n" +
                        "4. A senha do Hotspot deve ser: suicr7ap\n\n" +
                        "Deseja iniciar o servidor de configuração?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        // Passa os dados para o ViewModel antes de iniciar
                        viewModel.targetWifiSsid = wifiSsid
                        viewModel.targetWifiPassword = wifiPass
                        viewModel.targetEspIdInput = espId
                        viewModel.sendConfigurationToEsp()
                    }
                ) { Text("ATIVAR AGORA") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        if (step == 0) {
            // --- PASSO 1: Localização ---
            Text("Onde estamos?", style = MaterialTheme.typography.headlineSmall)
            Text("Vamos verificar se já existe um sensor nesta sala.", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))

            CustomInputText(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = "Local (Ex: Laboratório 1)",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            RectButton(
                text = "Buscar Dispositivo",
                onClick = {
                    viewModel.checkLocationAndFindDevice(locationInput)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Resultado da Busca
            if (selectedDevice != null) {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Encontrado!", style = MaterialTheme.typography.labelLarge)
                        Text(selectedDevice!!.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { step = 1 }) {
                            Text("Vincular a este dispositivo")
                        }
                    }
                }
            } else if (!isLoading && locationInput.isNotEmpty()) {
                // Sugestão de Criar
                Spacer(Modifier.height(24.dp))
                Text("Nenhum dispositivo encontrado aqui.", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        viewModel.createDevice("AirPower ${locationInput}", location = locationInput)
                        step = 1
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Criar Novo Dispositivo")
                }
            }
        } else {
            // --- PASSO 2: Configurar Conexão ---
            Text("Configurar Sensor", style = MaterialTheme.typography.headlineSmall)

            // Console de Status (Sempre visível para acompanhar o processo)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Text(
                    text = if (status == "SUCESSO_SOCKET") "✅ ESP32 Configurada! Aguardando GPS..." else status,
                    color = Color.Green,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!isSocketSuccess) {
                // --- FASE 2.A: Conexão via Hotspot ---

                Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Dispositivo Alvo:", style = MaterialTheme.typography.labelMedium)
                        Text(selectedDevice?.name ?: "Novo", style = MaterialTheme.typography.titleMedium)
                        Divider(Modifier.padding(vertical = 8.dp))
                        Text("Token: ${viewModel.currentToken}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                CustomInputText(
                    value = espId,
                    onValueChange = { espId = it },
                    label = "ID da ESP32 (ex: ESP32_01)",
                    placeholder = "Digite o ID que a ESP envia no Handshake",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                CustomInputText(
                    value = wifiSsid,
                    onValueChange = { wifiSsid = it },
                    label = "SSID da Rede IoT (Wi-Fi da escola)",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                CustomInputText(
                    value = wifiPass,
                    onValueChange = { wifiPass = it },
                    label = "Senha da Rede IoT",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                RectButton(
                    text = "Iniciar Configuração (Hotspot)",
                    onClick = {
                        // Verifica permissão de GPS antes de prosseguir
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            showConfirmationDialog = true
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

            } else {
                // --- FASE 2.B: Salvar GPS (Internet) ---

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("⚠️ Troca de Rede Necessária", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("1. A ESP32 já recebeu os dados e vai reiniciar.")
                        Text("2. AGORA, desligue o Hotspot do celular.")
                        Text("3. Conecte-se à internet (4G ou Wi-Fi).")
                        Text("4. Clique abaixo para salvar a localização.")
                    }
                }

                if (!status.contains("FINALIZADO")) {
                    RectButton(
                        text = "Já tenho Internet: Salvar GPS",
                        onClick = {
                            viewModel.saveLocationToThingsBoard()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Botão Finalizar
                    Button(
                        onClick = {
                            // Reseta para o início ou volta para home
                            step = 0
                            locationInput = ""
                            viewModel.resetUIState(Constants.UIStateKey.SESSION)
                            // Opcional: Navegar para Home
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("Concluir Processo") }
                }
            }
        }
    }
}