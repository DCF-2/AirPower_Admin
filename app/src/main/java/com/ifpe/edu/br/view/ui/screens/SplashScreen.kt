package com.ifpe.edu.br.view.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.ifpe.edu.br.R
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.repository.persistence.manager.JWTManager
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.util.AirPowerUtil
import com.ifpe.edu.br.view.AdminActivity
import com.ifpe.edu.br.viewmodel.AdminViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavHostController,
    viewModel: AdminViewModel,
    componentActivity: ComponentActivity
) {
    val theme = MaterialTheme.colorScheme

    // Animações para a logo
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Anima a escala e a opacidade simultaneamente
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }

        delay(1500) // Tempo de respiro para admirar a logo

        val isFirstRun = SharedPrefManager.getInstance(componentActivity).isFirstRun
        if (isFirstRun) {
            navigateAuthScreen(navController)
            return@LaunchedEffect
        }

        val isExpired = JWTManager.isTokenExpiredForConnection(Constants.ServerConnectionIds.CONNECTION_ID_THINGSBOARD)
        if (!isExpired) {
            navigateAdminActivity(navController, componentActivity)
        } else {
            navigateAuthScreen(navController)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_airpower_sem_fundo),
            contentDescription = "AirPower Logo",
            modifier = Modifier
                .size(200.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        )
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