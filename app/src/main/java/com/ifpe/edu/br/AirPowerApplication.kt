package com.ifpe.edu.br

import android.app.Application
import android.content.Context
import com.ifpe.edu.br.model.repository.AdminRepository
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.util.AirPowerLog

class AirPowerApplication : Application() {
    private val tag = AirPowerApplication::class.simpleName

    companion object {
        private lateinit var instance: AirPowerApplication
        fun getContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(tag, "onCreate() - Starting AirPower Admin")
        super.onCreate()
        instance = this

        // Inicializa o nosso novo Repositório Global com o Contexto
        AdminRepository.getInstance(applicationContext)

        // Inicializa o Shared Preferences
        SharedPrefManager.getInstance(applicationContext)
    }
}