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
            // Get credentials from secure storage
            val tenantSlug = credentialsStorage.getUsername()
            val apiKey = credentialsStorage.getApiKey()

            android.util.Log.d("AuthRepository", "Starting authentication...")
            android.util.Log.d("AuthRepository", "Tenant slug: $tenantSlug")
            android.util.Log.d("AuthRepository", "API key present: ${apiKey != null}")

            if (apiKey == null) {
                return Result.Error(Exception("No API key found. Please log in again."))
            }

            if (tenantSlug == null) {
                return Result.Error(Exception("No tenant slug found. Please log in again."))
            }

            // Build the full URL that will be called
            val fullUrl = "https://alquilame.io/api/mobile/auth/nonce"
            android.util.Log.d("AuthRepository", "Calling endpoint: $fullUrl")
            android.util.Log.d("AuthRepository", "Authorization: Bearer $apiKey")

            // Create device info
            val deviceInfo = DeviceInfo(
                os = "Android",
                version = Build.VERSION.RELEASE,
                model = Build.MODEL,
                appVersion = BuildConfig.VERSION_NAME
            )

            // Create request
            val request = NonceRequest(deviceInfo = deviceInfo)

            // Call API with Bearer token in Authorization header
            val response = apiClient.authApiService.generateNonce(
                authHeader = "Bearer $apiKey",
                request = request
            )

            android.util.Log.d("AuthRepository", "Response code: ${response.code()}")
            android.util.Log.d("AuthRepository", "Response success: ${response.isSuccessful}")

            // Handle response
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                android.util.Log.d("AuthRepository", "Response body success: ${body.success}")
                android.util.Log.d("AuthRepository", "Response message: ${body.message}")

                if (body.success && body.data != null) {
                    android.util.Log.d("AuthRepository", "Auth URL: ${body.data.authUrl}")
                    Result.Success(body.data.authUrl)
                } else {
                    Result.Error(Exception(body.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AuthRepository", "Error response body: $errorBody")

                val errorMessage = when (response.code()) {
                    401 -> "Invalid or missing API key"
                    403 -> "Access denied"
                    404 -> "Authentication endpoint not found at: $fullUrl"
                    500 -> "Server error. Please try again later"
                    else -> "Failed to get authentication URL: ${response.code()}"
                }
                Result.Error(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Exception during auth", e)
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
