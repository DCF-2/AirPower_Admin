package com.ifpe.edu.br.view.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
import com.ifpe.edu.br.viewmodel.AdminViewModel
import kotlinx.coroutines.delay

@Composable
fun DeviceListScreen(viewModel: AdminViewModel) {
    val devices by viewModel.devicesList.collectAsState()

    // Estados da Tela
    var searchQuery by remember { mutableStateOf("") }
    var deviceToShowDetails by remember { mutableStateOf<ThingsBoardDevice?>(null) }
    var deviceToDelete by remember { mutableStateOf<ThingsBoardDevice?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.fetchDevices()
            // Aguarda 10 segundos e repete o ciclo infinito silenciosamente
            // 10 segundos é o tempo ideal para ser "tempo real" sem sobrecarregar o servidor
            delay(10000)
        }
    }

    // Filtra a lista com base na barra de pesquisa (ignora maiúsculas/minúsculas)
    val filteredDevices = devices.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                (it.type?.contains(searchQuery, ignoreCase = true) == true)
    }

    // --- MODAL DE EXCLUSÃO (PERIGO) ---
    if (deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Excluir Permanentemente?") },
            text = {
                Text(
                    "Tem certeza que deseja apagar o dispositivo '${deviceToDelete?.name}'?\n\n" +
                            "Esta ação não tem volta. Todos os históricos de telemetria e vínculos serão perdidos."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDevice(deviceToDelete!!.id.id)
                        deviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sim, Apagar", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // --- MODAL DE DETALHES ---
    if (deviceToShowDetails != null) {
        DeviceDetailsModal(
            device = deviceToShowDetails!!,
            viewModel = viewModel,
            onDismiss = { deviceToShowDetails = null }
        )
    }

    // --- CORPO DA TELA ---
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // 1. Barra de Pesquisa
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar dispositivo ou tipo...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Cabeçalho e Contador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meus Dispositivos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Novo Contador (Estilo "Pill" moderno com ícone)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // Fundo suave
                        shape = RoundedCornerShape(50) // Arredondamento total (Pílula)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Router, // O mesmo ícone dos cards, ou use Icons.Default.Devices
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${filteredDevices.size} Registrados",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Lista de Dispositivos (Cards)
        if (filteredDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isNotEmpty()) "Nenhum resultado para a busca." else "Nenhum dispositivo encontrado.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredDevices) { device ->
                    DeviceCardItem(
                        device = device,
                        onClick = {
                            viewModel.fetchDeviceDetails(device.id.id)
                            deviceToShowDetails = device
                        },
                        onDeleteClick = { deviceToDelete = device }
                    )
                }
            }
        }
    }
}

// Subcomponente: O Visual do Card
@Composable
fun DeviceCardItem(
    device: ThingsBoardDevice,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Trata valores nulos com padrões seguros
    val isActive = device.active ?: false // Pega do JSON ("active": true/false)
    val typeName = device.type ?: "Sensor IoT" // Pega do JSON o tipo de perfil

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do Dispositivo (Arredondado)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Router, // Ícone moderno
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos (Nome, Tipo e Status)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Bolinha de Status (Verde = Ativo, Cinza = Inativo)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isActive) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isActive) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }

            // Lixeira
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Deletar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// O Modal de detalhes permanece igual ao anterior
@Composable
fun DeviceDetailsModal(
    device: ThingsBoardDevice,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val telemetry by viewModel.selectedDeviceTelemetry.collectAsState()
    val attributes by viewModel.selectedDeviceAttributes.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(device.name, style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Última Telemetria:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (telemetry.isEmpty()) Text("Sem dados recentes.", style = MaterialTheme.typography.bodySmall)
                telemetry.forEach { (key, value) ->
                    Text("• $key: $value", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Atributos de Servidor:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (attributes.isEmpty()) Text("Nenhum atributo definido.", style = MaterialTheme.typography.bodySmall)
                attributes.forEach { (key, value) ->
                    Text("• $key: $value", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar")
                }
            }
        }
    }
}