package com.ifpe.edu.br.view.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ifpe.edu.br.R
import com.ifpe.edu.br.viewmodel.AdminViewModel

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(viewModel: AdminViewModel) {
    val devices by viewModel.devicesList.collectAsState()
    val locations by viewModel.deviceLocations.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 1. Controle de Permissão
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    // Configuração da Câmera
    val cameraPositionState = rememberCameraPositionState()

    // Inicialização
    LaunchedEffect(Unit) {
        MapsInitializer.initialize(context)
        if (devices.isEmpty()) {
            viewModel.fetchDevices()
        }
    }

    // Se tiver permissão, busca a localização e move a câmera
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    // Move a câmera para a localização real com um zoom aproximado
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
                }
            }
        }
    }

    // Ícone personalizado dos sensores
    val deviceIconDescriptor = remember { mutableStateOf<BitmapDescriptor?>(null) }
    LaunchedEffect(Unit) {
        try {
            val original = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
            if (original != null) {
                val scaled = Bitmap.createScaledBitmap(original, 120, 120, false)
                deviceIconDescriptor.value = BitmapDescriptorFactory.fromBitmap(scaled)
            } else {
                deviceIconDescriptor.value = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            }
        } catch (e: Exception) {
            deviceIconDescriptor.value = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (hasLocationPermission) {
            // --- MOSTRA O MAPA ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true,
                    mapToolbarEnabled = false
                )
            ) {
                // Desenha os pinos que vieram da telemetria
                devices.forEach { device ->
                    val deviceId = device.id.id
                    val locationInfo = locations[deviceId]

                    if (locationInfo != null) {
                        val position = LatLng(locationInfo.latitude, locationInfo.longitude)
                        Marker(
                            state = MarkerState(position = position),
                            title = device.name,
                            snippet = "Sensor Ativo",
                            icon = deviceIconDescriptor.value
                        )
                    }
                }
            }

            // Contador de Dispositivos Localizados
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "${locations.size} / ${devices.size} Localizados",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        } else {
            // --- TELA DE PEDIDO DE PERMISSÃO ---
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Precisamos da sua localização",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Isso permite ver onde você está no mapa.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Permitir Acesso")
                }
            }
        }
    }
}