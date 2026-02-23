package com.ifpe.edu.br.view.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.common.CommonConstants
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomColumn
import com.ifpe.edu.br.common.components.CustomInputText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.viewmodel.AdminViewModel

@Composable
fun ProvisioningScreen(viewModel: AdminViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val theme = MaterialTheme.colorScheme
    val appDimens = AirPowerTheme.dimens

    // Estados observados do ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val deviceState by viewModel.deviceCreationState.collectAsState()
    val credentialsState by viewModel.credentialsState.collectAsState()
    val provisioningStatus by viewModel.provisioningStatus.collectAsState()

    // Controle do Dialog de Hotspot
    var showHotspotDialog by remember { mutableStateOf(false) }

    // Campos de Texto
    var deviceName by remember { mutableStateOf("") }
    var wifiSSID by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }

    // Variáveis auxiliares para controle de estado do botão (Lógica manual de Enabled)
    val isCreateEnabled = !isLoading && deviceName.isNotBlank()
    val isStartEnabled = viewModel.generatedDeviceToken.isNotEmpty() && wifiSSID.isNotBlank()

    // --- Modal de Instrução do Hotspot ---
    if (showHotspotDialog) {
        AlertDialog(
            onDismissRequest = { showHotspotDialog = false },
            title = { Text(text = "Configurar Hotspot") },
            text = {
                Text("Para conectar a ESP32, ative o Hotspot do celular com:\n\n" +
                        "SSID: ${wifiSSID}\n" +
                        "Senha: ${wifiPassword}\n\n" +
                        "1. Clique em 'Ir para Configurações'.\n" +
                        "2. Configure e ative o Roteador Wi-Fi.\n" +
                        "3. Volte aqui para iniciar o servidor.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHotspotDialog = false
                        // Abre configurações de Tethering/Hotspot
                        try {
                            val intent = Intent(Intent.ACTION_MAIN, null)
                            intent.addCategory(Intent.CATEGORY_LAUNCHER)
                            val componentName = android.content.ComponentName("com.android.settings", "com.android.settings.TetherSettings")
                            intent.component = componentName
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback se TetherSettings não existir
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    }
                ) { Text("Ir para Configurações") }
            },
            dismissButton = {
                Button(onClick = {
                    showHotspotDialog = false
                    // Inicia o Socket diretamente (usuário diz que já configurou)
                    viewModel.startProvisioning(deviceName.ifBlank { null })
                }) { Text("Já configurei, Iniciar") }
            }
        )
    }

    CustomColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(theme.background)
            .padding(16.dp),
        alignmentStrategy = CommonConstants.Ui.ALIGNMENT_CENTER,
        layouts = listOf {

            // --- CARD 1: Criar Dispositivo ---
            CustomCard(
                paddingBottom = 16.dp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(appDimens.cardCornerRadius)).background(theme.surface),
                layouts = listOf {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. Novo Dispositivo", style = MaterialTheme.typography.headlineSmall, color = theme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))

                        CustomInputText(
                            value = deviceName,
                            onValueChange = { deviceName = it },
                            label = "Nome do Dispositivo",
                            placeholder = "Ex: ESP32_Lab_01",
                            modifier = Modifier.fillMaxWidth(),
                            inputFieldColors = TextFieldDefaults.colors(
                                focusedContainerColor = theme.surfaceVariant,
                                unfocusedContainerColor = theme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        RectButton(
                            text = if (isLoading) "Processando..." else "Gerar Token no ThingsBoard",
                            // Lógica manual de "enabled": só clica se isCreateEnabled for true
                            onClick = {
                                if (isCreateEnabled) {
                                    viewModel.createDevice(deviceName, "ESP32", "AirPower Sensor")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                // Muda cor pra cinza se estiver desabilitado
                                containerColor = if (isCreateEnabled) theme.primary else Color.Gray
                            )
                        )

                        // Feedback Visual
                        if (deviceState is ResultWrapper.Success && credentialsState is ResultWrapper.Success) {
                            val token = (credentialsState as ResultWrapper.Success).value.credentialsId
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✅ Token Gerado: $token", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            )

            // --- CARD 2: Dados da Rede Wi-Fi ---
            CustomCard(
                paddingBottom = 16.dp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(appDimens.cardCornerRadius)).background(theme.surface),
                layouts = listOf {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. Configurar Rede IoT", style = MaterialTheme.typography.headlineSmall, color = theme.onSurface)
                        Text("Insira os dados da rede Wi-Fi onde a ESP32 irá operar.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                        Spacer(modifier = Modifier.height(16.dp))

                        CustomInputText(
                            value = wifiSSID,
                            onValueChange = { wifiSSID = it },
                            label = "SSID da Rede (Wi-Fi)",
                            modifier = Modifier.fillMaxWidth(),
                            inputFieldColors = TextFieldDefaults.colors(
                                focusedContainerColor = theme.surfaceVariant,
                                unfocusedContainerColor = theme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CustomInputText(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = "Senha da Rede",
                            isPassword = true,
                            modifier = Modifier.fillMaxWidth(),
                            inputFieldColors = TextFieldDefaults.colors(
                                focusedContainerColor = theme.surfaceVariant,
                                unfocusedContainerColor = theme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            )

            // --- CARD 3: Provisionamento (Socket) ---
            CustomCard(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(appDimens.cardCornerRadius)).background(theme.surface),
                layouts = listOf {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. Sincronizar com ESP32", style = MaterialTheme.typography.headlineSmall, color = theme.onSurface)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Log (Terminal)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "> $provisioningStatus",
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        RectButton(
                            text = "Iniciar Hotspot e Servidor",
                            // Lógica manual de "enabled"
                            onClick = {
                                if (isStartEnabled) {
                                    viewModel.targetWifiSsid = wifiSSID
                                    viewModel.targetWifiPassword = wifiPassword
                                    showHotspotDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStartEnabled) theme.secondary else Color.Gray,
                                contentColor = theme.onSecondary
                            )
                        )
                    }
                }
            )
        }
    )
}