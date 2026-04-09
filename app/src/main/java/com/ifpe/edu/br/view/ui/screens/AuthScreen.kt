package com.ifpe.edu.br.view.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.ifpe.edu.br.model.repository.AdminRepository
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.repository.remote.dto.auth.LoginRequest
import com.ifpe.edu.br.model.util.AirPowerUtil
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.view.ui.components.ServerConfigBottomSheet
import com.ifpe.edu.br.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    navController: NavHostController,
    viewModel: AdminViewModel, // Agora usando o AdminViewModel
    componentActivity: ComponentActivity
) {
    val scrollState = rememberScrollState()
    val theme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    // Controles de UI Locais
    var showServerConfig by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (SharedPrefManager.getInstance(componentActivity).isFirstRun) {
            showServerConfig = true
        }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val cardColor = theme.primary
            var isSelectionHandlerFocused by remember { mutableStateOf(false) }
            val appDimens = AirPowerTheme.dimens

            // ESPAÇO PARA A LOGO MODERNIZADA
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(appDimens.cardCornerRadius))
                    .wrapContentSize()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_airpower_sem_fundo),
                    contentDescription = "AirPower Logo",
                    modifier = Modifier.height(180.dp),
                    alignment = Alignment.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CARTÃO DE LOGIN
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
                        var email by rememberSaveable { mutableStateOf("") }
                        var password by rememberSaveable { mutableStateOf("") }

                        val inputBackgroundColor = lerp(cardColor, theme.onPrimary, 0.05f)
                        val customSelectionColors = remember(isSelectionHandlerFocused) {
                            TextSelectionColors(
                                handleColor = if (isSelectionHandlerFocused) theme.secondary else Color.Transparent,
                                backgroundColor = theme.secondary.copy(alpha = 0.4f)
                            )
                        }

                        Text(
                            text = "Acesso Restrito",
                            style = AirPowerTheme.typography.displayMedium,
                            color = theme.onPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

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

                        // BOTÃO DE LOGIN COM CHAMADA DIRETA PARA O REPOSITÓRIO
                        RectButton(
                            colors = ButtonDefaults.buttonColors(
                                contentColor = theme.onSecondary,
                                containerColor = theme.secondary,
                            ),
                            fontStyle = AirPowerTheme.typography.button,
                            text = "Entrar no Sistema",
                            onClick = {
                                isSelectionHandlerFocused = false
                                isLoading = true
                                errorMessage = ""

                                scope.launch {
                                    val result = AdminRepository.getInstance(componentActivity)
                                        .login(LoginRequest(email, password))

                                    isLoading = false
                                    if (result is ResultWrapper.Success) {
                                        // Sucesso! Vai para o AdminActivity
                                        navController.popBackStack()
                                        AirPowerUtil.launchActivity(componentActivity, com.ifpe.edu.br.view.AdminActivity::class.java)
                                        componentActivity.finish()
                                    } else if (result is ResultWrapper.ApiError) {
                                        errorMessage = when (result.code) {
                                            401 -> "Senha Incorreta"
                                            403 -> "Acesso Negado ou Usuário Pendente"
                                            404 -> "Usuário não encontrado"
                                            else -> "Erro do servidor (${result.code})"
                                        }
                                    } else {
                                        errorMessage = "Sem conexão com o servidor."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // MENSAGEM DE ERRO (Aparece se falhar o login)
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = theme.error,
                                modifier = Modifier.padding(top = 16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // TEXTO PARA REGISTRO (Link)
                        Text(
                            text = "Não tem acesso? Solicite aqui.",
                            color = theme.onPrimary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable {
                                    navController.navigate("REGISTER")
                                },
                            style = AirPowerTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÃO DE CONFIG DE SERVIDOR (Embaixo do Cartão)
            RectButton(
                colors = ButtonDefaults.buttonColors(
                    contentColor = theme.onSurface.copy(alpha = 0.6f),
                    containerColor = Color.Transparent,
                ),
                fontStyle = AirPowerTheme.typography.button,
                text = "⚙️ Configurar Servidor API",
                onClick = { showServerConfig = true },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // SOBREPOSIÇÕES (Loading e BottomSheet)
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
                    customBackground = { modifier ->
                        Box(modifier = modifier.background(Color.Transparent))
                    }
                )
            }
        }

        if (showServerConfig) {
            ServerConfigBottomSheet(
                onDismiss = { showServerConfig = false },
                onSave = { showServerConfig = false }
            )
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