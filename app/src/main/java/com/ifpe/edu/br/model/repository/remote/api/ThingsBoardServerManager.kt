package com.ifpe.edu.br.model.repository.remote.api


import com.ifpe.edu.br.model.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ThingsBoardServerManager {

    private var retrofit: Retrofit? = null
    private var apiService: ThingsBoardAPIService? = null

    // Função para obter o serviço, criando-o se necessário
    // Recebe o token atual para injetar no cabeçalho
    fun getService(token: String?): ThingsBoardAPIService {
        if (apiService == null || retrofit == null) {
            createRetrofit(token)
        }
        return apiService!!
    }

    private fun createRetrofit(token: String?) {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Adiciona o Token no Header se disponível
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

        retrofit = Retrofit.Builder()
            .baseUrl(Constants.Constants.THINGS_BOARD_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit!!.create(ThingsBoardAPIService::class.java)
    }
}