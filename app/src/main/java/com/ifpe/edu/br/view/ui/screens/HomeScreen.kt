/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/

package com.ifpe.edu.br.view.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ifpe.edu.br.common.CommonConstants
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomColumn
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.model.repository.model.ChartType
import com.ifpe.edu.br.model.repository.model.HomeScreenAlarmSummaryCard
import com.ifpe.edu.br.model.repository.remote.dto.AlarmInfo
import com.ifpe.edu.br.model.repository.remote.dto.DevicesStatusSummary
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggStrategy
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggregationRequest
import com.ifpe.edu.br.model.repository.remote.dto.agg.ChartDataWrapper
import com.ifpe.edu.br.model.repository.remote.dto.agg.TelemetryKey
import com.ifpe.edu.br.model.repository.remote.dto.agg.TimeInterval
import com.ifpe.edu.br.model.repository.remote.dto.agg.TimeIntervalWrapper
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.view.ui.components.AlarmCardInfo
import com.ifpe.edu.br.view.ui.components.CardInfo
import com.ifpe.edu.br.view.ui.components.EmptyStateCard
import com.ifpe.edu.br.view.ui.components.GradientDivider
import com.ifpe.edu.br.view.ui.components.MainChart
import com.ifpe.edu.br.view.ui.components.SolidDivider
import com.ifpe.edu.br.view.ui.components.StatItem

import com.ifpe.edu.br.viewmodel.AirPowerViewModel
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale


private val telemetryKey = TelemetryKey.POWER

@Composable
fun HomeScreen(
    navController: NavHostController,
    mainViewModel: AirPowerViewModel
) {
    val scrollState = rememberScrollState()
    val alarmInfo = mainViewModel.getAlarmInfoSet().collectAsState()
    val allDeviceIds =
        mainViewModel.getDevicesSummary().collectAsState().value.map { it.id.toString() }
    val isRefreshing by mainViewModel.isRefreshing.collectAsState()

    /*
     * this should me dynamic on future releases
     */
    val homeScreenRequest = AggregationRequest(
        devicesIds = allDeviceIds,
        aggStrategy = AggStrategy.AVG,
        aggKey = telemetryKey,
        timeIntervalWrapper = getTimeWrapper(
            System.currentTimeMillis(),
            TimeInterval.MONTH
        )
    )

    val aggregationState = mainViewModel.getAggregatedDataState(homeScreenRequest).collectAsState()

    LaunchedEffect(allDeviceIds) {
        if (allDeviceIds.isNotEmpty()) {
            mainViewModel.fetchAggregatedData(homeScreenRequest)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            mainViewModel.forceRefresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        CustomColumn(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize(),
            alignmentStrategy = CommonConstants.Ui.ALIGNMENT_TOP,
            layouts = listOf {
                Container(layouts = listOf {
                    MiscellaneousSection(aggregationState.value)
                    SummarySection(aggregationState.value)
                    ChartSection(aggregationState.value)
                    AlarmSection(alarmInfo.value)
                })
            }
        )
    }
}

@Composable
fun AlarmSection(alarmsInfo: List<AlarmInfo>) {
    if (alarmsInfo.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SimpleColumn(
                layouts = listOf {
                    SectionInfo("Alarmes dos dispositivos")
                    GradientDivider()
                    SubsectionTitle("Alarmes por Severidade")
                    val alarmDashboardData = processAlarms(alarmsInfo)
                    val severity = alarmDashboardData.bySeverity
                    TagsRow(
                        layouts = listOf {
                            // SEVERITY
                            if (severity.isNotEmpty()) {
                                val severities = getExistingSeverities(alarmsInfo)
                                severities.forEach { severity ->
                                    ItemCard(
                                        backGroudColor = AirPowerTheme.color.secondaryContainer,
                                        layouts = listOf {
                                            CustomText(
                                                text = severity.toTitleCase(),
                                                color = AirPowerTheme.color.onSecondaryContainer,
                                                fontStyle = AirPowerTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.padding(vertical = 5.dp))
                                            CustomText(
                                                text = severities.size.toString(),
                                                color = AirPowerTheme.color.onSecondaryContainer,
                                                fontStyle = AirPowerTheme.typography.bodyLarge
                                            )
                                        }
                                    )
                                }
                            }
                            SolidDivider()
                            SubsectionTitle("Alarmes por Tipo")
                            // TYPES
                            val existingTypes = getExistingTypes(alarmsInfo)
                            val groupedByType = alarmDashboardData.byType
                            if (existingTypes.isNotEmpty()) {
                                existingTypes.forEach { item ->
                                    ItemCard(
                                        backGroudColor = AirPowerTheme.color.secondaryContainer,
                                        layouts = listOf {
                                            val type = groupedByType[item]
                                            CustomText(
                                                text = item.toTitleCase(),
                                                color = AirPowerTheme.color.onSecondaryContainer,
                                                fontStyle = AirPowerTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.padding(vertical = 5.dp))
                                            CustomText(
                                                text = type?.size.toString(),
                                                color = AirPowerTheme.color.onSecondaryContainer,
                                                fontStyle = AirPowerTheme.typography.bodyLarge
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    )
                    SmallVerticalPadding()
                }
            )
        }
    }
}

@Composable
fun SubsectionTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            style = AirPowerTheme.typography.bodySmall,
            color = AirPowerTheme.color.onSecondaryContainer
        )
    }
}


fun String.toTitleCase(): String {
    if (this.isBlank()) return this

    return this.lowercase()
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
}

fun String.formatDecimalBr(): String {
    val number = this.toDoubleOrNull() ?: 0.0
    return number.formatDecimalBr()
}

fun Double.formatDecimalBr(): String {
    val ptBr = Locale("pt", "BR")
    val formatter = NumberFormat.getNumberInstance(ptBr).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(this)
}

private fun getExistingSeverities(alarms: List<AlarmInfo>): List<String> {
    return alarms
        .map { it.severity }
        .distinct()
        .sorted()
}

private fun getExistingTypes(alarms: List<AlarmInfo>): List<String> {
    return alarms
        .map { it.type }
        .distinct()
        .sorted()
}

data class AlarmDashboardData(
    // Mapa onde a Chave é a Severidade (ex: "CRITICAL") e o Valor é a lista de alarmes
    val bySeverity: Map<String, List<AlarmInfo>> = emptyMap(),

    // Mapa onde a Chave é o Tipo (ex: "High Temperature") e o Valor é a lista de alarmes
    val byType: Map<String, List<AlarmInfo>> = emptyMap(),
)

fun processAlarms(alarmsInfo: List<AlarmInfo>): AlarmDashboardData {
    return AlarmDashboardData(
        bySeverity = alarmsInfo.groupBy { it.severity },
        byType = alarmsInfo.groupBy { it.type },
    )
}

@Composable
private fun SectionInfo(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            style = AirPowerTheme.typography.bodyLarge,
            color = AirPowerTheme.color.onPrimaryContainer
        )
    }
}

@Composable
fun TagsRow(
    layouts: List<@Composable () -> Unit>
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        layouts.forEach { item ->
            item()
        }
    }
}

