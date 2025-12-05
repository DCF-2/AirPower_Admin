package com.ifpe.edu.br.view.ui.screens

/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ifpe.edu.br.common.CommonConstants
import com.ifpe.edu.br.common.components.CustomColumn
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.common.ui.theme.White
import com.ifpe.edu.br.model.repository.remote.dto.AlarmInfo
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggStrategy
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggregationRequest
import com.ifpe.edu.br.model.repository.remote.dto.agg.TelemetryKey
import com.ifpe.edu.br.model.repository.remote.dto.agg.TimeInterval
import com.ifpe.edu.br.model.util.AirPowerLog
import com.ifpe.edu.br.model.util.AirPowerUtil
import com.ifpe.edu.br.view.AuthActivity
import com.ifpe.edu.br.view.ui.theme.tb_secondary_light
import com.ifpe.edu.br.viewmodel.AirPowerViewModel

@Composable
fun DashBoardsScreen(
    navController: NavHostController,
    mainViewModel: AirPowerViewModel
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val userDashboards by
    mainViewModel.getDashboardsForCurrentUser().collectAsState(initial = emptyList())
    val allAlarms  =  mainViewModel.getAlarmInfoSet().collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.fetchDashboards()
    }

    CustomColumn(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxSize(),
        alignmentStrategy = CommonConstants.Ui.ALIGNMENT_TOP,
        layouts = listOf {
            if (!userDashboards.isEmpty()) {
                AirPowerLog.e("WILLIAM", "userDashboards => " + userDashboards)
                userDashboards.forEach { dashboard ->
                    val request = AggregationRequest(
                        devicesIds = dashboard.devicesIds,
                        aggStrategy = AggStrategy.AVG,
                        aggKey = TelemetryKey.POWER,
                        timeIntervalWrapper = getTimeWrapper(
                            System.currentTimeMillis(),
                            TimeInterval.DAY
                        )
                    )
                    val aggregatedDataState by mainViewModel.getAggregatedDataState(request)
                        .collectAsState()
                    LaunchedEffect(dashboard.id) {
                        mainViewModel.fetchAggregatedData(request)
                    }
                    AirPowerLog.d("FilterDebug", "alarms dashboardScreen: ${allAlarms.value}")
                    val filterAlarmsByDeviceIds =
                        filterAlarmsByDeviceIds(allAlarms.value, dashboard.devicesIds)
                    DevicesConsumptionSummaryCardBoard(
                        aggregationState = aggregatedDataState,
                        alarmInfo = filterAlarmsByDeviceIds,
                        cardLabel = dashboard.title
                    )
                    AirPowerLog.d(
                        "DashboardsScreen",
                        "Alarmes para o dashboard '${dashboard.title}': $filterAlarmsByDeviceIds"
                    )
                }
            }
            Spacer(modifier = Modifier.padding(vertical = 6.dp))
            RectButton(
                text = "Logout",
                onClick = {
                    mainViewModel.logout()
                    AirPowerUtil.launchActivity(
                        navController.context,
                        AuthActivity::class.java,
                    )
                    navController.popBackStack()
                    (context as? ComponentActivity)?.finish()
                },
                fontSize = 20.sp,
                colors = ButtonColors(
                    contentColor = White,
                    containerColor = tb_secondary_light,
                    disabledContentColor = Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.padding(vertical = 6.dp))
        }
    )
}

/**
 * Filtra uma lista de alarmes para retornar apenas aqueles associados a uma lista específica de IDs de dispositivos.
 *
 * @param alarms A lista completa de `AlarmInfo` a ser filtrada.
 * @param deviceIds Uma lista de `String` contendo os IDs dos dispositivos de interesse.
 * @return Uma nova lista de `AlarmInfo` contendo apenas os alarmes que correspondem aos `deviceIds` fornecidos.
 */
fun filterAlarmsByDeviceIds(alarms: List<AlarmInfo>, deviceIds: List<String>): List<AlarmInfo> {
    val deviceIdsSet = deviceIds.toSet()
    AirPowerLog.d("FilterDebug", "alarms: $alarms")

    alarms.forEach { alarm ->
        val alarmDeviceId = alarm.originator?.id?.toString()
        AirPowerLog.d("FilterDebug", "alarmDeviceId: " + alarmDeviceId)
    }

    return alarms.filter { alarm ->
        val alarmDeviceId = alarm.originator?.id?.toString()

        if (alarmDeviceId == null) {
            AirPowerLog.d("FilterDebug", "Alarme sem originatorId válido, descartando...")
            false
        } else {
            val matches = deviceIdsSet.contains(alarmDeviceId)
            AirPowerLog.d(
                "FilterDebug",
                "Verificando alarme do device: $alarmDeviceId. Corresponde? $matches"
            )
            matches
        }
    }
}