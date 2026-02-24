package com.ifpe.edu.br.view.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.Constants.Constants.THINGS_BOARD_BASE_URL
import com.ifpe.edu.br.viewmodel.AdminViewModel

@Composable
fun SettingsScreen(viewModel: AdminViewModel, onLogout: () -> Unit) {
    val theme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Configurações", style = MaterialTheme.typography.headlineMedium, color = theme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))

        // --- Seção de Perfil ---
        SettingsSection(title = "Conta") {
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Usuário",
                value = "tenant@thingsboard.org" // Poderia vir do ViewModel
            )
            Divider(color = theme.outlineVariant)
            SettingsItem(
                icon = Icons.Default.Settings,
                title = "Permissão",
                value = "Administrador do Tenant"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Seção de Conexão ---
        SettingsSection(title = "Conexão e Servidor") {
            SettingsItem(
                icon = Icons.Default.Wifi,
                title = "Servidor Atual",
                value = THINGS_BOARD_BASE_URL
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Seção Sobre ---
        SettingsSection(title = "Sobre o App") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Versão",
                value = "1.0.0 (Admin Build)"
            )
            Divider(color = theme.outlineVariant)
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Desenvolvido por",
                value = "Equipe AirPower"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        RectButton(
            text = "Sair da Conta",
            onClick = {
                viewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = theme.error)
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
    CustomCard(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
        layouts = listOf {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}