package com.ifpe.edu.br.view.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

@Composable
fun MapScreen(viewModel: AdminViewModel) {
    val devices by viewModel.devicesList.collectAsState()
    // Observa as localizações reais carregadas
    val locations by viewModel.deviceLocations.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        MapsInitializer.initialize(context)
        // Se a lista estiver vazia, puxa tudo (devices + locations)
        if (devices.isEmpty()) {
            viewModel.fetchDevices()
        }
    }

    // Configuração Inicial da Câmera (Recife)
    val recife = LatLng(-8.05428, -34.8813)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(recife, 12f)
    }

    // Carrega dados se a lista estiver vazia
    LaunchedEffect(Unit) {
        if (devices.isEmpty()) {
            viewModel.fetchDevices()
        }
    }

    // --- LÓGICA DE ÍCONE ---
    // Usamos um estado mutável para guardar o ícone, pois ele pode demorar a carregar
    // e só pode ser criado DEPOIS que o MapsInitializer rodar.
    val deviceIconDescriptor = remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(Unit) {
        try {
            // Tenta carregar 'ic_air_conditioner'
            // Se não tiver a imagem, coloque um png qualquer em res/drawable ou use R.drawable.ic_launcher_foreground para testar
            val resourceId = R.drawable.ic_launcher_foreground

            val original = BitmapFactory.decodeResource(context.resources, resourceId)
            if (original != null) {
                val scaled = Bitmap.createScaledBitmap(original, 120, 120, false)
                deviceIconDescriptor.value = BitmapDescriptorFactory.fromBitmap(scaled)
            } else {
                deviceIconDescriptor.value = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            }
        } catch (e: Exception) {
            // Mesmo no erro, garante que o mapa já inicializou antes de chamar isso
            try {
                deviceIconDescriptor.value = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            } catch (e2: Exception) {
                // Se falhar aqui, o mapa ainda não iniciou, o ícone ficará null (padrão vermelho)
            }
        }
    }

    // Configurações visuais do Mapa
    val uiSettings = remember {
        MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
    }
    val properties = remember {
        MapProperties(isMyLocationEnabled = false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings
        ) {
            // Itera sobre a lista de dispositivos
            devices.forEach { device ->
                // Tenta pegar a localização real deste dispositivo
                val realLocation = locations[device.id.id]

                // SÓ DESENHA SE TIVER LOCALIZAÇÃO
                if (realLocation != null) {
                    Marker(
                        state = MarkerState(position = realLocation),
                        title = device.name,
                        snippet = "Tipo: ${device.type} | ${device.label ?: ""}",
                        icon = deviceIconDescriptor.value
                    )
                }
            }
        }

        // Contador: Mostra quantos estão no mapa vs total
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            shadowElevation = 4.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                // Ex: "5 / 20 Dispositivos no Mapa"
                text = "${locations.size} / ${devices.size} Localizados",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}