package com.io.alquilame.data.model

import com.google.gson.annotations.SerializedName

data class NonceData(
    @SerializedName("nonce")
    val nonce: String,

    @SerializedName("auth_url")
    val authUrl: String,

    @SerializedName("expires_at")
    val expiresAt: String
)
