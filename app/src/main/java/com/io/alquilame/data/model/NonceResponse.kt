package com.io.alquilame.data.model

import com.google.gson.annotations.SerializedName

data class NonceResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: NonceData?,

    @SerializedName("message")
    val message: String
)
