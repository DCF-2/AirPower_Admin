package com.ifpe.edu.br

import android.app.Application
import android.content.Context
import com.ifpe.edu.br.model.util.AirPowerLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AirPowerApplication : Application() {
    private val tag = AirPowerApplication::class.simpleName

    companion object {
        private lateinit var instance: AirPowerApplication

        // Mantemos o getContext() apenas se você tiver classes muito antigas
        // ou utilitários (utils) que dependam estritamente deste contexto estático.
        fun getContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (AirPowerLog.ISLOGABLE) {
            AirPowerLog.d(tag, "onCreate() - Starting AirPower Admin with Dagger Hilt")
        }
    }
}