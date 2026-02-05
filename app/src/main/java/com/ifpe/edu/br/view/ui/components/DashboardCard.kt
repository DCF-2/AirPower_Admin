// Trabalho de conclusão de curso - IFPE 2025
// Author: Willian Santos
// Project: AirPower Costumer

// Copyright (c) 2025 IFPE. All rights reserved.
package com.ifpe.edu.br.view.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomColumn
import com.ifpe.edu.br.common.components.CustomColumnChart
import com.ifpe.edu.br.common.components.CustomIconButton
import com.ifpe.edu.br.common.components.CustomLineChart
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.model.repository.model.ChartType
import com.ifpe.edu.br.model.repository.model.DashboardFilters
import com.ifpe.edu.br.model.repository.remote.dto.AlarmInfo
import com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggStrategy
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggregationRequest
import com.ifpe.edu.br.model.repository.remote.dto.agg.ChartDataWrapper
import com.ifpe.edu.br.model.repository.remote.dto.agg.TelemetryKey
import com.ifpe.edu.br.model.repository.remote.dto.agg.TimeInterval
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.view.ui.screens.getTimeWrapper
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
                timeIntervalWrapper = getTimeWrapper(
                    System.currentTimeMillis(),
                    activeFilters.interval
                )
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
                    ChartQueryDetails(activeFilters)
                    MainChart(
                        aggregationState = aggregatedDataState,
                        chartType = activeFilters.chartType
                    )
                    if (aggregatedDataState is ResultWrapper.Success) {
                        val wrapper =
                            (aggregatedDataState as ResultWrapper.Success).value.chartDataWrapper
                        StatisticsRow(
                            dataWrapper = ChartDataWrapper(wrapper.label, wrapper.entries),
                            telemetryKey = activeFilters.telemetryKey
                        )
                    }
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

private val telemetryDisplayNames = mapOf(
    TelemetryKey.POWER to "Potência",
    TelemetryKey.CURRENT to "Corrente",
    TelemetryKey.VOLTAGE to "Tensão",
)
private val intervalLabels = mapOf(
    TimeInterval.DAY to "Hoje",
    TimeInterval.WEEK to "Esta Semana",
    TimeInterval.MONTH to "Este Mês",
    TimeInterval.YEAR to "Este Ano"
)

@Composable
fun ChartQueryDetails(filters: DashboardFilters) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomText(
                    color = MaterialTheme.colorScheme.primary,
                    text = "Intervalo:",
                    fontSize = 12.sp
                )
                CustomText(
                    color = MaterialTheme.colorScheme.primary,
                    text = "${intervalLabels[filters.interval]}",
                    fontSize = 10.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomText(
                    color = MaterialTheme.colorScheme.primary,
                    text = "Dado:",
                    fontSize = 12.sp
                )
                CustomText(
                    color = MaterialTheme.colorScheme.primary,
                    text = "${telemetryDisplayNames[filters.telemetryKey]}",
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun MainChart(
    aggregationState: ResultWrapper<AggDataWrapperResponse>,
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
    chartType: ChartType = ChartType.BAR
) {
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
                                when (chartType) {
                                    ChartType.BAR -> {
                                        CustomColumnChart(
                                            height = 300.dp,
                                            dataWrapper = chartDataWrapper
                                        )
                                    }

                                    ChartType.LINE -> {
                                        CustomLineChart(
                                            height = 300.dp,
                                            dataWrapper = chartDataWrapper
                                        )
                                    }
                                }

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
private fun EmptyStateChart() {
    Box(
        modifier = Modifier
            .background(Color.Transparent)
    ) {
        CustomCard(
            layouts = listOf {
                Spacer(modifier = Modifier.padding(vertical = 10.dp))
                CustomText(
                    color = MaterialTheme.colorScheme.primary,
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
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            CustomText(
                text = sheetTitle,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.padding(vertical = 8.dp))

            TimeIntervalSelector(
                selectedInterval = draftFilters.interval,
                onIntervalSelected = { draftFilters = draftFilters.copy(interval = it) }
            )

            ChartTypeSelector(
                chartType = draftFilters.chartType,
                onTypeSelected = { draftFilters = draftFilters.copy(chartType = it) }
            )

            KeySelector(
                telemetryKey = draftFilters.telemetryKey,
                onTelemetryKeyChange = { draftFilters = draftFilters.copy(telemetryKey = it) }
            )

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
        CustomText(color = MaterialTheme.colorScheme.primary, text = title, fontSize = 20.sp)

        CustomIconButton(
            iconResId = com.ifpe.edu.br.R.drawable.filter,
            iconTint = MaterialTheme.colorScheme.primary,
            backgroundColor = Color.Transparent,
            onClick = onSettingsClick,
            contentDescription = "filtering button",
            modifier = Modifier.size(45.dp)
        )
    }
}

@Composable
fun StatisticsRow(
    dataWrapper: ChartDataWrapper,
    telemetryKey: TelemetryKey
) {
    val stats = remember(dataWrapper) {
        val values = dataWrapper.entries.map { it.value }
        if (values.isEmpty()) return@remember null
        val max = values.maxOrNull() ?: 0.0
        val min = values.minOrNull() ?: 0.0
        val avg = values.average()
        Triple(max, min, avg)
    }

    if (stats == null) return
    val (max, min, avg) = stats
    val unit = when (telemetryKey) {
        TelemetryKey.POWER -> "W"
        TelemetryKey.VOLTAGE -> "V"
        TelemetryKey.CURRENT -> "A"
    }

    CustomCard(
        paddingTop = 8.dp,
        paddingBottom = 8.dp,
        layouts = listOf {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    label = "Mínimo",
                    value = min.toDouble(),
                    unit = unit,
                    color = Color(0xFF4CAF50)
                ) // green
                StatItem(
                    label = "Média",
                    value = avg,
                    unit = unit,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Máximo",
                    value = max.toDouble(),
                    unit = unit,
                    color = Color(0xFFE91E63)
                ) // red
            }
        }
    )
}

@Composable
fun StatItem(
    label: String,
    value: Double,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomText(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontWeight = FontWeight.Normal
        )
        CustomText(
            text = "%.1f %s".format(value, unit),
            fontSize = 18.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}