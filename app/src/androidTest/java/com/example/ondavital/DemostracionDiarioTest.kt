package com.example.ondavital

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ondavital.data.*
import com.example.ondavital.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class DemostracionDiarioTest {
    @Test fun sustituyeEjemplosSinRespaldoYNoReemplazaDeNuevo() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val id = UUID.randomUUID().toString()
        val directorio = File(base.cacheDir, "demo-test-$id").apply { mkdirs() }
        val contexto = object : ContextWrapper(base) {
            override fun getFilesDir(): File = directorio
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("test-$id-$name", mode)
        }
        try {
            val anterior = RegistroDiario("2025-01-01", 1, EstadoAnimo.EN_CALMA, 6, "Recuerdo de prueba", emptySet())
            val diario = DiarioLocal(contexto)
            diario.guardar(listOf(anterior))
            prepararDemostracion(contexto, "2026-11-05")
            assertFalse(File(directorio, "diario_antes_demo_v1.json").exists())
            val demo = diario.leer()
            assertEquals(90, demo.size)
            assertEquals(90, demo.map { it.fechaIso }.distinct().size)
            assertTrue(demo.map { it.estado }.distinct().size > 1) // Colores diferentes en el calendario.
            diario.guardar(demo + anterior)
            prepararDemostracion(contexto, "2026-11-06")
            assertEquals(91, diario.leer().size)
            assertFalse(File(directorio, "diario_antes_demo_v1.json").exists())
        } finally {
            // Solo borra archivos creados por esta prueba dentro de su directorio único.
            directorio.listFiles()?.forEach { it.delete() }
            directorio.delete()
            contexto.getSharedPreferences("ondavital_demo_entrega", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
