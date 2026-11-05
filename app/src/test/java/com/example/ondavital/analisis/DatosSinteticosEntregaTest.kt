package com.example.ondavital.analisis

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.TimeZone

/** Exporta exclusivamente datos del generador; nunca abre DiarioLocal ni preferencias. */
class DatosSinteticosEntregaTest {
    @Test fun generarArchivosYValidarPatronesPlantados() {
        val anterior = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"))
            val salida = File("build/datos-sinteticos").apply { mkdirs() }
            val fin = "2026-11-05"
            val semilla = 20271L
            val filas = mutableListOf<String>()
            fun q(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
            EscenarioDemo.values().forEach { escenario ->
                val datos = generarDemostracion(escenario, fin, semilla)
                val r = resumirDiario(datos, moverDia(fin, -89), fin)
                val a = r.actividades.first { it.id == "caminata" }
                val diasEsperados = when (escenario) {
                    EscenarioDemo.INSUFICIENTE -> 8
                    EscenarioDemo.HUECOS -> 81
                    else -> 90
                }
                assertEquals(diasEsperados, r.dias.size)
                assertEquals(if (escenario == EscenarioDemo.HUECOS) 92 else diasEsperados, datos.size)
                assertEquals(datos.size, datos.map { it.horaRegistroEpochMs }.distinct().size)
                assertTrue(datos.all { it.fechaIso <= fin && it.nota == "Dato sintético de demostración" })
                when (escenario) {
                    EscenarioDemo.POSITIVA -> { assertEquals(3.0, a.diferencia!!, 1e-9); assertEquals(3.0, a.diferenciaAnimo!!, 1e-9) }
                    EscenarioDemo.NEGATIVA -> { assertEquals(-3.0, a.diferencia!!, 1e-9); assertEquals(-3.0, a.diferenciaAnimo!!, 1e-9) }
                    EscenarioDemo.SIN_ASOCIACION, EscenarioDemo.CONSTANTE -> assertEquals(0.0, a.diferencia!!, 1e-9)
                    EscenarioDemo.INSUFICIENTE -> assertFalse(a.suficiente)
                    EscenarioDemo.HUECOS -> assertTrue(a.diferencia!! > 2)
                }
                if (escenario == EscenarioDemo.CONSTANTE) {
                    assertEquals(0.0, descriptiva(r.dias.map { it.energia }).desviacion!!, 0.0)
                    assertFalse(r.actividades.first { it.id.startsWith("personal:") }.suficiente)
                }
                val registros = datos.joinToString(",\n") { d ->
                    "{\"fecha\":${q(d.fechaIso)},\"hora\":${d.horaRegistroEpochMs},\"estado\":${q(d.estado.id)}," +
                        "\"energia\":${d.energia},\"nota\":${q(d.nota)},\"actividades\":[${d.actividadIds.sorted().joinToString(",") { q(it) }}]}"
                }
                File(salida, "${escenario.name.lowercase()}.json").writeText(
                    "{\"version\":1,\"demostracion\":true,\"escenario\":${q(escenario.name)},\"semilla\":$semilla," +
                        "\"fin\":${q(fin)},\"zonaHoraria\":\"America/Mexico_City\",\"registros\":[\n$registros\n]}", Charsets.UTF_8)
                filas += "| ${escenario.etiqueta} | ${datos.size} | ${r.dias.size} | ${a.diasCon} / ${a.diasSin} | ${numeroDiario(a.diferencia)} | ${explicarActividad(r, a).estado} |"
            }
            File(salida, "RESUMEN.md").writeText("""
                # Datos sintéticos de OndaVital

                Semilla: $semilla. Fecha final: $fin. Zona: America/Mexico_City.
                Archivos generados por el mismo motor Kotlin que utiliza la aplicación.
                Son ejemplos de prueba, no datos personales ni resultados clínicos.

                | Escenario | Registros | Días | Días con / sin caminata | Diferencia energía (puntos) | Resultado esperado |
                |---|---:|---:|---:|---:|---|
            """.trimIndent() + "\n" + filas.joinToString("\n") + "\n", Charsets.UTF_8)
            println("DATOS_SINTETICOS: ${salida.absolutePath}")
        } finally {
            TimeZone.setDefault(anterior)
        }
    }
}
