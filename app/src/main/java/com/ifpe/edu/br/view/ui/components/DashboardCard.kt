// Trabalho de conclusão de curso - IFPE 2025
// Author: Willian Santos
// Project: AirPower Costumer

// Copyright (c) 2025 IFPE. All rights reserved.
package com.ifpe.edu.br.view.ui.components

import CustomBarChart
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomColumn
import com.ifpe.edu.br.common.components.CustomIconButton
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.model.repository.model.DashboardFilters
import com.ifpe.edu.br.model.repository.remote.dto.AlarmInfo
import com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggStrategy
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggregationRequest
import com.ifpe.edu.br.model.repository.remote.dto.agg.ChartDataWrapper
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.view.ui.screens.getTimeWrapper
import com.ifpe.edu.br.view.ui.theme.tb_primary_light
import com.ifpe.edu.br.viewmodel.AirPowerViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(
    dashboard: DashboardInfo,
    mainViewModel: AirPowerViewModel,
    allAlarms: List<AlarmInfo>
) {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var activeFilters by remember { mutableStateOf(DashboardFilters()) }
    var request by remember(activeFilters) {
        mutableStateOf(
            AggregationRequest(
                devicesIds = dashboard.devicesIds,
                aggStrategy = AggStrategy.AVG,
                aggKey = activeFilters.telemetryKey,
                timeIntervalWrapper = getTimeWrapper(System.currentTimeMillis(), activeFilters.interval)
            )
        )
    }

    val aggregatedDataState by mainViewModel.getAggregatedDataState(request).collectAsState()

    LaunchedEffect(request) {
        mainViewModel.fetchAggregatedData(request)
    }

    CustomCard(
        layouts = listOf {
            CustomColumn(
                modifier = Modifier.fillMaxSize(),
                layouts = listOf {
                    HeaderWithSettings(
                        title = dashboard.title,
                        onSettingsClick = { showSheet = true }
                    )

                    MainChart(aggregationState = aggregatedDataState)

                    Footer("Detalhes")
                }
            )
        }
    )

    if (showSheet) {
        FilterBottomSheet(
            sheetTitle = dashboard.title,
            initialFilters = activeFilters,
            sheetState = sheetState,
            onDismiss = { showSheet = false },
            onApply = { newFilters ->
                activeFilters = newFilters
                showSheet = false
            }
        )
    }
}

@Composable
fun MainChart(
    aggregationState: ResultWrapper<AggDataWrapperResponse>,
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
){
    CustomCard(
        paddingStart = paddingStart,
        paddingEnd = paddingEnd,
        paddingTop = paddingTop,
        paddingBottom = paddingBottom,
        layouts = listOf {
            CustomColumn(
                modifier = Modifier.fillMaxSize(),
                layouts = listOf {
                    when (aggregationState) {
                        is ResultWrapper.Success -> {
                            val chartDataWrapper = aggregationState.value.chartDataWrapper
                            key(chartDataWrapper) {
                                CustomBarChart(
                                    height = 300.dp,
                                    thickNes = 3.dp,
                                    dataWrapper = ChartDataWrapper(
                                        chartDataWrapper.label,
                                        chartDataWrapper.entries
                                    )
                                )
                            }
                        }

                        is ResultWrapper.Empty -> {
                            LoadingCard()
                        }

                        else -> {
                            EmptyStateChart()
                        }
                    }
                }
            )
        }
    )
}

@Composable
private fun Header(
    title: String,
    fontSize: TextUnit = 20.sp
) {
    Spacer(modifier = Modifier.padding(vertical = 5.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        CustomText(
            color = tb_primary_light,
            text = title,
            fontSize = fontSize
        )
    }
    Spacer(modifier = Modifier.padding(vertical = 10.dp))
}

@Composable
private fun Footer(text: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        CustomText(
            modifier = Modifier.clickable {
                Toast.makeText(
                    context,
                    "Essa funcionalidade está em desenvolvimento",
                    Toast.LENGTH_SHORT
                ).show()
            },
            color = tb_primary_light,
            text = text,
            fontSize = 14.sp
        )
    }
}


@Composable
private fun EmptyStateChart() {
    Box(
        modifier = Modifier
            .background(Color.Transparent)
    ) {
        CustomCard(
            layouts = listOf {
                Spacer(modifier = Modifier.padding(vertical = 10.dp))
                CustomText(
                    color = tb_primary_light,
                    text = "Não há dados a exibir",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.padding(vertical = 10.dp))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    initialFilters: DashboardFilters,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onApply: (DashboardFilters) -> Unit,
    sheetTitle: String
) {
    var draftFilters by remember { mutableStateOf(initialFilters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            CustomText(text = "Configurações do Gráfico de $sheetTitle", fontSize = 20.sp, color = tb_primary_light)

            Spacer(modifier = Modifier.padding(vertical = 8.dp))

            TimeIntervalSelector(
                selectedInterval = draftFilters.interval,
                onIntervalSelected = { draftFilters = draftFilters.copy(interval = it) }
            )

            // Aqui você pode adicionar o TelemetryToggleRow (W, V, A)
            // TelemetryToggleRow(draftFilters.telemetryKey) { ... }

            Spacer(modifier = Modifier.padding(vertical = 16.dp))

            RectButton(
                text = "Aplicar Filtros",
                onClick = { onApply(draftFilters) }
            )

            Spacer(modifier = Modifier.padding(vertical = 24.dp))
        }
    }
}

@Composable
private fun HeaderWithSettings(
    title: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomText(color = tb_primary_light, text = title, fontSize = 20.sp)

        CustomIconButton(
            iconResId = com.ifpe.edu.br.R.drawable.notification_icon,
            iconTint = tb_primary_light,
            backgroundColor = Color.Transparent,
            onClick = onSettingsClick,
            contentDescription = "Settings button"
        )
    }
    Spacer(modifier = Modifier.padding(vertical = 10.dp))
}