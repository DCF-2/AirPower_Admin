package com.ifpe.edu.br.view

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Importante para injetar o ViewModel
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ifpe.edu.br.R
import com.ifpe.edu.br.common.ui.theme.AirPowerCostumerTheme
import com.ifpe.edu.br.model.util.AirPowerLog
import com.ifpe.edu.br.view.ui.screens.AuthScreen
import com.ifpe.edu.br.view.ui.screens.SplashScreen
import com.ifpe.edu.br.view.ui.theme.darkAppThemeSchema
import com.ifpe.edu.br.view.ui.theme.lightAppThemeSchema
import com.ifpe.edu.br.viewmodel.AdminViewModel // O nosso novo cérebro!

class AuthActivity : ComponentActivity() {
    val TAG = "AuthActivity"

    private val viewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)

        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "onCreate()")
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        enableEdgeToEdge()

        setContent {
            val adjustedContext = LocalContext.current.adjustedFontScale()
            val navController = rememberNavController()

            CompositionLocalProvider(LocalContext provides adjustedContext) {
                AirPowerCostumerTheme(
                    lightAppScheme = lightAppThemeSchema,
                    darkAppColorScheme = darkAppThemeSchema
                ) {
                    Surface {
                        InitializeNavigation(viewModel, navController, this)
                    }
                }
            }
        }
    }

    override fun finish() {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "finish()")
        super.finish()
    }

    private fun Context.adjustedFontScale(): Context {
        val configuration = Configuration(this.resources.configuration)
        configuration.fontScale = 1.0f
        configuration.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
        return createConfigurationContext(configuration)
    }
}

@Composable
private fun InitializeNavigation(
    mainViewModel: AdminViewModel,
    navController: NavHostController,
    componentActivity: ComponentActivity
) {
    NavHost(
        navController = navController,
        startDestination = "SPLASH"
    ) {
        composable("SPLASH") {
            SplashScreen(
                navController = navController,
                viewModel = mainViewModel,
                componentActivity = componentActivity
            )
        }

        composable("AUTH") {
            AuthScreen(
                navController = navController,
                viewModel = mainViewModel,
                componentActivity = componentActivity
            )
        }

        // --- ROTA DE REGISTRO! ---
        composable("REGISTER") {
            com.ifpe.edu.br.view.ui.screens.RegisterScreen(
                navController = navController,
                viewModel = mainViewModel
            )
        }
    }
}