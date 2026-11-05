package com.example.ondavital

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ondavital.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class DatosUsuarioTest {
    private fun contexto(): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val id = UUID.randomUUID().toString()
        val directorio = File(base.cacheDir, "privacidad-$id").apply { mkdirs() }
        return object : ContextWrapper(base) {
            override fun getFilesDir(): File = directorio
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("priv-$id-$name", mode)
        }
    }

    @Test fun leeJsonAnteriorYExportaTodosLosCamposSinSecretos() {
        val c = contexto()
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("diario_v1_legacy.json").use { it.readBytes() }
        File(c.filesDir, "diario_v1.json").writeBytes(bytes)
        val diario = DiarioLocal(c)
        val previos = diario.leer()
        assertEquals(2, previos.size)
        assertTrue(previos.any { it.nota == "Hoy estudié un rato. Después cené con mi familia." && it.actividadIds == setOf("estudio", "familia") })
        diario.guardar(previos)
        assertEquals(previos, diario.leer())
        c.getSharedPreferences("ondavital_perfil", 0).edit().putString("nombre", "Ana demo").putInt("personaje", 2).commit()
        c.getSharedPreferences("ondavital_actividades", 0).edit().putStringSet("actividades_personales", setOf("Dibujé", "Leí")).commit()
        c.getSharedPreferences("ondavital_seguridad", 0).edit().putString("hash", "NO_EXPORTAR_HASH").putString("sal", "NO_EXPORTAR_SAL").commit()
        val json = DatosUsuarioLocal(c).exportar()
        assertEquals("Ana demo", json.getJSONObject("perfil").getString("nombre"))
        assertEquals(2, json.getJSONArray("actividadesPersonales").length())
        assertEquals(2, json.getJSONObject("diario").getJSONArray("registros").length())
        val fila = json.getJSONObject("diario").getJSONArray("registros").getJSONObject(0)
        assertEquals(previos.first().nota, fila.getString("nota"))
        assertEquals(previos.first().fechaIso, fila.getString("fecha"))
        assertEquals(previos.first().energia, fila.getInt("energia"))
        assertTrue(fila.has("fecha") && fila.has("hora") && fila.has("estado") && fila.has("energia") && fila.has("nota") && fila.has("actividades"))
        assertFalse(json.toString().contains("NO_EXPORTAR"))
        assertFalse(json.has("seguridad"))
    }

    @Test fun borrarNoResucitaEjemplosYReanudaSiSeInterrumpe() {
        val c = contexto()
        prepararDemostracion(c, "2026-11-05")
        assertEquals(90, DiarioLocal(c).leer().size)
        c.getSharedPreferences("ondavital_perfil", 0).edit().putString("nombre", "Prueba").commit()
        c.getSharedPreferences("ondavital_seguridad", 0).edit().putString("hash", "temporal").commit()
        DatosUsuarioLocal(c).borrarTodo()
        prepararDemostracion(c, "2026-11-06")
        assertTrue(DiarioLocal(c).leer().isEmpty())
        assertTrue(c.getSharedPreferences("ondavital_perfil", 0).all.isEmpty())
        assertTrue(c.getSharedPreferences("ondavital_seguridad", 0).all.isEmpty())
        c.getSharedPreferences("ondavital_demo_entrega", 0).edit().putBoolean("borrado_en_curso", true).commit()
        c.getSharedPreferences("ondavital_actividades", 0).edit().putStringSet("actividades_personales", setOf("Pendiente")).commit()
        prepararDemostracion(c, "2026-11-07")
        assertTrue(c.getSharedPreferences("ondavital_actividades", 0).all.isEmpty())
        assertFalse(c.getSharedPreferences("ondavital_demo_entrega", 0).getBoolean("borrado_en_curso", false))
    }
}