@Composable
fun ChartSection(value: ResultWrapper<AggDataWrapperResponse>) {
    when (value) {
        is ResultWrapper.Success -> {
            SectionInfo(value.value.label)
            GradientDivider()
            Spacer(modifier = Modifier.padding(top = AirPowerTheme.dimens.paddingSmall))
            MainChart(
                chartHeight = 250.dp,
                aggregationState = value,
                chartType = ChartType.LINE
            )
            SmallVerticalPadding()
            StatisticsRow(
                dataWrapper = value.value.chartDataWrapper,
                telemetryKey = telemetryKey
            )
            SmallVerticalPadding()
        }

        else -> {}
    }
}

@Composable
fun SummarySection(
    aggregationWrapper: ResultWrapper<AggDataWrapperResponse>
) {
    when (aggregationWrapper) {
        is ResultWrapper.Success<AggDataWrapperResponse> -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val aggDataWrapper = aggregationWrapper.value
                val statusSummariesList = aggDataWrapper.statusSummaries
                SimpleColumn(
                    layouts = listOf {
                        SectionInfo("Status dos dispositivos")
                        GradientDivider()
                        TagsRow(
                            layouts = statusSummariesList.map { item ->
                                {
                                    ItemCard(
                                        backGroudColor = AirPowerTheme.color.secondaryContainer,
                                        layouts = listOf {
                                            CustomText(
                                                text = item.label,
                                                color = AirPowerTheme.color.onSecondaryContainer,
                                                fontStyle = AirPowerTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.padding(vertical = 5.dp))
                                            CustomText(
                                                text = item.occurrence.toString(),
                                                color = AirPowerTheme.color.onSecondaryContainer,
                                                fontStyle = AirPowerTheme.typography.bodyLarge
                                            )
                                        }
                                    )
                                }
                            }
                        )
                        SmallVerticalPadding()
                    }
                )
            }
        }

        else -> {}
    }
}

