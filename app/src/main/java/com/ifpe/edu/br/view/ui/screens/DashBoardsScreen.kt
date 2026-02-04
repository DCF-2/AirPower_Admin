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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import com.ifpe.edu.br.model.util.AirPowerUtil
import com.ifpe.edu.br.view.AuthActivity
import com.ifpe.edu.br.view.ui.components.EmptyStateCard
import com.ifpe.edu.br.view.ui.theme.tb_secondary_light
import com.ifpe.edu.br.viewmodel.AirPowerViewModel
import com.ifpe.edu.br.view.ui.components.DashboardCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashBoardsScreen(
    navController: NavHostController,
    mainViewModel: AirPowerViewModel
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val userDashboards by
    mainViewModel.getDashboardsForCurrentUser().collectAsState(initial = emptyList())
    val allAlarms = mainViewModel.getAlarmInfoSet().collectAsState()
    val isRefreshing by mainViewModel.isRefreshing.collectAsState()

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
                .fillMaxSize().padding(horizontal = 10.dp),
            alignmentStrategy = CommonConstants.Ui.ALIGNMENT_TOP,
            layouts = listOf {
                if (userDashboards.isEmpty()) {
                    EmptyStateCard()
                } else {
                    userDashboards.forEach { dashboard ->
                        DashboardCard(
                            dashboard = dashboard,
                            mainViewModel = mainViewModel,
                            allAlarms = allAlarms.value
                        )
                        Spacer(modifier = Modifier.padding(vertical = 8.dp))
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
                    colors = ButtonDefaults.buttonColors(
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
}


/**
 * Filtra uma lista de objetos [AlarmInfo] para retornar apenas aqueles associados a um conjunto específico de IDs de dispositivos.
 *
 * Este método é "null-safe" e ignora com segurança quaisquer alarmes na lista de entrada
 * que tenham um `originator` ou `originator.id` nulos, prevenindo `NullPointerException`.
 * Ele utiliza um `Set` para os IDs de dispositivos fornecidos para garantir uma verificação de contenção eficiente (complexidade O(1) em média).
 *
 * @param alarms A lista completa de [AlarmInfo] a ser filtrada. Pode conter alarmes com dados nulos.
 * @param deviceIds Uma lista de [String] contendo os IDs dos dispositivos a serem usados como critério de filtro.
 * @return Uma nova [List] de [AlarmInfo] contendo apenas os alarmes cujo ID do originador
 *         corresponde a um dos IDs na lista `deviceIds`. Retorna uma lista vazia se não houver correspondências.
 */
fun filterAlarmsByDeviceIds(alarms: List<AlarmInfo>, deviceIds: List<String>): List<AlarmInfo> {
    val deviceIdsSet = deviceIds.toSet()
    return alarms.filter { alarm ->
        alarm.originator?.id?.toString()?.let { deviceId ->
            deviceIdsSet.contains(deviceId)
        } ?: false
    }
}