package com.example.ondavital.analisis

import com.example.ondavital.model.EstadoAnimo
import com.example.ondavital.model.RegistroDiario
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class PatronesDiarioTest {
    @Test fun explicacionesHablanDePuntosSinAfirmarCausas() {
        listOf(EscenarioDemo.POSITIVA to "mayor", EscenarioDemo.NEGATIVA to "menor").forEach { (e, direccion) ->
            val r = resumirDiario(generarDemostracion(e, "2026-11-05"), "2026-08-08", "2026-11-05")
            val a = r.actividades.first { it.id == "caminata" }
            val texto = explicarActividad(r, a).texto
            assertTrue(texto.contains("tu energía fue 3 puntos $direccion"))
            assertTrue(texto.contains("los días sin esa actividad"))
            assertFalse(listOf("media", "desviación", "sube", "baja", "causa").any { it in texto })
        }
    }
    private fun r(fecha: String, energia: Int) = RegistroDiario(fecha, 1, EstadoAnimo.NEUTRAL, energia, "", emptySet())

    @Test fun calculoManualYEmpates() {
        val d = descriptiva(listOf(2.0, 2.0, 4.0, 4.0))
        assertEquals(3.0, d.media!!, 1e-9)
        assertEquals(3.0, d.mediana!!, 1e-9)
        assertEquals(1.0, d.desviacion!!, 1e-9)
        assertEquals(listOf(2.0, 4.0), d.modas)
        assertEquals(2.0, descriptiva(listOf(1.0, 2.0, 9.0)).mediana!!, 1e-9)
        assertEquals(sqrt(8.0 / 3), descriptiva(listOf(2.0, 4.0, 6.0)).desviacion!!, 1e-9)
    }

    @Test fun vacioYConstante() {
        assertNull(descriptiva(emptyList()).media)
        assertNull(descriptiva(emptyList()).mediana)
        assertNull(descriptiva(emptyList()).desviacion)
        assertEquals(0.0, descriptiva(listOf(5.0)).desviacion!!, 0.0)
    }

    @Test fun huecoCortaRachaYMediaNoMiraFuturo() {
        val datos = listOf(r("2026-12-30", 2), r("2026-12-31", 4), r("2027-01-02", 10))
        val p = estadisticaPeriodo(datos, "2026-12-30", "2027-01-03")
        assertEquals(2, p.rachaMaxima)
        assertEquals(0, p.rachaAlCierre)
        val t = tendencia(p.resumen)
        assertEquals(2.0, t[0].mediaEnergia!!, 0.0)
        assertEquals(3.0, t[1].mediaEnergia!!, 0.0)
        assertNull(t[2].energia); assertNull(t[2].mediaEnergia)
        assertEquals(16.0 / 3, t[3].mediaEnergia!!, 1e-9)
        assertEquals(1, estadisticaPeriodo(datos, "2027-01-01", "2027-01-02").rachaAlCierre)
    }

    @Test fun ventanaMovilExpulsaFechasAntiguas() {
        val resumen = resumirDiario(listOf(r("2026-11-01", 10), r("2026-11-08", 2)), "2026-11-01", "2026-11-08")
        assertEquals(2.0, tendencia(resumen).last().mediaEnergia!!, 0.0)
        assertEquals(1, tendencia(resumen).last().diasVentana)
    }

    @Test fun generadorReproducibleYNoModificaFuente() {
        val a = generarDemostracion(EscenarioDemo.HUECOS, "2026-11-04")
        assertEquals(a, generarDemostracion(EscenarioDemo.HUECOS, "2026-11-04"))
        assertNotEquals(a, generarDemostracion(EscenarioDemo.HUECOS, "2026-11-04", 2))
        assertTrue(a.map { it.fechaIso }.distinct().size < 90)
        assertTrue(a.size > a.map { it.fechaIso }.distinct().size)
        assertTrue(a.all { it.energia in 1..10 && it.puntuacionAnimo in 1..8 })
    }

    @Test fun escenariosEncuentranDireccionYRespetanMinimos() {
        EscenarioDemo.values().forEach { e ->
            val datos = generarDemostracion(e, "2026-11-04")
            val resumen = resumirDiario(datos, "2026-08-07", "2026-11-04")
            val a = resumen.actividades.first { it.id == "caminata" }
            val x = explicarActividad(resumen, a)
            when (e) {
                EscenarioDemo.POSITIVA, EscenarioDemo.HUECOS -> { assertTrue(a.diferencia!! > 2); assertEquals(EstadoPista.OBSERVADA, x.estado) }
                EscenarioDemo.NEGATIVA -> { assertTrue(a.diferencia!! < -2); assertEquals(EstadoPista.OBSERVADA, x.estado) }
                EscenarioDemo.SIN_ASOCIACION, EscenarioDemo.CONSTANTE -> { assertEquals(0.0, a.diferencia!!, 0.0); assertEquals(EstadoPista.PEQUENA, x.estado) }
                EscenarioDemo.INSUFICIENTE -> { assertNull(a.diferencia); assertEquals(EstadoPista.INSUFICIENTE, x.estado) }
            }
            resumen.actividades.filter { it.id != "caminata" }.forEach {
                assertEquals(EstadoPista.INSUFICIENTE, explicarActividad(resumen, it).estado)
            }
        }
    }

    @Test fun diferenciaPequenaNoSeDestacaYLimiteEsExplicito() {
        val r = resumirDiario(generarDemostracion(EscenarioDemo.CONSTANTE, "2026-11-04"), "2026-08-07", "2026-11-04")
        val a = r.actividades.first { it.id == "caminata" }
        assertEquals(EstadoPista.PEQUENA, explicarActividad(r, a.copy(energiaCon = a.energiaSin!! + .49)).estado)
        assertEquals(EstadoPista.OBSERVADA, explicarActividad(r, a.copy(energiaCon = a.energiaSin!! + .5)).estado)
    }

    @Test fun evaluacionModuloDFrenteABaseline() {
        // Oracle independiente: 90 días completos alternos, salvo escenario insuficiente (8).
        // Baseline sencillo: siempre emitir una asociación sin umbral ni distinción de tamaño.
        var aciertos = 0; var baseline = 0; var causales = 0; var factuales = 0; var total = 0
        EscenarioDemo.values().forEach { e ->
            val esperado = when (e) {
                EscenarioDemo.INSUFICIENTE -> EstadoPista.INSUFICIENTE
                EscenarioDemo.CONSTANTE, EscenarioDemo.SIN_ASOCIACION -> EstadoPista.PEQUENA
                else -> EstadoPista.OBSERVADA
            }
            val r = resumirDiario(generarDemostracion(e, "2026-11-04"), "2026-08-07", "2026-11-04")
            val a = r.actividades.first { it.id == "caminata" }
            val x = explicarActividad(r, a)
            total++
            if (x.estado == esperado) aciertos++
            if (EstadoPista.OBSERVADA == esperado) baseline++
            if (listOf("provoca", "mejora tu", "debes", "diagnóstico:", "causa un").any { it in x.texto.lowercase() }) causales++
            if (x.evidencia.contains("${r.dias.size} días y ${r.registros.size} registros") &&
                x.evidencia.contains("${a.diasCon} días, ${a.registrosCon} registros") &&
                x.evidencia.contains(numeroDiario(a.energiaCon))) factuales++
        }
        assertEquals(6, aciertos); assertEquals(3, baseline); assertEquals(0, causales); assertEquals(6, factuales)
        println("MODULO_D casos=$total clasificacion=$aciertos/$total baseline=$baseline/$total evidencia=$factuales/$total expresiones_indebidas=$causales/$total")
    }
}