@Composable
private fun Container(
    layouts: List<@Composable () -> Unit>
) {
    val colorSchema = AirPowerTheme.color
    val dimens = AirPowerTheme.dimens
    CustomCard(
        paddingStart = dimens.paddingMedium,
        paddingEnd = dimens.paddingMedium,
        paddingTop = dimens.paddingMedium,
        paddingBottom = dimens.paddingMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(dimens.cardCornerRadius))
            .background(colorSchema.primaryContainer)
            .fillMaxWidth(),
        layouts = layouts
    )
}

@Composable
private fun ItemCard(
    layouts: List<@Composable () -> Unit>,
    backGroudColor: Color = Color.Transparent
) {
    val dimens = AirPowerTheme.dimens
    CustomCard(
        paddingStart = dimens.paddingSmall,
        paddingEnd = dimens.paddingSmall,
        paddingTop = dimens.paddingSmall,
        paddingBottom = dimens.paddingSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(dimens.cardCornerRadius))
            .background(backGroudColor)
            .wrapContentSize(),
        layouts = layouts
    )
}

@Composable
private fun SimpleColumn(
    layouts: List<@Composable () -> Unit>
) {
    CustomColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        alignmentStrategy = CommonConstants.Ui.ALIGNMENT_TOP,
        layouts = layouts
    )
}

@Composable
fun SimpleRow(
    layouts: List<@Composable () -> Unit>,
    isCentered: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.Start
    ) {
        layouts.forEach { layout ->
            layout()
        }
    }
}

@Composable
fun MiscellaneousSection(aggDataWrapper: ResultWrapper<AggDataWrapperResponse>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (aggDataWrapper) {
            is ResultWrapper.Success<AggDataWrapperResponse> -> {
                val aggDataWrapper = aggDataWrapper.value
                val deviceAmount = aggDataWrapper.size
                val consumptionLabel = aggDataWrapper.label
                val consumptionValue = aggDataWrapper.aggregation
                val measureUnit = aggDataWrapper.chartDataWrapper.label
                SectionInfo("Informações")
                GradientDivider()
                TagsRow(
                    layouts = listOf {
                        ItemCard(
                            backGroudColor = AirPowerTheme.color.secondaryContainer,
                            layouts = listOf {
                                CustomText(
                                    text = "Seus Dispositivos".toTitleCase(),
                                    fontStyle = AirPowerTheme.typography.bodyLarge,
                                    color = AirPowerTheme.color.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.padding(vertical = 5.dp))
                                CustomText(
                                    text = "$deviceAmount",
                                    fontStyle = AirPowerTheme.typography.bodyLarge,
                                    color = AirPowerTheme.color.onSecondaryContainer
                                )
                            }
                        )
                        Spacer(modifier = Modifier.padding(vertical = 5.dp))
                        ItemCard(
                            backGroudColor = AirPowerTheme.color.secondaryContainer,
                            layouts = listOf {

                                CustomText(
                                    text = consumptionLabel.toTitleCase(),
                                    fontStyle = AirPowerTheme.typography.bodyLarge,
                                    color = AirPowerTheme.color.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.padding(vertical = 5.dp))
                                CustomText(
                                    text = consumptionValue.value.formatDecimalBr()+ " $measureUnit".toTitleCase(),
                                    fontStyle = AirPowerTheme.typography.bodyLarge,
                                    color = AirPowerTheme.color.onSecondaryContainer
                                )
                            }
                        )
                    }
                )
                SmallVerticalPadding()
            }

            else -> {}
        }
    }
}

@Composable
private fun SmallVerticalPadding() {
    Spacer(modifier = Modifier.padding(bottom = AirPowerTheme.dimens.paddingSmall))
}

@Composable
private fun BigVerticalPadding() {
    Spacer(modifier = Modifier.padding(bottom = AirPowerTheme.dimens.paddingLarge))
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

    ItemCard(
        backGroudColor = AirPowerTheme.color.secondaryContainer,
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
                    color = AirPowerTheme.color.onSecondaryContainer
                )
                StatItem(
                    label = "Média",
                    value = avg,
                    unit = unit,
                    color = AirPowerTheme.color.onSecondaryContainer
                )
                StatItem(
                    label = "Máximo",
                    value = max.toDouble(),
                    unit = unit,
                    color = AirPowerTheme.color.onSecondaryContainer
                )
            }
        }
    )
}

