package com.io.alquilame.data.model

import com.google.gson.annotations.SerializedName

data class DeviceInfo(
    @SerializedName("os")
    val os: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("model")
    val model: String,

    @SerializedName("app_version")
    val appVersion: String
)
