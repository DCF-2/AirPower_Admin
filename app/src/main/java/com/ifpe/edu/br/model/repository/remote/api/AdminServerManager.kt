package com.ifpe.edu.br.model.repository.remote.api

import android.content.Context
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AdminServerManager(private val context: Context) {

    private var retrofit: Retrofit? = null
    private var apiService: AdminAPIService? = null
    private var lastUsedToken: String? = null
    private var lastUsedUrl: String? = null // Guarda a última URL usada

    fun getService(token: String?): AdminAPIService {
        // Puxa a URL configurada pelo utilizador na tela de Configurações
        val currentUrl = SharedPrefManager.getInstance(context).proxyBaseUrl

        // Só recria o Retrofit se o token mudar OU se o utilizador trocar o servidor nas configurações!
        if (apiService == null || lastUsedToken != token || lastUsedUrl != currentUrl) {
            createRetrofit(token, currentUrl)
            lastUsedToken = token
            lastUsedUrl = currentUrl
        }
        return apiService!!
    }

    private fun createRetrofit(token: String?, baseUrl: String) {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // 1. ADICIONA O TOKEN PRIMEIRO
        if (!token.isNullOrEmpty()) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("X-Authorization", "Bearer $token") // OBRIGATÓRIO PARA O THINGSBOARD
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
        }

        // 2. ADICIONA O LOG DEPOIS
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        clientBuilder.addInterceptor(logging)

        // Prevenção de Crash: O Retrofit exige que a URL base termine sempre com "/"
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        retrofit = Retrofit.Builder()
            .baseUrl(safeBaseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit!!.create(AdminAPIService::class.java)
    }
}