/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
package com.ifpe.edu.br.view.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            CustomText(
                text = "Configuração de Rede",
                fontSize = 20.sp,
                color = tb_primary_light,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = localIp,
                onValueChange = { localIp = it },
                label = { Text("Endereço Local (Wi-Fi)", color = tb_primary_light) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = vpnIp,
                onValueChange = { vpnIp = it },
                label = { Text("Endereço VPN", color = tb_primary_light) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Forçar uso do endereço VPN",
                    modifier = Modifier.weight(1f),
                    color = tb_primary_light
                )
                Switch(
                    checked = forceVpn,
                    onCheckedChange = { forceVpn = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            RectButton(
                text = "Salvar",
                onClick = {
                    SharedPrefManager.getInstance().setServerIps(localIp, vpnIp)
                    SharedPrefManager.getInstance().isForceVpn = forceVpn
                    Toast.makeText(context, "Configurações salvas!", Toast.LENGTH_SHORT).show()
                    onSave()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}