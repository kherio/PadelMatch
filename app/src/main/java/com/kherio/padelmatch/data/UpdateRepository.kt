package com.kherio.padelmatch.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String?,
    val body: String?,
    @SerializedName("html_url") val htmlUrl: String,
    val assets: List<ReleaseAsset> = emptyList()
)

data class ReleaseAsset(
    val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("content_type") val contentType: String?
)

sealed interface UpdateCheckResult {
    data class Available(
        val release: AppRelease,
        val installedVersion: String
    ) : UpdateCheckResult

    data class UpToDate(
        val installedVersion: String,
        val latestVersion: String
    ) : UpdateCheckResult

    data class Error(val message: String) : UpdateCheckResult
}

class UpdateRepository {

    companion object {
        private const val RELEASES_URL =
            "https://api.github.com/repos/kherio/PadelMatch/releases/latest"
    }

    private val gson = Gson()

    suspend fun checkForUpdate(installedVersion: String): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            try {
                val release = fetchLatestRelease()
                val latestVersion = normalizeVersion(release.tagName)

                if (compareVersions(latestVersion, normalizeVersion(installedVersion)) > 0) {
                    UpdateCheckResult.Available(release, installedVersion)
                } else {
                    UpdateCheckResult.UpToDate(installedVersion, latestVersion)
                }
            } catch (e: Exception) {
                UpdateCheckResult.Error(
                    e.message ?: "No se pudo comprobar si hay actualizaciones."
                )
            }
        }

    private fun fetchLatestRelease(): AppRelease {
        val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            setRequestProperty("User-Agent", "PadelMatch-Android")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("GitHub respondió con HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            gson.fromJson(body, AppRelease::class.java)
                ?: throw IllegalStateException("Respuesta de GitHub vacía.")
        } finally {
            connection.disconnect()
        }
    }

    fun downloadUrlFor(release: AppRelease): String {
        // El APK oficial se publica como "PadelMatch-<versión>.apk"; es el único que se busca.
        val named = release.assets.firstOrNull {
            it.name.startsWith("PadelMatch", ignoreCase = true) &&
                it.name.endsWith(".apk", ignoreCase = true)
        }
        if (named != null) return named.browserDownloadUrl

        val preferred = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) &&
                !it.name.contains("debug", ignoreCase = true)
        }

        val fallback = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        }

        return (preferred ?: fallback)?.browserDownloadUrl ?: release.htmlUrl
    }

    private fun normalizeVersion(version: String): String =
        version.trim().removePrefix("v").removePrefix("V")

    private fun compareVersions(a: String, b: String): Int {
        val aParts = a.split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        val bParts = b.split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        val size = maxOf(aParts.size, bParts.size)

        for (i in 0 until size) {
            val av = aParts.getOrElse(i) { 0 }
            val bv = bParts.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }

        return 0
    }
}
