package com.kherio.padelmatch.data

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga un archivo a disco (bloqueante; llamar desde un hilo de E/S).
 * Sin dependencias de Android para poder probarlo aparte.
 *
 * Escribe primero en un fichero ".part" y solo lo renombra al nombre final
 * cuando la descarga está completa, así nunca queda un APK a medias con el
 * nombre bueno. GitHub responde con una redirección a su CDN; se sigue sola.
 */
object ApkDownloader {

    /**
     * @param onProgress porcentaje 0..100 (solo cuando cambia), o -1 si el
     *        servidor no indica el tamaño total.
     * @param checkActive se llama en cada bloque; puede lanzar una excepción
     *        para cancelar la descarga.
     */
    fun download(
        url: String,
        target: File,
        onProgress: (Int) -> Unit,
        checkActive: () -> Unit = {}
    ) {
        target.absoluteFile.parentFile?.mkdirs()
        val partial = File(target.absoluteFile.parentFile, target.name + ".part")

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "PadelMatch-Android")
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("GitHub respondió con HTTP $code al descargar la actualización.")
            }

            val total = connection.contentLengthLong
            onProgress(if (total > 0) 0 else -1)

            var downloaded = 0L
            var lastPercent = 0
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        checkActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = (downloaded * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }

            if (downloaded == 0L) {
                throw IOException("El archivo descargado está vacío.")
            }
            if (total > 0 && downloaded != total) {
                throw IOException("La descarga quedó incompleta ($downloaded de $total bytes).")
            }
            if (target.exists() && !target.delete()) {
                throw IOException("No se pudo reemplazar la descarga anterior.")
            }
            if (!partial.renameTo(target)) {
                throw IOException("No se pudo guardar el archivo descargado.")
            }
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }
}
