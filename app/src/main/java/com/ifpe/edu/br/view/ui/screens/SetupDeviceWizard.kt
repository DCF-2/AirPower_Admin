package com.ifpe.edu.br.view.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ifpe.edu.br.common.components.CustomInputText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.view.ui.components.EspSelectionBottomSheet
import com.ifpe.edu.br.viewmodel.AdminViewModel

@Composable
fun SetupDeviceWizard(viewModel: AdminViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val status by viewModel.provisioningStatus.collectAsState()
    val context = LocalContext.current

    // --- Estados de escolha da eps32---
    val showEspSelection by viewModel.showEspSelection.collectAsState()
    val discoveredEsps by viewModel.discoveredEsps.collectAsState()
    val isSearchingEsps by viewModel.isSearchingEsps.collectAsState()

    // --- Estados de conexão (WI-FI) ---
    val availableNetworks by viewModel.availableNetworks.collectAsState()
    val isScanningWifi by viewModel.isScanningWifi.collectAsState()

    // Estados do Wizard
    var step by remember { mutableStateOf(0) }
    var locationInput by remember { mutableStateOf("") }

    // NOTA: wifiSsid e wifiPass foram removidos da UI. O ViewModel os gerencia secretamente.
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
            // Se o usuário clicou numa rede E deu permissão, abre o diálogo final de confirmação
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
                        "Deseja iniciar o servidor de configuração para a rede escolhida?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
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

    // --- MÓDULO 1: MODAL DE ESCOLHA DA ESP32 ---
    if (showEspSelection) {
        EspSelectionBottomSheet(
            discoveredDevices = discoveredEsps,
            isSearching = isSearchingEsps,
            onDismiss = { viewModel.closeEspSelectionModal() },
            onDeviceSelected = { espEscolhida ->
                viewModel.proceedToNetworkModule(espEscolhida)
                // IMPORTANTE: Ao escolher a placa, nós avançamos o Wizard para o Passo 1 (Wi-Fi)
                espId = espEscolhida.id // Preenche automaticamente o ID na tela de Wi-Fi!
                step = 1
            },
            onBlinkTest = { espParaPiscar ->
                viewModel.testBlinkLed(espParaPiscar.ip)
            }
        )
    }

    // --- MÓDULO 2: DISPARO AUTOMÁTICO DO SCANNER ---
    LaunchedEffect(step) {
        if (step == 1 && !isSocketSuccess) {
            viewModel.scanForAuthorizedNetworks()
        }
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

                        Button(onClick = {
                            viewModel.openEspSelectionModal()
                        }) {
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
                // --- FASE 2.A: Conexão via Hotspot & ESCOLHA DE REDE ---

                Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Dispositivo Alvo:", style = MaterialTheme.typography.labelMedium)
                        Text(selectedDevice?.name ?: "Novo", style = MaterialTheme.typography.titleMedium)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("ID da Placa Física: $espId", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Text("Selecione a Rede Wi-Fi:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                // --- UI DO SCANNER WI-FI ---
                if (isScanningWifi) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("A varrer o ambiente...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (availableNetworks.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Nenhuma rede autorizada encontrada aqui.", color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.scanForAuthorizedNetworks() }) {
                                Text("Tentar Novamente")
                            }
                        }
                    }
                } else {
                    // Lista Inteligente de Redes
                    availableNetworks.forEach { network ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // 1. O ViewModel guarda a senha e o SSID secretamente
                                    viewModel.selectNetworkAndProceed(network)

                                    // 2. Dispara a verificação de permissão e abre o Dialog do Hotspot
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        showConfirmationDialog = true
                                    } else {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Wifi,
                                    contentDescription = "Wi-Fi"
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(network.ssid, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Local: ${network.location}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

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
                            step = 0
                            locationInput = ""
                            viewModel.resetUIState(Constants.UIStateKey.SESSION)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("Concluir Processo") }
                }
            }
        }
    }
}