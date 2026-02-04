/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
package com.ifpe.edu.br.view.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.common.components.CustomInputText
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.repository.remote.dto.auth.AuthUser
import com.ifpe.edu.br.view.ui.theme.tb_primary_light

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigBottomSheet(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    var localIp by remember { mutableStateOf(SharedPrefManager.getInstance().localIp) }
    var vpnIp by remember { mutableStateOf(SharedPrefManager.getInstance().vpnIp) }
    var forceVpn by remember { mutableStateOf(SharedPrefManager.getInstance().isForceVpn) }

    val theme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomText(
                    text = "Configuração de Rede",
                    fontSize = 20.sp,
                    color = theme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val inputBackgroundColor = lerp(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.onPrimary,
                0.05f)

            val customSelectionColors = TextSelectionColors(
                handleColor = theme.secondary,
                backgroundColor = theme.secondary
            )

            CustomInputText(
                value = localIp,
                onValueChange = { localIp = it },
                label = "Endereço Local (Wi-Fi)",
                placeholder = "Digite o endereço IP e porta",
                inputFieldColors = TextFieldDefaults.colors(
                    focusedTextColor = theme.onSurface,
                    unfocusedTextColor = theme.onSurface,
                    focusedLabelColor = theme.onSurface,
                    unfocusedLabelColor = theme.onSurface,
                    focusedContainerColor = inputBackgroundColor,
                    unfocusedPlaceholderColor = theme.surface,
                    unfocusedContainerColor = inputBackgroundColor,
                    focusedIndicatorColor = theme.onSurface,
                    unfocusedIndicatorColor = theme.surface,
                    cursorColor = theme.secondary,
                    selectionColors = customSelectionColors
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            CustomInputText(
                value = vpnIp,
                onValueChange = { vpnIp = it },
                label = "Endereço VPN",
                placeholder = "Digite o endereço IP e porta",
                inputFieldColors = TextFieldDefaults.colors(
                    focusedTextColor = theme.onSurface,
                    unfocusedTextColor = theme.onSurface,
                    focusedLabelColor = theme.onSurface,
                    unfocusedLabelColor = theme.onSurface,
                    focusedContainerColor = inputBackgroundColor,
                    unfocusedPlaceholderColor = theme.surface,
                    unfocusedContainerColor = inputBackgroundColor,
                    focusedIndicatorColor = theme.onSurface,
                    unfocusedIndicatorColor = theme.surface,
                    cursorColor = theme.secondary,
                    selectionColors = customSelectionColors
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Forçar uso do endereço VPN",
                    modifier = Modifier.weight(1f),
                    color = theme.onSurface
                )
                Switch(
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = theme.onPrimary,
                        checkedTrackColor = theme.primary,
                    ),
                    checked = forceVpn,
                    onCheckedChange = { forceVpn = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            RectButton(
                colors = ButtonDefaults.buttonColors(
                    contentColor = theme.onPrimary,
                    containerColor = theme.primary,
                    disabledContentColor = theme.onPrimary,
                    disabledContainerColor = theme.primary
                ),
                text = "Salvar",
                fontSize = 15.sp,
                onClick = {
                    SharedPrefManager.getInstance().setServerIps(localIp, vpnIp)
                    SharedPrefManager.getInstance().setForceVpn(forceVpn)
                    Toast.makeText(context, "Configurações salvas!", Toast.LENGTH_SHORT).show()
                    onSave()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}