package com.io.alquilame.data.api

import android.content.Context
import com.io.alquilame.data.SecureCredentialsStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    companion object {
        private const val BASE_DOMAIN = "alquilame.io"
        private const val MAIN_BASE_URL = "https://alquilame.io/"
        private const val TIMEOUT_SECONDS = 30L

        /**
         * Build the base URL for a specific tenant
         * @param tenantSlug The tenant subdomain (e.g., "negocio" for negocio.alquilame.io)
         * @return The full base URL (e.g., "https://negocio.alquilame.io/")
         */
        fun buildBaseUrl(tenantSlug: String): String {
            return "https://$tenantSlug.$BASE_DOMAIN/"
        }
    }

    private val credentialsStorage = SecureCredentialsStorage(context)

    private val httpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Get Retrofit instance for a specific tenant
     * Uses the stored username (tenant slug) to determine the base URL
     */
    private fun getRetrofitForTenant(): Retrofit {
        val tenantSlug = credentialsStorage.getUsername() ?: "www"
        val baseUrl = buildBaseUrl(tenantSlug)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Get Retrofit instance for main domain (alquilame.io)
     * Used for /api/mobile endpoints
     */
    private fun getRetrofitForMainDomain(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MAIN_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val reservationsApiService: ReservationsApiService by lazy {
        getRetrofitForTenant().create(ReservationsApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        getRetrofitForMainDomain().create(AuthApiService::class.java)
    }

    fun getAuthorizationHeader(): String? {
        val apiKey = credentialsStorage.getApiKey()
        return if (apiKey != null) "Bearer $apiKey" else null
    }

    fun hasCredentials(): Boolean {
        return credentialsStorage.hasCredentials()
    }
}