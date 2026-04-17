    package com.ifpe.edu.br.view.ui.screens

    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Info
    import androidx.compose.material.icons.filled.Router
    import androidx.compose.material.icons.filled.Search
    import androidx.compose.material.icons.outlined.Dashboard
    import androidx.compose.material.icons.outlined.Info
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.window.Dialog
    import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice
    import com.ifpe.edu.br.viewmodel.AdminViewModel
    import kotlinx.coroutines.delay

    @Composable
    fun DeviceListScreen(
        viewModel: AdminViewModel,
        onNavigateToDashboard: () -> Unit
    ) {
        val devices by viewModel.devicesList.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        var searchQuery by remember { mutableStateOf("") }
        var deviceToShowDetails by remember { mutableStateOf<ThingsBoardDevice?>(null) }

        // Atualiza a lista a cada 10 segundos
        LaunchedEffect(Unit) {
            while (true) {
                viewModel.fetchDevices()
                delay(10000)
            }
        }

        val filteredDevices = devices.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.type?.contains(searchQuery, ignoreCase = true) == true)
        }

        // Modal de Detalhes (Mantido para mostrar IDs completos e informações técnicas)
        if (deviceToShowDetails != null) {
            DeviceDetailsModal(
                device = deviceToShowDetails!!,
                viewModel = viewModel,
                onDismiss = { deviceToShowDetails = null },
                onOpenDashboard = {
                    deviceToShowDetails = null
                    onNavigateToDashboard()
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // --- BARRA DE PESQUISA ---
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

            // --- CABEÇALHO DA LISTA ---
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

                // Contador de dispositivos
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${filteredDevices.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LISTA DE CARTÕES ---
            if (isLoading && devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isNotEmpty()) "Nenhum resultado para a busca." else "Nenhum dispositivo encontrado.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredDevices) { device ->
                        DeviceCardItem(
                            device = device,
                            onDetailsClick = { deviceToShowDetails = device },
                            onDashboardClick = {
                                viewModel.selectDeviceForDashboard(device)
                                onNavigateToDashboard()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun DeviceCardItem(
        device: ThingsBoardDevice,
        onDetailsClick: () -> Unit,
        onDashboardClick: () -> Unit
    ) {
        val isActive = device.active ?: false
        val typeName = device.type ?: "Dispositivo IoT"
        val labelName = device.label.takeIf { !it.isNullOrBlank() } ?: "Sem descrição"

        // Cores dinâmicas baseadas no status
        val statusColor = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828) // Verde escuro / Vermelho escuro
        val statusBgColor = if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE) // Fundo Verde claro / Vermelho claro

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- LINHA SUPERIOR: ÍCONE, NOME E STATUS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícone do dispositivo
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Nome e Tipo
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = typeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Badge de Status (MUITO mais visual)
                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isActive) "ONLINE" else "OFFLINE",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // --- INFORMAÇÕES RESUMIDAS ---
                Text(
                    text = "Descrição: $labelName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Se no futuro tiver telemetria na lista, adicionaria aqui:
                // Text("Última temp: 24°C", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.height(16.dp))

                // --- LINHA DE AÇÕES (BOTÕES DIRETOS) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão de Detalhes (Texto)
                    TextButton(onClick = onDetailsClick) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Detalhes")
                    }

                    Spacer(Modifier.width(8.dp))

                    // Botão de Dashboard (Destaque)
                    Button(
                        onClick = onDashboardClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Outlined.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir Painel")
                    }
                }
            }
        }
    }

    // O Modal de detalhes permanece para exibir IDs e outras infos complexas
    @Composable
    fun DeviceDetailsModal(
        device: ThingsBoardDevice,
        viewModel: AdminViewModel,
        onDismiss: () -> Unit,
        onOpenDashboard: () -> Unit
    ) {
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

                    Text("Detalhes do Dispositivo:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("• Tipo: ${device.type ?: "Desconhecido"}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Label: ${device.label ?: "Não definido"}", style = MaterialTheme.typography.bodyMedium)
                    Text("• ID Interno: ${device.id.id}", style = MaterialTheme.typography.bodyMedium)

                    val isActive = device.active ?: false
                    Text("• Status: ${if (isActive) "Online" else "Offline"}", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.selectDeviceForDashboard(device)
                            onOpenDashboard()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Abrir Painel de Controle")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Fechar")
                    }
                }
            }
        }
    }