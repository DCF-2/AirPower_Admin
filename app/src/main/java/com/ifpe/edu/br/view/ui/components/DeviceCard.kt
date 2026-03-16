package com.ifpe.edu.br.view.ui.components

/*
* Refactored for: AirPower Admin (BFF Integration)
*/

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.R
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.components.ImageIcon
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.common.ui.theme.cardCornerRadius
import com.ifpe.edu.br.model.repository.remote.dto.ThingsBoardDevice

@Composable
fun DeviceCard(
    device: ThingsBoardDevice, // Mudamos de DeviceSummary para o modelo oficial!
    onClick: (deviceId: String) -> Unit, // O ID agora é String (ex-UUID)
) {
    CustomCard(
        modifier = Modifier
            .clip(RoundedCornerShape(cardCornerRadius))
            .wrapContentSize()
            .background(AirPowerTheme.color.primaryContainer)
            .clickable { onClick(device.id.id) },
        layouts = listOf {
            Column(
                modifier = Modifier.wrapContentSize()
            ) {
                // Substituído o SimpleRow por um Row nativo do Compose
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImageIcon(
                        description = "device icon",
                        iconResId = R.drawable.generic_device_icon,
                        modifier = Modifier.size(40.dp),
                        iconTint = AirPowerTheme.color.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    CustomText(
                        text = device.name,
                        fontStyle = AirPowerTheme.typography.bodyLarge,
                        alignment = TextAlign.Center,
                        color = AirPowerTheme.color.onPrimaryContainer,
                        modifier = Modifier.wrapContentWidth()
                    )
                }

                Spacer(modifier = Modifier.padding(vertical = AirPowerTheme.dimens.paddingSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trata a nulidade caso a API não devolva o status ativo
                    val isActive = device.active ?: false
                    val status = if (isActive) "online" else "offline"

                    CustomText(
                        text = status,
                        fontStyle = AirPowerTheme.typography.displayMedium,
                        color = getStatusColor(status),
                        alignment = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

@Composable
fun getStatusColor(status: String): Color {
    val isDark = isSystemInDarkTheme() // Mantivemos a sua lógica de tema noturno!
    return when (status.lowercase()) {
        "online" -> AirPowerTheme.color.onSecondaryContainer
        else -> AirPowerTheme.color.secondary
    }
}