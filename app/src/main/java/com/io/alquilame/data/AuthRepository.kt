package com.io.alquilame.data

import android.content.Context
import android.os.Build
import com.io.alquilame.BuildConfig
import com.io.alquilame.data.api.ApiClient
import com.io.alquilame.data.model.DeviceInfo
import com.io.alquilame.data.model.NonceRequest
import com.io.alquilame.data.Result

class AuthRepository(private val context: Context) {

    private val apiClient = ApiClient(context)
    private val credentialsStorage = SecureCredentialsStorage(context)

    /**
     * Get authentication URL using the new nonce-based method
     * Returns Result.Success with auth URL or Result.Error with error message
     */
    suspend fun getAuthUrl(): Result<String> {
        return try {
            // Get API key from secure storage
            val apiKey = credentialsStorage.getApiKey()
            if (apiKey == null) {
                return Result.Error(Exception("No API key found. Please log in again."))
            }

            // Create device info
            val deviceInfo = DeviceInfo(
                os = "Android",
                version = Build.VERSION.RELEASE,
                model = Build.MODEL,
                appVersion = BuildConfig.VERSION_NAME
            )

            // Create request
            val request = NonceRequest(deviceInfo = deviceInfo)

            // Call API
            val response = apiClient.authApiService.generateNonce(
                authHeader = "Bearer $apiKey",
                request = request
            )

            // Handle response
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.data != null) {
                    Result.Success(body.data.authUrl)
                } else {
                    Result.Error(Exception(body.message))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Invalid or missing API key"
                    403 -> "Access denied"
                    404 -> "Authentication endpoint not found"
                    500 -> "Server error. Please try again later"
                    else -> "Failed to get authentication URL: ${response.code()}"
                }
                Result.Error(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Network error: ${e.message}", e))
        }
    }

    /**
     * Legacy method - Generate secure URL using old dynamic secret method
     * DEPRECATED - Use getAuthUrl() instead
     */
    @Deprecated(
        message = "Use getAuthUrl() for nonce-based authentication instead",
        replaceWith = ReplaceWith("getAuthUrl()"),
        level = DeprecationLevel.WARNING
    )
    fun generateSecureUrlLegacy(tenantSlug: String, secretSlug: String): String {
        val randomKey = System.currentTimeMillis().toString()
        val combinedSecret = "$tenantSlug|$secretSlug|$randomKey|${BuildConfig.SECRET_STRING_LOGIN}"
        val hashedSecret = generateSHA256Hash(combinedSecret)
        val secretTenantSlugDynamic = hashedSecret.substring(0, 16)
        return "https://$tenantSlug.alquilame.io/s/tenant/$tenantSlug/$secretTenantSlugDynamic/$randomKey"
    }

    private fun generateSHA256Hash(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
