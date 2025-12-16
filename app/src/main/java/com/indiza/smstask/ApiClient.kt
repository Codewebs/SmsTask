package com.indiza.smstask

import com.indiza.smstask.tools.SmsApi   // ✅ IMPORT CORRECT
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val AUTH_TOKEN = "super-secret-token-123"
    private var retrofit: Retrofit? = null

    fun init(baseUrl: String) {
        println("🔍 INIT ApiClient avec: $baseUrl")
        println("🔑 Token utilisé: $AUTH_TOKEN")
        val auth = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $AUTH_TOKEN")
                .build()
            chain.proceed(req)
        }

        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logger)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: SmsApi
        get() = retrofit!!.create(SmsApi::class.java)
}

