package com.pubgconnect.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Default points to live Oracle Cloud server
    // Can be dynamically customized from app Settings / Login
    private var currentBaseUrl = "http://145.241.123.224:5000/"
    private var apiServiceInstance: ApiService? = null

    val baseUrl: String
        get() = currentBaseUrl

    fun updateBaseUrl(newUrl: String) {
        var cleanUrl = newUrl.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        if (cleanUrl != currentBaseUrl) {
            currentBaseUrl = cleanUrl
            apiServiceInstance = null // Force recreation with new URL
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    fun getService(): ApiService {
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
                .also { apiServiceInstance = it }
        }
    }
}
