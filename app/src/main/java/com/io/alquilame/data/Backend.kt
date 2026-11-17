package com.io.alquilame.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.io.alquilame.BuildConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** A failed API call with a message that can be shown to the user as is. */
class ApiException(message: String, val status: Int? = null) : Exception(message)

@OptIn(ExperimentalSerializationApi::class)
val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/** Persists the signed-in tenant and its API key in Keystore-encrypted preferences. */
class SessionStore(context: Context) {
    private val prefs: SharedPreferences = open(context.applicationContext)

    var tenant: Tenant? by mutableStateOf(loadTenant())
        private set

    val apiKey: String? get() = prefs.getString(KEY_API_KEY, null)
    val isSignedIn: Boolean get() = tenant != null && apiKey != null

    fun save(session: Session) {
        prefs.edit()
            .putString(KEY_API_KEY, session.apiKey)
            .putString(KEY_TENANT, AppJson.encodeToString(Tenant.serializer(), session.tenant))
            .apply()
        tenant = session.tenant
    }

    fun updateTenant(updated: Tenant) {
        prefs.edit().putString(KEY_TENANT, AppJson.encodeToString(Tenant.serializer(), updated)).apply()
        tenant = updated
    }

    fun clear() {
        prefs.edit().clear().apply()
        tenant = null
    }

    private fun loadTenant(): Tenant? {
        val raw = prefs.getString(KEY_TENANT, null) ?: return null
        if (prefs.getString(KEY_API_KEY, null) == null) return null
        return runCatching { AppJson.decodeFromString(Tenant.serializer(), raw) }.getOrNull()
    }

    private fun open(context: Context): SharedPreferences {
        fun create(): SharedPreferences {
            val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            return EncryptedSharedPreferences.create(
                context,
                FILE,
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
        return try {
            create()
        } catch (_: Exception) {
            // The Keystore key was lost (e.g. restored data): start from a clean file.
            context.deleteSharedPreferences(FILE)
            create()
        }
    }

    private companion object {
        const val FILE = "alquilame_session"
        const val KEY_API_KEY = "api_key"
        const val KEY_TENANT = "tenant"
    }
}

/** Networking entry point: the Retrofit API plus authenticated file downloads. */
class Backend(private val context: Context, val session: SessionStore) {

    private val auth = Interceptor { chain ->
        val request = chain.request().newBuilder().header("Accept", "application/json")
        session.apiKey?.let { request.header("Authorization", "Bearer $it") }
        chain.proceed(request.build())
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(auth)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    }
                )
            }
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: Api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(http)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(Api::class.java)

    /**
     * Runs an API call and unwraps its envelope, turning every failure into
     * an [ApiException]. A 401 means the API key was revoked: the session is
     * dropped, which sends the user back to the sign-in screen.
     */
    suspend fun <T> call(block: suspend Api.() -> Envelope<T>): T {
        try {
            val envelope = api.block()
            return envelope.data ?: throw ApiException(envelope.message ?: GENERIC_ERROR)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: HttpException) {
            val signedIn = session.isSignedIn
            if (e.code() == 401 && signedIn) session.clear()
            throw ApiException(errorMessage(e), e.code())
        } catch (_: IOException) {
            throw ApiException(context.getString(com.io.alquilame.R.string.error_network))
        } catch (_: Exception) {
            throw ApiException(GENERIC_ERROR)
        }
    }

    private fun errorMessage(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val parsed = body?.let { runCatching { AppJson.decodeFromString(JsonObject.serializer(), it) }.getOrNull() }
        val message = parsed?.get("message") ?: parsed?.get("error")
        return runCatching { message?.jsonPrimitive?.content }.getOrNull()
            ?: when (e.code()) {
                404 -> context.getString(com.io.alquilame.R.string.error_not_found)
                429 -> context.getString(com.io.alquilame.R.string.error_rate_limit)
                else -> "$GENERIC_ERROR (${e.code()})"
            }
    }

    /** Downloads an authenticated file (a PDF) into the shareable cache folder. */
    suspend fun download(path: String, fileName: String): File = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(BuildConfig.API_BASE_URL + path).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("$GENERIC_ERROR (${response.code})", response.code)
                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                val file = File(dir, fileName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
                response.body!!.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
                file
            }
        } catch (_: IOException) {
            throw ApiException(context.getString(com.io.alquilame.R.string.error_network))
        }
    }

    private val GENERIC_ERROR get() = context.getString(com.io.alquilame.R.string.error_generic)
}
