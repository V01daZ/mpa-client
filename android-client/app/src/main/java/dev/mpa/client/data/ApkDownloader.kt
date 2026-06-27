package dev.mpa.client.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()  // 0–100
    data class Ready(val apkFile: File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

object ApkDownloader {

    /**
     * Скачивает APK с прогрессом. Возвращает Flow<DownloadState>.
     */
    fun download(context: Context, url: String, fileName: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))
        try {
            val outFile = File(context.getExternalFilesDir("updates"), fileName)
            outFile.parentFile?.mkdirs()

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", "MPA-Android")
            }
            conn.connect()
            val total = conn.contentLengthLong

            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            emit(DownloadState.Downloading((downloaded * 100 / total).toInt()))
                        }
                    }
                }
            }
            conn.disconnect()
            emit(DownloadState.Ready(outFile))
        } catch (e: Exception) {
            emit(DownloadState.Failed(e.message ?: "Ошибка загрузки"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Открывает системный установщик APK.
     * Требует FileProvider и разрешение REQUEST_INSTALL_PACKAGES.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, authority, apkFile)
            } else {
                Uri.fromFile(apkFile)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("ApkDownloader", "Failed to start installation", e)
        }
    }
}
