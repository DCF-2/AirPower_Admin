package com.ifpe.edu.br.view.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AdminMainScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Home") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen != "Map",
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                // Header do Menu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Administrador", style = MaterialTheme.typography.titleMedium)
                        Text("tenant@thingsboard.org", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Itens do Menu
                NavigationItem(
                    label = "Início",
                    icon = Icons.Outlined.Home,
                    selected = currentScreen == "Home",
                    onClick = { currentScreen = "Home"; scope.launch { drawerState.close() } }
                )
                NavigationItem(
                    label = "Instalar Sensor", // Nome moderno para Provisionar
                    icon = Icons.Outlined.AddCircle, // Ícone genérico se não tiver wifi
                    selected = currentScreen == "Setup",
                    onClick = { currentScreen = "Setup"; scope.launch { drawerState.close() } }
                )
                NavigationItem(
                    label = "Dispositivos",
                    icon = Icons.Outlined.Devices,
                    selected = currentScreen == "Devices",
                    onClick = { currentScreen = "Devices"; scope.launch { drawerState.close() } }
                )
                NavigationItem(
                    label = "Mapa",
                    icon = Icons.Default.Map,
                    selected = currentScreen == "Map",
                    onClick = { currentScreen = "Map"; scope.launch { drawerState.close() } }
                )

                NavigationItem(
                    label = "Configurações",
                    icon = Icons.Default.Settings,
                    selected = currentScreen == "Settings",
                    onClick = { currentScreen = "Settings"; scope.launch { drawerState.close() } }
                )
                NavigationItem(
                    label = "Sair",
                    icon = Icons.Outlined.ExitToApp,
                    selected = false,
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when(currentScreen) {
                                "Home" -> "AirPower Admin"
                                "Setup" -> "Nova Instalação"
                                "Devices" -> "Meus Dispositivos"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.fetchDevices() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)) {
                when (currentScreen) {
                    "Home" -> AdminHomeScreen(
                        onNavigateToSetup = { currentScreen = "Setup" },
                        onNavigateToDevices = { currentScreen = "Devices" },
                        onNavigateToMap = { currentScreen = "Map" },
                        onNavigateToSettings = { currentScreen = "Settings" }
                    )
                    "Setup" -> SetupDeviceWizard(viewModel)
                    "Devices" -> DeviceListScreen(viewModel)
                    "Map" -> MapScreen(viewModel)
                    "Settings" -> SettingsScreen(viewModel, onLogout)
                }
            }
        }
    }
}

@Composable
fun NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null, tint = if(selected) MaterialTheme.colorScheme.primary else color) },
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedContainerColor = Color.Transparent
        )
    )
}