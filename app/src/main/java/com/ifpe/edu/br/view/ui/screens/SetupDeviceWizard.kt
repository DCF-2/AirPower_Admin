package com.ifpe.edu.br.view.ui.screens


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupDeviceWizard(viewModel: AdminViewModel) {
    val step by viewModel.wizardStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val theme = MaterialTheme.colorScheme

    // Permissão de GPS (Necessária para Wi-Fi e Localização)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (step == 1) viewModel.scanForAuthorizedNetworks()
            if (step == 2) viewModel.captureLocationInBackground()
        }
    }

    // Pede permissão logo no Passo 1
    LaunchedEffect(step) {
        if (step == 1 || step == 2) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                if (step == 1) viewModel.scanForAuthorizedNetworks()
                if (step == 2) viewModel.captureLocationInBackground()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // --- CABEÇALHO DO WIZARD ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1 && step < 5) {
                IconButton(onClick = { viewModel.previousStep() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            }
            Text(
                text = "Configuração do Sensor (Passo $step/6)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.onBackground,
                modifier = Modifier.padding(start = if (step > 1 && step < 6) 0.dp else 16.dp)
            )
        }

        LinearProgressIndicator(
            progress = { step / 6f },
            modifier = Modifier.fillMaxWidth(),
            color = theme.primary,
        )

        // --- CONTEÚDO DINÂMICO ---
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (step) {
                1 -> Step1WifiScan(viewModel)
                2 -> Step2LocationAndThingsBoard(viewModel, isLoading)
                3 -> Step3HotspotInstructions(viewModel)
                4 -> Step4EspDiscovery(viewModel)
                5 -> Step5ValidationAndTerminal(viewModel)
                6 -> Step6FinalTelemetry(viewModel)
            }
        }
    }
}

@Composable
fun Step1WifiScan(viewModel: AdminViewModel) {
    val availableNetworks by viewModel.availableNetworks.collectAsState()
    val isScanning by viewModel.isScanningWifi.collectAsState()
    val status by viewModel.provisioningStatus.collectAsState()
    val theme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Rede Wi-Fi do Ambiente", style = MaterialTheme.typography.headlineSmall)
                Text("Escolha a rede onde o sensor vai operar.", style = MaterialTheme.typography.bodyMedium)
            }

            // O Botão de Atualizar (Desativa enquanto já estiver a procurar)
            IconButton(
                onClick = { viewModel.scanForAuthorizedNetworks() },
                enabled = !isScanning
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Atualizar Wi-Fi",
                    tint = if (isScanning) Color.Gray else theme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isScanning) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Cruzando dados da API com o ambiente...")
                }
            }
        } else if (availableNetworks.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(status, color = theme.onErrorContainer)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableNetworks) { network ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectNetworkAndProceed(network) },
                        colors = CardDefaults.cardColors(containerColor = theme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi", tint = theme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(network.ssid, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Sinal: ${network.signalLevel ?: "N/A"} dBm", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selecionar", tint = theme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step2LocationAndThingsBoard(viewModel: AdminViewModel, isLoading: Boolean) {
    var locationInput by remember { mutableStateOf(viewModel.locationName) }
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val theme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Localização e Registo", style = MaterialTheme.typography.headlineSmall)
        Text("Onde este sensor vai ficar?", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = locationInput,
            onValueChange = { locationInput = it },
            label = { Text("Nome do Local (Ex: Laboratório 1)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        RectButton(
            text = "Buscar Dispositivo",
            onClick = { viewModel.checkLocationAndFindDevice(locationInput) },
            modifier = Modifier.fillMaxWidth()
        )

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (selectedDevice != null) {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = theme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Dispositivo Encontrado na Nuvem!", style = MaterialTheme.typography.labelLarge)
                        Text(selectedDevice!!.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.nextStep() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Vincular a este dispositivo") }
                    }
                }
            } else if (locationInput.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Nenhum dispositivo encontrado para este local.", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        // 1. Atualizamos a variável mestre do ViewModel com o que o usuário digitou
                        viewModel.locationName = locationInput
                        // 2. Chamamos a função de criar apenas com o nome gerado!
                        viewModel.createDevice("AirPower $locationInput")
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Criar Novo no Servidor") }
            }
        }
    }
}

@Composable
fun Step3HotspotInstructions(viewModel: AdminViewModel) {
    val theme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = theme.primaryContainer)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Router, contentDescription = "Hotspot", modifier = Modifier.size(64.dp), tint = theme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Atenção Necessária", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("1. Ative o Hotspot do celular agora.", style = MaterialTheme.typography.bodyLarge)
                Text("2. O SSID (Nome) deve ser: airp", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("3. A senha deve ser: suicr7ap", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("4. Ligue a placa ESP32 na tomada.", style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.startUdpListener() // Começa a ouvir UDP antes de mudar de tela!
                        viewModel.nextStep()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Já liguei, Continuar") }
            }
        }
    }
}

