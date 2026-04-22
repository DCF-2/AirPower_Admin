package com.ifpe.edu.br.view.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ifpe.edu.br.R
import com.ifpe.edu.br.model.repository.AdminRepository
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.repository.remote.dto.auth.LoginRequest
import com.ifpe.edu.br.model.util.AirPowerUtil
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.view.ui.components.ServerConfigBottomSheet
import com.ifpe.edu.br.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navController: NavHostController,
    viewModel: AdminViewModel,
    componentActivity: ComponentActivity
) {
    val scrollState = rememberScrollState()
    val theme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var showServerConfig by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (SharedPrefManager.getInstance(componentActivity).isFirstRun) {
            showServerConfig = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background) // Fundo Clean
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Nova Identidade Visual - Logo
            Image(
                painter = painterResource(id = R.drawable.logo_airpower_sem_fundo),
                contentDescription = "AirPower Logo",
                modifier = Modifier.height(120.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Card Limpo e Moderno
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = theme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Acesso ao Sistema",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Correção do MD3: OutlinedTextFieldDefaults.colors
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.primary,
                            focusedLabelColor = theme.primary,
                            cursorColor = theme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.primary,
                            focusedLabelColor = theme.primary,
                            cursorColor = theme.primary
                        )
                    )

                    AnimatedVisibility(
                        visible = errorMessage.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage,
                            color = theme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botão com Loading Integrado (Atendendo ao Acceptance Criteria)
                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            focusManager.clearFocus()
                            isLoading = true
                            errorMessage = ""

                            scope.launch {
                                val result = viewModel.repository.login(LoginRequest(email, password))

                                isLoading = false
                                if (result is ResultWrapper.Success<*>) {
                                    navController.popBackStack()
                                    AirPowerUtil.launchActivity(componentActivity, com.ifpe.edu.br.view.AdminActivity::class.java)
                                    componentActivity.finish()
                                } else if (result is ResultWrapper.ApiError) {
                                    errorMessage = when (result.code) {
                                        401 -> "Credenciais Incorretas"
                                        403 -> "Acesso Negado ou Usuário Pendente"
                                        404 -> "Usuário não encontrado"
                                        else -> "Erro do servidor (${result.code})"
                                    }
                                } else {
                                    errorMessage = "Servidor offline ou inacessível."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primary,
                            contentColor = theme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = theme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Entrar",
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Solicitar Acesso",
                        color = theme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { navController.navigate("REGISTER") }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { showServerConfig = true }) {
                Text(
                    text = "⚙️ Configurar Servidor",
                    color = theme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
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