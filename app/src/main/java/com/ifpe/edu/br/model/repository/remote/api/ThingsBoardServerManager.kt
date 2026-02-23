package com.ifpe.edu.br.model.repository.remote.api

import android.annotation.SuppressLint
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.Constants.Constants.THINGS_BOARD_BASE_URL
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ThingsBoardServerManager {

    private var retrofit: Retrofit? = null
    private var apiService: ThingsBoardAPIService? = null

    // Variável para lembrar qual foi o último token usado
    private var lastUsedToken: String? = null

    fun getService(token: String?): ThingsBoardAPIService {
        // CORREÇÃO: Verifica se o serviço é nulo OU se o token mudou desde a última vez
        if (apiService == null || lastUsedToken != token) {
            createRetrofit(token)
            lastUsedToken = token
        }
        return apiService!!
    }

    private fun createRetrofit(token: String?) {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        // --- Configuração "Unsafe" SSL (Aceita certificado autoassinado/HTTP) ---
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory
        // -----------------------------------------------------------------------

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Adiciona o token APENAS se ele não for nulo
        if (!token.isNullOrEmpty()) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
        }

        val client = clientBuilder.build()

        val baseUrl = if (THINGS_BOARD_BASE_URL.endsWith("/"))
            THINGS_BOARD_BASE_URL
        else
            "${THINGS_BOARD_BASE_URL}/"

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit!!.create(ThingsBoardAPIService::class.java)
    }
}