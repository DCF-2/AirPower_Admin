package com.ifpe.edu.br.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.ifpe.edu.br.view.ui.components.DashboardCard
import java.util.Locale

@Composable
fun AdminHomeScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboards: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Texto de boas vindas
        Text(
            "Painel de Controle",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Gerencie os sensores AirPower",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Variáveis de estado e serviços (Coloque isto antes do seu Card)
        val context = LocalContext.current
        var currentLocationName by remember { mutableStateOf("Buscando...") }
        val fusedLocationClient =
            remember { LocationServices.getFusedLocationProviderClient(context) }

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 2. Efeito que traduz o GPS para Texto (Reverse Geocoding)
        LaunchedEffect(hasLocationPermission) {
            if (hasLocationPermission) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val geocoder = Geocoder(context, Locale.getDefault())

                            // O Android 13 (Tiramisu) mudou a forma como o Geocoder funciona, precisamos tratar as duas formas:
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1
                                ) { addresses ->
                                    if (addresses.isNotEmpty()) {
                                        val address = addresses[0]
                                        val city =
                                            address.subAdminArea ?: address.locality ?: "Local"
                                        currentLocationName = city
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1
                                )
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val city = address.subAdminArea ?: address.locality ?: "Local"
                                    currentLocationName = city
                                }
                            }
                        } else {
                            currentLocationName = "GPS Indisponível"
                        }
                    }
                } catch (e: Exception) {
                    currentLocationName = "Erro ao buscar local"
                }
            } else {
                currentLocationName = "Localização Protegida"
            }
        }

        // 3. O seu Card atualizado
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Localização Atual",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = currentLocationName, // <--- A MÁGICA ACONTECE AQUI!
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1ª Linha: Novo Sensor e Dispositivos
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = "Novo Sensor", subtitle = "Configurar e instalar",
                icon = Icons.Default.Add, modifier = Modifier.weight(1f),
                onClick = onNavigateToSetup
            )
            DashboardCard(
                title = "Dispositivos", subtitle = "Ver lista completa",
                icon = Icons.Default.Devices, modifier = Modifier.weight(1f),
                onClick = onNavigateToDevices
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2ª Linha: Mapas e Dashboards (Visualização)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = "Mapa", subtitle = "Localização GPS",
                icon = Icons.Default.Map, modifier = Modifier.weight(1f),
                onClick = onNavigateToMap
            )
            DashboardCard(
                title = "Dashboards", subtitle = "Painel de Gráficos",
                icon = Icons.Default.Dashboard, modifier = Modifier.weight(1f),
                onClick = onNavigateToDashboards
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3ª Linha: Ajustes (Ocupando a largura inteira ou junta com outro se quiser)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = "Ajustes", subtitle = "Configurações do App",
                icon = Icons.Default.Settings, modifier = Modifier.weight(1f),
                onClick = onNavigateToSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}