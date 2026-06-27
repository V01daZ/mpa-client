package dev.mpa.client.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class RuleSetState {
    data object Idle        : RuleSetState()
    data object Checking    : RuleSetState()
    data object Downloading : RuleSetState()
    data object Ready       : RuleSetState()
    data class  Error(val message: String) : RuleSetState()
}

object RuleSetDownloader {

    private const val TAG = "RuleSetDownloader"

    // Правильные URL — ветка rule-set репозиториев sing-geoip / sing-geosite
    private const val GEOIP_RU_URL =
        "https://raw.githubusercontent.com/SagerNet/sing-geoip/rule-set/geoip-ru.srs"
    private const val GEOSITE_RU_URL =
        "https://raw.githubusercontent.com/SagerNet/sing-geosite/rule-set/geosite-ru.srs"

    // Обновляем раз в сутки
    private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L

    fun ensureReady(context: Context): Flow<RuleSetState> = flow {
        emit(RuleSetState.Checking)

        val geoipFile   = File(context.filesDir, SingBoxConfig.GEOIP_RU_FILENAME)
        val geositeFile = File(context.filesDir, SingBoxConfig.GEOSITE_RU_FILENAME)

        if (!needsUpdate(geoipFile) && !needsUpdate(geositeFile)) {
            Log.d(TAG, "Rule sets are up to date")
            emit(RuleSetState.Ready)
            return@flow
        }

        emit(RuleSetState.Downloading)

        try {
            downloadFile(GEOIP_RU_URL, geoipFile)
            Log.d(TAG, "Downloaded geoip-ru: ${geoipFile.length()} bytes")

            downloadFile(GEOSITE_RU_URL, geositeFile)
            Log.d(TAG, "Downloaded geosite-ru: ${geositeFile.length()} bytes")

            emit(RuleSetState.Ready)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            if (geoipFile.exists() || geositeFile.exists()) {
                Log.d(TAG, "Using cached rule sets")
                emit(RuleSetState.Ready)
            } else {
                emit(RuleSetState.Error(e.message ?: "Ошибка загрузки rule-set"))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun isReady(context: Context): Boolean {
        val geoipFile   = File(context.filesDir, SingBoxConfig.GEOIP_RU_FILENAME)
        val geositeFile = File(context.filesDir, SingBoxConfig.GEOSITE_RU_FILENAME)
        return geoipFile.exists() || geositeFile.exists()
    }

    private fun needsUpdate(file: File): Boolean {
        if (!file.exists()) return true
        return System.currentTimeMillis() - file.lastModified() > UPDATE_INTERVAL_MS
    }

    private fun downloadFile(url: String, dest: File) {
        val tmp = File(dest.parent, "${dest.name}.tmp")
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod          = "GET"
                connectTimeout         = 15_000
                readTimeout            = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MPA-Android")
            }
            val code = conn.responseCode
            if (code != 200) throw Exception("HTTP $code для $url")

            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
            conn.disconnect()
            tmp.renameTo(dest)
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }
}
