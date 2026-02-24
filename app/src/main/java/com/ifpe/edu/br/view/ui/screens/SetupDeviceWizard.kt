package com.ifpe.edu.br.view.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.common.components.CustomInputText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.viewmodel.AdminViewModel

@Composable
fun SetupDeviceWizard(viewModel: AdminViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val status by viewModel.provisioningStatus.collectAsState()

    // Passos do Wizard: 0 = Localização, 1 = Configurar
    var step by remember { mutableStateOf(0) }

    // Dados
    var locationInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var espId by remember { mutableStateOf("") } // ID da ESP no Hotspot

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
            // --- PASSO 2: Conexão Técnica ---
            Text("Configurar Conexão", style = MaterialTheme.typography.headlineSmall)

            Card(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dispositivo Alvo:", style = MaterialTheme.typography.labelMedium)
                    Text(selectedDevice?.name ?: "Novo", style = MaterialTheme.typography.titleMedium)
                    Divider(Modifier.padding(vertical = 8.dp))
                    Text("Token: ${viewModel.currentToken}", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }

            CustomInputText(
                value = espId,
                onValueChange = { espId = it },
                label = "ID da ESP32 (Check no Hotspot)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            CustomInputText(
                value = wifiSsid,
                onValueChange = { wifiSsid = it },
                label = "SSID da Rede IoT",
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

            // Console de Status
            Box(
                Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black).padding(8.dp)
            ) {
                Text(status, color = Color.Green, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            RectButton(
                text = "Configurar e Finalizar",
                onClick = {
                    viewModel.targetWifiSsid = wifiSsid
                    viewModel.targetWifiPassword = wifiPass
                    viewModel.sendConfigurationToEsp()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}