package com.qstarem.app.update

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateChecker(
    private val gson: Gson = Gson(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    fun fetchLatestManifest(): UpdateManifest? {
        val request = Request.Builder()
            .url(MANIFEST_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "QStarem-Android")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return gson.fromJson(body, UpdateManifest::class.java)
        }
    }

    companion object {
        const val MANIFEST_URL =
            "https://github.com/perlytiara/QStarem-Android/releases/latest/download/latest.json"
    }
}
