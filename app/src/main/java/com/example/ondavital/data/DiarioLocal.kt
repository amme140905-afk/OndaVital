package com.example.ondavital.data

import android.content.Context
import android.util.AtomicFile
import com.example.ondavital.model.EstadoAnimo
import com.example.ondavital.model.RegistroDiario
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/** Leer y escribir desde Dispatchers.IO. AtomicFile conserva la copia anterior ante fallos. */
class DiarioLocal(context: Context) {
    private val archivo = AtomicFile(File(context.filesDir, "diario_v1.json"))

    fun leer(): List<RegistroDiario> {
        val texto = try {
            archivo.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            // Una copia de recuperación también cuenta como diario existente.
            if (archivo.baseFile.exists() || File(archivo.baseFile.path + ".bak").exists()) {
                throw IllegalStateException("No se pudo abrir el diario existente")
            }
            return emptyList()
        }
        val raiz = JSONObject(texto)
        require(raiz.getInt("version") == 1) { "Versión de diario desconocida" }
        val filas = raiz.getJSONArray("registros")
        return (0 until filas.length()).map { i ->
            val fila = filas.getJSONObject(i)
            val actividades = fila.getJSONArray("actividades")
            RegistroDiario(
                fila.getString("fecha"), fila.getLong("hora"),
                EstadoAnimo.values().first { it.id == fila.getString("estado") },
                fila.getInt("energia"), fila.getString("nota"),
                (0 until actividades.length()).map { actividades.getString(it) }.toSet()
            )
        }.sortedByDescending { it.horaRegistroEpochMs }
    }

    fun guardar(registros: List<RegistroDiario>) {
        val bytes = jsonDiario(registros).toString().toByteArray(Charsets.UTF_8)
        val salida = archivo.startWrite()
        try {
            salida.write(bytes)
            archivo.finishWrite(salida)
        } catch (error: Exception) {
            archivo.failWrite(salida)
            throw error
        }
    }
}

internal fun jsonDiario(registros: List<RegistroDiario>): JSONObject {
        val filas = JSONArray()
        registros.forEach { r ->
            filas.put(JSONObject().put("fecha", r.fechaIso).put("hora", r.horaRegistroEpochMs)
                .put("estado", r.estado.id).put("energia", r.energia).put("nota", r.nota)
                .put("actividades", JSONArray(r.actividadIds.sorted())))
        }
        return JSONObject().put("version", 1).put("registros", filas)
}
