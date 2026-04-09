package com.ifpe.edu.br.view.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.viewmodel.AdminViewModel
import kotlinx.coroutines.delay
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.entryModelOf


@Composable
fun DeviceDashboardScreen(
    device: ThingsBoardDevice,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    // Agora temos 3 abas! (0 = Painel, 1 = Controle, 2 = Análise)
    var selectedTab by remember { mutableStateOf(0) }
    val theme = MaterialTheme.colorScheme

    val telemetryMap by viewModel.currentDeviceTelemetry.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // O estado da taxa de atualização fica aqui para alimentar o LaunchedEffect
    var refreshRateMillis by remember { mutableLongStateOf(5000L) }

    // O "Motor" do Dashboard (Ciclo infinito que respeita o Slider)
    LaunchedEffect(refreshRateMillis) {
        while(true) {
            viewModel.fetchDeviceTelemetry(device.id.id)
            delay(refreshRateMillis)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // --- HEADER ---
        Surface(color = theme.surface, shadowElevation = 4.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
                    Column {
                        Text(device.name ?: "Dispositivo", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(device.label ?: "Sem descrição", color = theme.outline, fontSize = 14.sp)
                    }
                }

                // --- ABAS (TABS) ---
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Painel") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Controle") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Análise") })
                }
            }
        }

        // --- CONTEÚDO DAS ABAS ---
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (isLoading && telemetryMap.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> TelemetryAutoGrid(telemetryMap)
                    1 -> AirConditionerRemoteControl(deviceId = device.id.id, viewModel = viewModel)
                    2 -> TelemetryChartsTab(
                        viewModel = viewModel,
                        refreshRate = refreshRateMillis,
                        onRefreshRateChange = { refreshRateMillis = it }
                    )
                }
            }
        }
    }
}

// =================================================================
// ABA 1: GERADOR AUTOMÁTICO DE TELEMETRIA
// =================================================================
@Composable
fun TelemetryAutoGrid(telemetryMap: Map<String, String>) {
    if (telemetryMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum dado de telemetria recebido.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        gridItems(telemetryMap.entries.toList()) { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 110.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = when {
                        entry.key.contains("Temp", true) -> Icons.Default.Thermostat
                        entry.key.contains("Umid", true) || entry.key.contains("Humi", true) -> Icons.Default.WaterDrop
                        entry.key.contains("IP", true) -> Icons.Default.Wifi
                        entry.key.contains("Tens", true) || entry.key.contains("Volt", true) -> Icons.Default.FlashOn
                        entry.key.contains("Lat", true) || entry.key.contains("Lon", true) -> Icons.Default.LocationOn
                        entry.key.contains("Data", true) || entry.key.contains("Hora", true) || entry.key.contains("Time", true) -> Icons.Default.Schedule
                        else -> Icons.Default.Sensors
                    }
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = entry.key.uppercase(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = entry.value,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// =================================================================
// ABA 2: CONTROLE NATIVO DE AR CONDICIONADO
// =================================================================
@Composable
fun AirConditionerRemoteControl(deviceId: String, viewModel: AdminViewModel) {
    val powerState by viewModel.devicePowerState.collectAsState()
    var targetTemp by remember { mutableStateOf(23) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (powerState) Color(0xFFE3F2FD) else Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Alvo", color = Color.Gray, fontSize = 14.sp)
                Text(if (powerState) "$targetTemp°" else "--", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = if(powerState) Color(0xFF1976D2) else Color.Gray)
                Text("Ar Condicionado", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = {
                    if(powerState && targetTemp > 16) {
                        targetTemp--
                        viewModel.sendRpcCommand(deviceId, "setTemperature", targetTemp)
                    }
                },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) { Icon(Icons.Default.Remove, contentDescription = "Diminuir", modifier = Modifier.size(32.dp)) }

            FilledIconButton(
                onClick = {
                    viewModel.sendRpcCommand(deviceId, "setPower", !powerState)
                },
                modifier = Modifier.size(80.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (powerState) Color(0xFF4CAF50) else Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) { Icon(Icons.Default.PowerSettingsNew, contentDescription = "Ligar/Desligar", modifier = Modifier.size(40.dp)) }

            FilledIconButton(
                onClick = {
                    if(powerState && targetTemp < 30) {
                        targetTemp++
                        viewModel.sendRpcCommand(deviceId, "setTemperature", targetTemp)
                    }
                },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) { Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(32.dp)) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { viewModel.sendRpcCommand(deviceId, "setMode", "cool") }) { Text("Modo Frio") }
            OutlinedButton(onClick = { viewModel.sendRpcCommand(deviceId, "setFanSpeed", "high") }) { Text("Vento Alto") }
        }
    }
}

// =================================================================
// ABA 3: GRÁFICOS E ANÁLISE DE TELEMETRIA
// =================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryChartsTab(
    viewModel: AdminViewModel,
    refreshRate: Long,
    onRefreshRateChange: (Long) -> Unit
) {
    val historyMap by viewModel.telemetryHistory.collectAsState()

    // Filtra apenas as chaves que têm valores numéricos (ignora strings como "description" ou "statusluz")
    val availableMetrics = historyMap.filterValues { it.isNotEmpty() }.keys.toList()

    var selectedMetrics by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(availableMetrics) {
        if (selectedMetrics.isEmpty() && availableMetrics.isNotEmpty()) {
            selectedMetrics = setOf(availableMetrics.first())
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Filtros de Telemetria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (availableMetrics.isEmpty()) {
            Text("A aguardar dados numéricos...", color = MaterialTheme.colorScheme.outline)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableMetrics) { metric ->
                    FilterChip(
                        selected = selectedMetrics.contains(metric),
                        onClick = {
                            selectedMetrics = if (selectedMetrics.contains(metric)) {
                                if (selectedMetrics.size > 1) selectedMetrics - metric else selectedMetrics
                            } else {
                                selectedMetrics + metric
                            }
                        },
                        label = { Text(metric) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Taxa de Atualização: ${refreshRate / 1000}s", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = refreshRate.toFloat(),
            onValueChange = { onRefreshRateChange(it.toLong()) },
            valueRange = 1000f..10000f,
            steps = 8
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val dataToDraw = historyMap.filterKeys { it in selectedMetrics }

                if (dataToDraw.isEmpty()) {
                    Text("Selecione uma métrica", color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.Center))
                } else {
                    // --- O GRÁFICO VICO ENTRA AQUI ---

                    // 1. Converte a nossa lista de Floats no formato que o Vico entende (FloatEntry)
                    val chartEntries = dataToDraw.values.map { floatList ->
                        floatList.mapIndexed { index, value -> FloatEntry(x = index.toFloat(), y = value) }
                    }

                    // 2. Só desenhamos o gráfico se houver pontos suficientes (pelo menos 1 ponto)
                    if (chartEntries.isNotEmpty() && chartEntries.first().isNotEmpty()) {

                        // Cria o modelo de dados para o Vico
                        val chartEntryModel = entryModelOf(*chartEntries.toTypedArray())

                        // 3. Define as cores das linhas para o cruzamento de dados (até 3 cores distintas)
                        val lineColors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.error
                        )

                        // 4. O Componente Gráfico do Vico
                        Chart(
                            chart = lineChart(
                                lines = chartEntries.indices.map { index ->
                                    lineSpec(lineColor = lineColors[index % lineColors.size])
                                }
                            ),
                            model = chartEntryModel,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}