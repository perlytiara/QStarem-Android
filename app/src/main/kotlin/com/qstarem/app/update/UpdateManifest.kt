package com.qstarem.app.update

import com.google.gson.annotations.SerializedName

data class UpdateManifest(
    val version: String,
    val versionCode: Int,
    val notes: String? = null,
    @SerializedName("pub_date")
    val pubDate: String? = null,
    val apk: ApkAsset,
)

data class ApkAsset(
    val url: String,
    val sha256: String,
)
