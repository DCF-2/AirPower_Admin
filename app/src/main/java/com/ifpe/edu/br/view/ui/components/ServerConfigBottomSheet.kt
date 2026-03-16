/*
* Refactored for: AirPower Admin (BFF Integration)
*/
package com.ifpe.edu.br.view.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.common.components.CustomInputText
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigBottomSheet(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    // O FIM DO HARDCODE: Apenas uma URL mestre (Proxy)
    val sharedPrefs = SharedPrefManager.getInstance(context)
    var proxyUrl by remember { mutableStateOf(sharedPrefs.proxyBaseUrl ?: "") }

    val theme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {

            // CORRIGIDO: Substituímos o SimpleRow e o listOf{} pelo Row nativo!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CustomText(
                    text = "Configuração do Servidor",
                    fontStyle = AirPowerTheme.typography.displayMedium,
                    color = theme.onSurface
                )
            }

            Spacer(modifier = Modifier.padding(vertical = 20.dp))

            val inputBackgroundColor = lerp(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.onPrimary,
                0.05f)

            val customSelectionColors = TextSelectionColors(
                handleColor = theme.secondary,
                backgroundColor = theme.secondary
            )

            // Um único input limpo!
            CustomInputText(
                value = proxyUrl,
                onValueChange = { proxyUrl = it },
                label = "URL do Servidor Proxy",
                labelFontStyle = AirPowerTheme.typography.bodySmall,
                placeholderFontStyle = AirPowerTheme.typography.button,
                placeholder = "Ex: http://10.5.0.66:8080",
                inputFieldColors = getCustomInputTextColors(
                    inputBackgroundColor,
                    customSelectionColors
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            RectButton(
                colors = ButtonDefaults.buttonColors(
                    contentColor = theme.onPrimary,
                    containerColor = theme.primary,
                    disabledContentColor = theme.onPrimary,
                    disabledContainerColor = theme.primary
                ),
                fontStyle = AirPowerTheme.typography.button,
                text = "Salvar e Conectar",
                fontSize = 15.sp,
                onClick = {
                    if(proxyUrl.isNotBlank()) {
                        sharedPrefs.proxyBaseUrl = proxyUrl
                        Toast.makeText(context, "Servidor configurado!", Toast.LENGTH_SHORT).show()
                        onSave()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "A URL não pode estar vazia", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun getCustomInputTextColors(
    inputBackgroundColor: Color,
    customSelectionColors: TextSelectionColors
): TextFieldColors {
    val theme = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        focusedTextColor = theme.onSurface,
        unfocusedTextColor = theme.onSurface,
        focusedLabelColor = theme.onSurface,
        unfocusedLabelColor = theme.onSurface,
        focusedContainerColor = inputBackgroundColor,
        unfocusedContainerColor = inputBackgroundColor,
        focusedIndicatorColor = theme.onSurface,
        unfocusedIndicatorColor = theme.surface,
        cursorColor = theme.secondary,
        selectionColors = customSelectionColors,
        focusedPlaceholderColor = theme.primary,
        unfocusedPlaceholderColor = theme.onSurface,
    )
}