@Composable
fun Step4EspDiscovery(viewModel: AdminViewModel) {
    val discoveredEsps by viewModel.discoveredEsps.collectAsState()
    val isSearching by viewModel.isSearchingEsps.collectAsState()
    val theme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Escolha a Placa (ESP32)", style = MaterialTheme.typography.headlineSmall)
        Text("Aguardando dispositivos entrarem no Hotspot...", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        if (isSearching && discoveredEsps.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(discoveredEsps) { esp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.lockEsp32(esp.id) // 🔒 TRAVA UDP APLICADA!
                                viewModel.stopUdpListener()
                                viewModel.nextStep()
                            },
                        colors = CardDefaults.cardColors(containerColor = theme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = "Placa", tint = if (esp.isBlinking) Color.Yellow else theme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ID: ${esp.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("IP: ${esp.ip}", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { viewModel.testBlinkLed(esp.ip) }) {
                                Text("Piscar LED")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step5ValidationAndTerminal(viewModel: AdminViewModel) {
    val logs by viewModel.provisioningLogs.collectAsState()
    val status by viewModel.provisioningStatus.collectAsState()
    val theme = MaterialTheme.colorScheme
    val isSocketSuccess = status == "SUCESSO_SOCKET" || status.contains("FINALIZADO")

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Validação e Envio", style = MaterialTheme.typography.headlineSmall)
        Text("Confira os dados e inicie o envio.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        // --- RESUMO ---
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = theme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("📡 Wi-Fi Destino: ${viewModel.targetWifiSsid}", style = MaterialTheme.typography.bodyMedium)
                Text("📍 Local: ${viewModel.locationName}", style = MaterialTheme.typography.bodyMedium)
                Text("☁️ Dispositivo: ${viewModel.selectedDevice.value?.name ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text("🔒 Placa Alvo (Lock): ${viewModel.targetEspId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- TERMINAL VERDE ---
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                items(logs) { logMsg ->
                    Text(
                        text = logMsg,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Mostra o status atual como última linha (se não for SUCESSO_SOCKET)
                if (!isSocketSuccess) {
                    item {
                        Text(text = "> STATUS: $status", color = Color.Yellow, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- BOTÕES DE AÇÃO DO PASSO 5 ---
        if (!isSocketSuccess) {
            Button(
                onClick = { viewModel.sendConfigurationToEsp() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) { Text("INICIAR PROVISIONAMENTO") }
        } else {
            Button(
                onClick = { viewModel.nextStep() }, // <--- AGORA ELE AVANÇA PARA O PASSO 6!
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("Avançar para Localização") }
        }
    }
} // <-- FECHA O PASSO 5 AQUI!

// ---> PASSO 6 COMEÇA AQUI, COMPLETAMENTE SEPARADO! <---
@Composable
fun Step6FinalTelemetry(viewModel: AdminViewModel) {
    val theme = MaterialTheme.colorScheme
    val status by viewModel.provisioningStatus.collectAsState()
    var descriptionInput by remember { mutableStateOf(viewModel.locationDescription) }
    val isSaved = status == "TELEMETRIA_SALVA"

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Geolocalização Final", style = MaterialTheme.typography.headlineSmall)
        Text("Vincule as coordenadas ao ThingsBoard.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        if (!isSaved) {
            // AVISO IMPORTANTE SOBRE A INTERNET
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ ATENÇÃO", style = MaterialTheme.typography.titleMedium, color = theme.onErrorContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("O seu celular ainda pode estar conectado ao Hotspot da placa.", color = theme.onErrorContainer)
                    Text("Desligue o Hotspot e ative o Wifi novamente antes de enviar!", fontWeight = FontWeight.Bold, color = theme.onErrorContainer)
                }
            }

            Spacer(Modifier.height(24.dp))

            // DADOS CAPTURADOS
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📍 Latitude: ${viewModel.locationLat ?: "Não capturada"}", style = MaterialTheme.typography.bodyLarge)
                    Text("📍 Longitude: ${viewModel.locationLon ?: "Não capturada"}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(16.dp))

            // INPUT DE DESCRIÇÃO
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Descrição (Ex: Laboratório DEXTER)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.weight(1f))

            if (status.contains("Erro")) {
                Text(status, color = theme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            Button(
                onClick = {
                    viewModel.locationDescription = descriptionInput
                    viewModel.saveLocationToThingsBoard()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) { Text("Salvar Telemetria no Servidor") }

        } else {
            // TELA DE SUCESSO TOTAL
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Sucesso", modifier = Modifier.size(100.dp), tint = Color(0xFF4CAF50))
                Spacer(Modifier.height(16.dp))
                Text("Tudo Pronto!", style = MaterialTheme.typography.headlineMedium)
                Text("Sensor online e geolocalizado com sucesso.")

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.resetWizard() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Voltar ao Início") }
            }
        }
    }
}