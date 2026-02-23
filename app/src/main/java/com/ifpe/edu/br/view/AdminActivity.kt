package com.ifpe.edu.br.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.ifpe.edu.br.R
import com.ifpe.edu.br.common.ui.theme.AirPowerCostumerTheme
import com.ifpe.edu.br.view.ui.screens.AdminMainScreen
import com.ifpe.edu.br.view.ui.theme.darkAppThemeSchema
import com.ifpe.edu.br.view.ui.theme.lightAppThemeSchema
import com.ifpe.edu.br.viewmodel.AdminViewModel

class AdminActivity : ComponentActivity() {

    private lateinit var viewModel: AdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        setContent {
            // Usando o mesmo tema do cliente
            AirPowerCostumerTheme(
                lightAppScheme = lightAppThemeSchema,
                darkAppColorScheme = darkAppThemeSchema
            ) {
                AdminMainScreen(
                    viewModel = viewModel,
                    onLogout = {
                        // Volta para a tela de Login
                        val intent = Intent(this, AuthActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}