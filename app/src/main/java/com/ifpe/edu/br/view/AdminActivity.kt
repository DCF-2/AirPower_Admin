package com.ifpe.edu.br.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Importação moderna do ViewModel
import androidx.compose.material3.Surface
import com.ifpe.edu.br.R
import com.ifpe.edu.br.common.ui.theme.AirPowerCostumerTheme
import com.ifpe.edu.br.view.ui.screens.AdminMainScreen
import com.ifpe.edu.br.view.ui.theme.darkAppThemeSchema
import com.ifpe.edu.br.view.ui.theme.lightAppThemeSchema
import com.ifpe.edu.br.viewmodel.AdminViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminActivity : ComponentActivity() {

    // Instanciação moderna e segura do cérebro da tela
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transição de tela
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        enableEdgeToEdge()

        setContent {
            // Usando o tema que você definiu
            AirPowerCostumerTheme(
                lightAppScheme = lightAppThemeSchema,
                darkAppColorScheme = darkAppThemeSchema
            ) {
                Surface {
                    AdminMainScreen(
                        viewModel = viewModel,
                        onLogout = {
                            // Volta para a tela de Login e limpa o histórico
                            val intent = Intent(this, AuthActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}