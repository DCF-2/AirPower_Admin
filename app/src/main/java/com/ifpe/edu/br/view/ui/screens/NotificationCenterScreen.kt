package com.ifpe.edu.br.view.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ifpe.edu.br.common.CommonConstants
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomColumn
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.ui.theme.cardBackgroundGradientDark
import com.ifpe.edu.br.common.ui.theme.cardBackgroundGradientLight
import com.ifpe.edu.br.common.ui.theme.cardCornerRadius
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.repository.remote.dto.AirPowerNotificationItem
import com.ifpe.edu.br.view.ui.components.NotificationCard
import com.ifpe.edu.br.view.ui.theme.tb_primary_light
import com.ifpe.edu.br.viewmodel.AirPowerViewModel

/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
@Composable
fun NotificationCenterScreen(
    navController: NavHostController,
    mainViewModel: AirPowerViewModel
) {
    val notification = mainViewModel.getNotifications().collectAsState()
    CustomColumn(
        modifier = Modifier.fillMaxSize(),
        alignmentStrategy = CommonConstants.Ui.ALIGNMENT_TOP,
        layouts = listOf {
            if (notification.value.isEmpty()) {
                EmptyStateNotificationCard()
            } else {
                NotificationGrid(
                    notificationSet = notification.value,
                    viewModel = mainViewModel
                )
            }
        }
    )
}

@Composable
fun EmptyStateNotificationCard() {
    Box(
        modifier = Modifier
            .background(Color.Transparent)
    ) {
        CustomCard(
            modifier = Modifier
                .clip(RoundedCornerShape(cardCornerRadius))
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isSystemInDarkTheme()) cardBackgroundGradientDark else cardBackgroundGradientLight,
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                ),
            layouts = listOf {
                Spacer(modifier = Modifier.padding(vertical = 65.dp))
                CustomText(
                    color = tb_primary_light,
                    text = "Você não possui notificações",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.padding(vertical = 65.dp))
            }
        )
    }
}

@Composable
private fun NotificationGrid(
    notificationSet: List<AirPowerNotificationItem>,
    viewModel: AirPowerViewModel
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notificationSet) { notification ->
            NotificationCard(
                item = notification,
                onClick = {
                    if (notification.status != Constants.NotificationState.READ) {
                        viewModel.markNotificationAsRead(notification.id)
                    }
                }
            )
        }
    }
}