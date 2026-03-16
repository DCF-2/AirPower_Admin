package com.ifpe.edu.br.view.ui.screens


import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.ifpe.edu.br.model.util.AirPowerUtil
import com.ifpe.edu.br.view.AuthActivity
import com.ifpe.edu.br.view.ui.components.EmptyStateCard
import com.ifpe.edu.br.view.ui.components.DashboardCard
import com.ifpe.edu.br.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashBoardsScreen(
    navController: NavHostController,
    mainViewModel: AdminViewModel // Usando o novo maestro
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // TODO: No futuro, vamos adicionar uma rota no AdminRepository para buscar a lista de Dashboards disponíveis para o Tenant.
    // Por enquanto, criamos uma lista vazia simulada para não dar erro de compilação.
    val userDashboards = emptyList<com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo>()

    val isRefreshing by mainViewModel.isLoading.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            // TODO: Chamar o refresh dos Dashboards do AdminViewModel
        },
        modifier = Modifier.fillMaxSize()
    ) {
        CustomColumn(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            alignmentStrategy = CommonConstants.Ui.ALIGNMENT_TOP,
            layouts = listOf {
                Spacer(modifier = Modifier.padding(vertical = 16.dp))

                if (userDashboards.isEmpty()) {
                    EmptyStateCard()
                } else {
                    userDashboards.forEach { dashboard ->
                        DashboardCard(
                            dashboard = dashboard,
                            onClick = {
                                // TODO: Ação de abrir a WebView com o Dashboard
                            }
                        )
                        Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 24.dp))

                RectButton(
                    text = "Sair da Conta",
                    onClick = {
                        mainViewModel.logout()
                        AirPowerUtil.launchActivity(
                            navController.context,
                            AuthActivity::class.java,
                        )
                        (context as? ComponentActivity)?.finish()
                    },
                    fontSize = 20.sp,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = White,
                        containerColor = MaterialTheme.colorScheme.secondary,
                    )
                )
                Spacer(modifier = Modifier.padding(vertical = 16.dp))
            }
        )
    }
}