package com.io.alquilame.data.api

import com.io.alquilame.data.model.NonceRequest
import com.io.alquilame.data.model.NonceResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/mobile/auth/nonce")
    suspend fun generateNonce(
        @Header("Authorization") authHeader: String,
        @Body request: NonceRequest
    ): Response<NonceResponse>
}
