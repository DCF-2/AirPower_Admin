package com.ifpe.edu.br.view.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ifpe.edu.br.R
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomInputText
import com.ifpe.edu.br.common.components.CustomProgressDialog
import com.ifpe.edu.br.common.components.RectButton
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.common.ui.theme.White
import com.ifpe.edu.br.viewmodel.AdminViewModel

@Composable
fun RegisterScreen(
    navController: NavHostController,
    viewModel: AdminViewModel
) {
    val scrollState = rememberScrollState()
    val theme = MaterialTheme.colorScheme

    // Estados do Formulário
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Estados de UI
    var isSelectionHandlerFocused by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Estado do Popup de Sucesso
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // --- POPUP DE SUCESSO (Aguardando Aprovação) ---
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Obrigamos a clicar no botão para sair */ },
            title = { Text("Cadastro Concluído! 🎉") },
            text = { Text(successMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack() // Volta para o Login
                    }
                ) {
                    Text("Voltar ao Login")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Botão de Voltar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = theme.onBackground)
                }
            }

            val cardColor = theme.primary
            val appDimens = AirPowerTheme.dimens

            // LOGO
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(appDimens.cardCornerRadius))
                    .wrapContentSize()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "AirPower Logo",
                    modifier = Modifier.height(120.dp),
                    alignment = Alignment.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CARTÃO DE REGISTRO
            CustomCard(
                paddingStart = appDimens.paddingMedium,
                paddingEnd = appDimens.paddingMedium,
                paddingTop = appDimens.paddingMedium,
                paddingBottom = appDimens.paddingMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(appDimens.cardCornerRadius))
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isSelectionHandlerFocused = false }
                    .background(cardColor),
                layouts = listOf {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        val inputBackgroundColor = lerp(cardColor, theme.onPrimary, 0.05f)
                        val customSelectionColors = remember(isSelectionHandlerFocused) {
                            TextSelectionColors(
                                handleColor = if (isSelectionHandlerFocused) theme.secondary else Color.Transparent,
                                backgroundColor = theme.secondary.copy(alpha = 0.4f)
                            )
                        }

                        Text(
                            text = "Solicitar Acesso",
                            style = AirPowerTheme.typography.displayMedium,
                            color = theme.onPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        CustomInputText(
                            value = name,
                            onFocusChanged = { focused -> isSelectionHandlerFocused = focused },
                            onValueChange = { name = it },
                            label = "Nome Completo",
                            labelColor = theme.onPrimary,
                            placeholder = "Digite seu nome",
                            placeHolderColor = theme.onPrimary,
                            inputFieldColors = getInputColors(inputBackgroundColor, customSelectionColors, cardColor),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CustomInputText(
                            value = email,
                            onFocusChanged = { focused -> isSelectionHandlerFocused = focused },
                            onValueChange = { email = it },
                            label = "Email",
                            labelColor = theme.onPrimary,
                            placeholder = "admin@airpower.com",
                            placeHolderColor = theme.onPrimary,
                            inputFieldColors = getInputColors(inputBackgroundColor, customSelectionColors, cardColor),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CustomInputText(
                            value = password,
                            onValueChange = { password = it },
                            onFocusChanged = { focused -> isSelectionHandlerFocused = focused },
                            label = "Senha",
                            labelColor = theme.onPrimary,
                            placeholder = "******",
                            placeHolderColor = theme.onPrimary,
                            isPassword = true,
                            inputFieldColors = getInputColors(inputBackgroundColor, customSelectionColors, cardColor),
                            modifier = Modifier.fillMaxWidth(),
                            iconColor = White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        RectButton(
                            colors = ButtonDefaults.buttonColors(
                                contentColor = theme.onSecondary,
                                containerColor = theme.secondary,
                            ),
                            fontStyle = AirPowerTheme.typography.button,
                            text = "Cadastrar",
                            onClick = {
                                isSelectionHandlerFocused = false
                                if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                    errorMessage = "Preencha todos os campos."
                                    return@RectButton
                                }

                                isLoading = true
                                errorMessage = ""

                                // Chama a função que criamos no ViewModel!
                                viewModel.registerNewAdmin(name, email, password) { isSuccess, msg ->
                                    isLoading = false
                                    if (isSuccess) {
                                        successMessage = msg
                                        showSuccessDialog = true
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = theme.error,
                                modifier = Modifier.padding(top = 16.dp),
                                fontWeight = FontWeight.Bold,
                                style = AirPowerTheme.typography.bodySmall
                            )
                        }
                    }
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CustomProgressDialog(
                    indicatorColor = theme.secondary,
                    textColor = theme.surface,
                    fontStyle = AirPowerTheme.typography.displayMedium,
                    customBackground = { modifier -> Box(modifier = modifier.background(Color.Transparent)) }
                )
            }
        }
    }
}

@Composable
private fun getInputColors(
    inputBackgroundColor: Color,
    customSelectionColors: TextSelectionColors,
    cardColor: Color
): TextFieldColors {
    val theme = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        focusedTextColor = theme.onPrimary,
        unfocusedTextColor = theme.onPrimary,
        focusedLabelColor = theme.onPrimary,
        unfocusedLabelColor = theme.onPrimary,
        focusedContainerColor = inputBackgroundColor,
        unfocusedPlaceholderColor = cardColor,
        unfocusedContainerColor = inputBackgroundColor,
        focusedIndicatorColor = cardColor,
        unfocusedIndicatorColor = cardColor,
        selectionColors = customSelectionColors,
        cursorColor = theme.secondary
    )
}