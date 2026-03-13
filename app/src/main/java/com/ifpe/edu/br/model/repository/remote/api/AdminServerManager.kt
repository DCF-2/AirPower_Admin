package com.ifpe.edu.br.model.repository.remote.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AdminServerManager {

    // ATENÇÃO: Substitua pelo IP REAL da máquina do seu laboratório!
    // Exemplo: "http://192.168.1.50:9090"
    private val PROXY_BASE_URL = "http://10.5.0.68:8080"

    private var retrofit: Retrofit? = null
    private var apiService: AdminAPIService? = null
    private var lastUsedToken: String? = null

    fun getService(token: String?): AdminAPIService {
        if (apiService == null || lastUsedToken != token) {
            createRetrofit(token)
            lastUsedToken = token
        }
        return apiService!!
    }

    private fun createRetrofit(token: String?) {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // Adiciona o token (A Proxy vai apanhá-lo e reencaminhá-lo)
        if (!token.isNullOrEmpty()) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
        }

        retrofit = Retrofit.Builder()
            .baseUrl(PROXY_BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit!!.create(AdminAPIService::class.java)
    }
}