package com.ifpe.edu.br.view.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.R
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
    var currentScreen by remember { mutableStateOf("Provisionamento") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    "Menu Administrador",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )

                // Item 1: Provisionamento
                NavigationDrawerItem(
                    label = { Text(text = "Provisionar ESP32") },
                    selected = currentScreen == "Provisionamento",
                    onClick = {
                        currentScreen = "Provisionamento"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(painter = painterResource(id = R.drawable.wifi_icon), contentDescription = null) }, // Use um ícone adequado
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        selectedTextColor = MaterialTheme.colorScheme.secondary
                    )
                )

                // Item 2: Sair
                NavigationDrawerItem(
                    label = { Text(text = "Sair") },
                    selected = false,
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    icon = { Icon(painter = painterResource(id = R.drawable.arrow_back), contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen, style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        // Botão do Menu (Hambúrguer)
                        androidx.compose.material3.IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.menu_icon), // Garanta que este ícone exista
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    "Provisionamento" -> ProvisioningScreen(viewModel)
                    // Futuro: "Dashboard Admin", etc.
                }
            }
        }
    }
}