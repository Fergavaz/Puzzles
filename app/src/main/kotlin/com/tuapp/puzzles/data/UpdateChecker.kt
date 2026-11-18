package com.tuapp.puzzles.data

import android.content.Context
import com.tuapp.puzzles.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UpdateInfo(
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val isUpdateAvailable: Boolean,
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class CheckFailed(val message: String, val manualDownloadUrl: String) : UpdateCheckResult
}

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val GITHUB_RELEASES_PAGE = "https://github.com/Fergavaz/Puzzles/releases"
        private const val GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/Fergavaz/Puzzles/releases/latest"
    }

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_LATEST_RELEASE)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "PuzzlesApp/${BuildConfig.VERSION_NAME}")
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.use { it.readText() }
                conn.disconnect()

                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "").removePrefix("v")
                val releaseNotes = json.optString("body", "Novedades y mejoras.")
                var downloadUrl = json.optString("html_url", GITHUB_RELEASES_PAGE)

                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", downloadUrl)
                            if (name == "puzzles.fergavaz.apk") break
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val isNewer = isVersionNewer(tagName, currentVersion)

                if (isNewer) {
                    UpdateCheckResult.Available(
                        UpdateInfo(
                            latestVersionName = tagName,
                            downloadUrl = downloadUrl,
                            releaseNotes = releaseNotes,
                            isUpdateAvailable = true,
                        ),
                    )
                } else {
                    UpdateCheckResult.UpToDate(currentVersion)
                }
            } else {
                conn.disconnect()
                UpdateCheckResult.CheckFailed(
                    message = "No se pudo consultar el repositorio directamente.",
                    manualDownloadUrl = GITHUB_RELEASES_PAGE,
                )
            }
        } catch (e: Exception) {
            UpdateCheckResult.CheckFailed(
                message = e.message ?: "Error de conexión al comprobar actualizaciones.",
                manualDownloadUrl = GITHUB_RELEASES_PAGE,
            )
        }
    }

    /** Compara números de versión como "1.2.0" vs "1.1.1". Retorna true si remote > current. */
    fun isVersionNewer(remote: String, current: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(rParts.size, cParts.size)
        for (i in 0 until len) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
