package com.ifpe.edu.br.view.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo

@Composable
fun DashboardCard(
    dashboard: DashboardInfo,
    onClick: () -> Unit // O que acontece quando clica (Abrir a WebView!)
) {
    val theme = MaterialTheme.colorScheme

    CustomCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clip(RoundedCornerShape(AirPowerTheme.dimens.cardCornerRadius))
            .background(theme.primaryContainer)
            .clickable { onClick() },
        layouts = listOf {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Ícone genérico de Gráfico/Dashboard
                Icon(
                    imageVector = Icons.Default.Info, // Pode trocar pelo seu ícone R.drawable se preferir
                    contentDescription = "Dashboard Icon",
                    tint = theme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                // Nome do Dashboard (Ex: "Painel Geral do Laboratório")
                CustomText(
                    text = dashboard.title,
                    fontStyle = AirPowerTheme.typography.displayMedium,
                    color = theme.onPrimaryContainer
                )
            }
        }
    )
}