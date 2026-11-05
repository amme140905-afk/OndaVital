package com.example.ondavital.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

/** Alternativa sin permisos de almacenamiento, solo Android 10+. Publica únicamente al completar. */
fun guardarPdfEnDescargas(context: Context, nombre: String, escribir: (OutputStream) -> Unit) {
    check(Build.VERSION.SDK_INT >= 29)
    val resolver = context.contentResolver
    val valores = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, nombre)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OndaVital")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores))
    try {
        requireNotNull(resolver.openOutputStream(uri)).use(escribir)
        check(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) > 0)
    } catch (e: Exception) {
        resolver.delete(uri, null, null) // Solo la fila nueva creada por esta operación.
        throw e
    }
}
