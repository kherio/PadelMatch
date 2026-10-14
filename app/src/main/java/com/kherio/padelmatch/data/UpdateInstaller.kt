package com.kherio.padelmatch.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Lanza el instalador de Android con un APK ya descargado.
 *
 * Android exige que el usuario permita a la app "instalar apps desconocidas"
 * (una sola vez) y confirme la instalación en su propio diálogo. El APK debe
 * estar firmado con la misma clave que la app instalada; si no, Android
 * rechaza la actualización.
 */
object UpdateInstaller {

    private const val APK_MIME = "application/vnd.android.package-archive"

    /** true si la app ya tiene permiso para pedir la instalación de APKs. */
    fun canInstall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    /** Abre el ajuste donde se concede el permiso a esta app. Devuelve false si no se pudo abrir. */
    fun openInstallPermissionSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /** Abre el instalador de Android con el APK. Devuelve false si no se pudo abrir. */
    fun install(context: Context, apk: File): Boolean {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        } catch (e: IllegalArgumentException) {
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
