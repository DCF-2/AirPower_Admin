package com.ifpe.edu.br.model.provisioning

/**
 * Representa uma ESP32 que foi encontrada na rede local via UDP Broadcast.
 */
data class DiscoveredEsp(
    val id: String,       // Ex: "12345" (MAC Address ou ID gerado)
    val ip: String,       // Ex: "192.168.4.1" (O IP da ESP32 na rede Hotspot)
    val isBlinking: Boolean = false // Controle de UI para saber se o LED está a piscar
)