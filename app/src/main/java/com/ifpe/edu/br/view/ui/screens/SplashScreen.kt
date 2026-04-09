package com.ifpe.edu.br.view.ui.screens

/*
* Refactored for: AirPower Admin
*/

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.ifpe.edu.br.R
import com.ifpe.edu.br.common.CommonConstants
import com.ifpe.edu.br.common.components.CustomColumn
import com.ifpe.edu.br.common.components.FailureDialog
import com.ifpe.edu.br.common.components.RoundedImageIcon
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.repository.persistence.manager.JWTManager
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.util.AirPowerUtil
import com.ifpe.edu.br.view.AdminActivity
import com.ifpe.edu.br.view.ui.components.CustomFullScreenGradientBackground
import com.ifpe.edu.br.viewmodel.AdminViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavHostController,
    viewModel: AdminViewModel,
    componentActivity: ComponentActivity
) {
    val theme = MaterialTheme.colorScheme
    CustomFullScreenGradientBackground(
        listColor = listOf(theme.background, theme.background.copy(alpha = 0.6f))
    )

    CustomColumn(
        modifier = Modifier.fillMaxSize(),
        alignmentStrategy = CommonConstants.Ui.ALIGNMENT_CENTER,
        layouts = listOf {
            Spacer(modifier = Modifier.padding(vertical = 100.dp))
            RoundedImageIcon(
                description = "custom icon",
                iconResId = R.drawable.logo_airpower_sem_fundo,
                modifier = Modifier.wrapContentSize()
            )
            Spacer(modifier = Modifier.padding(vertical = 100.dp))
        }
    )

    // Efeito para verificar o token assim que a tela abre
    LaunchedEffect(Unit) {
        delay(1500) // Dá tempo do usuário ver a logo bonita

        val isFirstRun = SharedPrefManager.getInstance(componentActivity).isFirstRun
        if (isFirstRun) {
            navigateAuthScreen(navController)
            return@LaunchedEffect
        }

        // Verifica o banco local
        val isExpired = JWTManager.isTokenExpiredForConnection(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)

        if (!isExpired) {
            navigateAdminActivity(navController, componentActivity)
        } else {
            navigateAuthScreen(navController)
        }
    }
}

private fun navigateAdminActivity(
    navController: NavController,
    componentActivity: ComponentActivity
) {
    navController.popBackStack()
    AirPowerUtil.launchActivity(
        navController.context,
        AdminActivity::class.java
    )
    componentActivity.finish()
}

private fun navigateAuthScreen(navController: NavController) {
    navController.navigate("AUTH") {
        popUpTo("SPLASH") { inclusive = true }
    }
}