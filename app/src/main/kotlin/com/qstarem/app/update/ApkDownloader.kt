package com.qstarem.app.update

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ApkDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .build(),
) {
    fun destinationFor(versionCode: Int): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        return File(directory, "qstarem-$versionCode.apk")
    }

    fun download(
        manifest: UpdateManifest,
        onProgress: (Float) -> Unit,
    ): File {
        val destination = destinationFor(manifest.versionCode)
        if (destination.isFile && destination.length() > 0 &&
            verifySha256(destination, manifest.apk.sha256)
        ) {
            onProgress(1f)
            return destination
        }

        val request = Request.Builder()
            .url(manifest.apk.url)
            .header("User-Agent", "QStarem-Android")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Download failed with status ${response.code}")
            }

            val body = response.body ?: error("Empty APK response body")
            val totalBytes = body.contentLength().coerceAtLeast(0L)
            var downloaded = 0L

            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }

        if (!verifySha256(destination, manifest.apk.sha256)) {
            destination.delete()
            error("Downloaded APK failed checksum verification")
        }

        onProgress(1f)
        return destination
    }

    private fun verifySha256(file: File, expected: String): Boolean {
        if (expected.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { byte ->
            "%02x".format(byte)
        }
        return actual.equals(expected, ignoreCase = true)
    }
}
