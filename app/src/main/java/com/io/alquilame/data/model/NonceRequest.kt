package com.io.alquilame.data.model

import com.google.gson.annotations.SerializedName

data class NonceRequest(
    @SerializedName("device_info")
    val deviceInfo: DeviceInfo
)