@Composable
private fun HomeScreenAlarmGrid(
    alarmInfoSet: List<AlarmInfo>,
    onClick: (String) -> Unit
) {
    val severityAggregationMap: MutableMap<String, Int> = mutableMapOf()
    alarmInfoSet.forEach { item ->
        val severity = item.severity
        val occurrence = severityAggregationMap[severity] ?: 0
        severityAggregationMap[severity] = occurrence + 1
    }

    val cards: List<HomeScreenAlarmSummaryCard> =
        severityAggregationMap.map { (severity, count) ->
            HomeScreenAlarmSummaryCard(severity, count)
        }

    val gridCount = if (cards.size > 3) 2 else 3
    var cardHeight = if (gridCount == 2) 160.dp else 140.dp
    if (cards.size > 6) {
        cardHeight = 260.dp
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridCount),
        modifier = Modifier
            .height(cardHeight)
            .fillMaxWidth(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(cards, key = { it.severity }) { deviceItem ->
            AlarmCardInfo(
                alarmCardInfo = deviceItem,
                onClick = onClick,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
private fun DevicesStatusGrid(
    statusSummaries: List<DevicesStatusSummary>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    if (statusSummaries != null && statusSummaries.isNotEmpty()) {
        val gridCount = if (statusSummaries.size > 3) 2 else 3
        var cardHeight = if (gridCount == 2) 160.dp else 140.dp
        if (statusSummaries.size > 6) {
            cardHeight = 260.dp
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCount),
            modifier = Modifier
                .height(cardHeight)
                .fillMaxWidth(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(statusSummaries) { deviceItem ->
                CardInfo(
                    label = deviceItem.label,
                    value = deviceItem.occurrence.toString(),
                    onClick = onClick,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

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
                color = MaterialTheme.colorScheme.primary,
                text = "Detalhes",
                fontSize = 12.sp
            )
        }
    } else {
        EmptyStateCard()
    }
}

/**
 * Calcula a data de início de um intervalo de tempo com base em um timestamp de referência,
 * retornando um [TimeIntervalWrapper] que representa o início do período e o tipo de agregação desejado.
 *
 * Essa função é utilizada para padronizar o alinhamento temporal dos dados de telemetria
 * agregados que serão consultados via API do ThingsBoard, como médias por hora, por dia etc.
 *
 * Exemplo de uso: ao selecionar o intervalo "WEEK", o timestamp de início será ajustado
 * para a segunda-feira mais próxima (ou o próprio dia, se já for segunda).
 *
 * @param refEpochMillis O timestamp de referência em milissegundos (Epoch).
 * Normalmente, representa o instante atual no cliente (ex: `System.currentTimeMillis()`).
 *
 * @param timeInterval Enum que define o intervalo desejado: [TimeInterval.DAY],
 * [TimeInterval.WEEK], [TimeInterval.MONTH] ou [TimeInterval.YEAR].
 *
 * @return [TimeIntervalWrapper] contendo o timestamp de início ajustado (`startTs`)
 * e o tipo de intervalo (`timeInterval`). Esse wrapper é usado para construir as consultas
 * com agregações temporais à API do ThingsBoard.
 *
 * Exemplo de retorno para `refEpochMillis` em 13/07/2025 às 10h30:
 * - DAY  → startTs = 13/07/2025 00:00
 * - WEEK → startTs = 07/07/2025 00:00 (segunda-feira da semana)
 * - MONTH → startTs = 01/07/2025 00:00
 * - YEAR → startTs = 01/01/2025 00:00
 */
fun getTimeWrapper(
    refEpochMillis: Long,
    timeInterval: TimeInterval
): TimeIntervalWrapper {

    val rawStart = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(refEpochMillis),
        ZoneId.systemDefault()
    ).withNano(0)

    val startTs = when (timeInterval) {
        TimeInterval.DAY -> {
            val start = rawStart.truncatedTo(ChronoUnit.DAYS)
            start.toInstant().toEpochMilli()

        }

        TimeInterval.WEEK -> {
            val start =
                rawStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS)
            start.toInstant().toEpochMilli()
        }

        TimeInterval.MONTH -> {
            val start = rawStart.with(TemporalAdjusters.firstDayOfMonth())
                .truncatedTo(ChronoUnit.DAYS)
            start.toInstant().toEpochMilli()
        }

        TimeInterval.YEAR -> {
            val start = rawStart.with(TemporalAdjusters.firstDayOfYear())
                .truncatedTo(ChronoUnit.DAYS)
            start.toInstant().toEpochMilli()
        }
    }

    return TimeIntervalWrapper(
        startTs,
        timeInterval
    )
}