package com.ifpe.edu.br.model.repository.remote.dto.auth


data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    // --- INJEÇÃO AUTOMÁTICA DE IDENTIFICADORES ---
    val appKey: String = "AIRPOWER_ADMIN_KEY", // Chave de segurança do App
    val appName: String = "Airpower_Admin",    // Nome legível do App
    val role: String = "TENANT_ADMIN"          // Nível de acesso solicitado
)
