package com.ifpe.edu.br.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ifpe.edu.br.R
import com.ifpe.edu.br.common.ui.theme.AirPowerCostumerTheme
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.viewmodel.AirPowerViewModelProvider

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Animação de entrada (mesma da MainActivity)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        enableEdgeToEdge()

        // Inicializa ViewModel para garantir logout e sessao futura
        val viewModel = AirPowerViewModelProvider.getInstance()

        setContent {
            AirPowerCostumerTheme{
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Área do Administrador\n(Em Construção)",
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}