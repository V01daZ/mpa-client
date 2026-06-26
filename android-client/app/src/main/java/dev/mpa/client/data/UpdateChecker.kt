package dev.mpa.client.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.mpa.client.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val versionName: String,   // напр. "1.2.0"
    val versionCode: Int,      // напр. 3
    val apkUrl: String,        // прямая ссылка на .apk asset
    val releaseNotes: String,  // тело релиза
    val htmlUrl: String,       // ссылка на страницу релиза
)

private data class GithubRelease(
    @SerializedName("name")         val name: String,
    @SerializedName("body")         val body: String?,
    @SerializedName("html_url")     val htmlUrl: String,
    @SerializedName("tag_name")     val tagName: String,
    @SerializedName("assets")       val assets: List<GithubAsset>,
)

private data class GithubAsset(
    @SerializedName("name")                  val name: String,
    @SerializedName("browser_download_url")  val downloadUrl: String,
    @SerializedName("content_type")          val contentType: String,
    @SerializedName("size")                  val size: Long,
)

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/V01daZ/mpa-client/releases/tags/beta_android"

    /**
     * Проверяет наличие обновления.
     * Возвращает ReleaseInfo если versionCode в релизе > текущего, иначе null.
     *
     * Версия кодируется в имени APK-файла: mpa-android-vX.Y.Z-mrt-N.apk
     * Например: mpa-android-v1.1.0-mrt-2.apk
     * Если нет такого паттерна — сравниваем только имя релиза.
     */
    suspend fun checkForUpdate(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val json = httpGet(API_URL)
            val release = Gson().fromJson(json, GithubRelease::class.java)

            // Ищем APK среди ассетов
            val apkAsset = release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true)
            } ?: return@withContext null

            // Пытаемся извлечь versionCode из имени файла (mpa-android-...-mrt-N.apk)
            val remoteVersionCode = extractVersionCode(apkAsset.name)
            val remoteVersionName = extractVersionName(apkAsset.name) ?: release.name

            // Сравниваем с текущим
            val currentCode = BuildConfig.VERSION_CODE
            if (remoteVersionCode != null && remoteVersionCode <= currentCode) {
                return@withContext null
            }
            // Если versionCode не удалось извлечь — всё равно показываем баннер
            // (пользователь сам решит нужно ли обновляться)

            ReleaseInfo(
                versionName  = remoteVersionName,
                versionCode  = remoteVersionCode ?: 0,
                apkUrl       = apkAsset.downloadUrl,
                releaseNotes = release.body?.take(300) ?: "",
                htmlUrl      = release.htmlUrl,
            )
        } catch (e: Exception) {
            Log.w("UpdateChecker", "Check failed: ${e.message}")
            null
        }
    }

    // mpa-android-v1.1.0-mrt-2.apk → 2
    private fun extractVersionCode(filename: String): Int? {
        return Regex("mrt-?(\\d+)", RegexOption.IGNORE_CASE)
            .find(filename)?.groupValues?.get(1)?.toIntOrNull()
    }

    // mpa-android-v1.1.0-vcode-2.apk → "1.1.0"
    private fun extractVersionName(filename: String): String? {
        return Regex("v(\\d+\\.\\d+\\.\\d+)", RegexOption.IGNORE_CASE)
            .find(filename)?.groupValues?.get(1)
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MPA-Android/${BuildConfig.VERSION_NAME}")
        }
        return try {
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}
