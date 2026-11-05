package com.example.ondavital.analisis

import org.junit.Assert.*
import org.junit.Test

class DiarioEjemploTest {
    @Test fun notasYActividadesParecenUnDiarioYSeReproducen() {
        val datos = generarDiarioEjemplo("2026-11-05")
        assertEquals(datos, generarDiarioEjemplo("2026-11-05"))
        assertEquals(90, datos.size)
        assertEquals(90, datos.map { it.fechaIso }.distinct().size)
        assertTrue(datos.map { it.nota }.distinct().size >= 14)
        assertTrue(datos.flatMap { it.actividadIds }.distinct().size >= 12)
        assertTrue(datos.any { "guiso" in it.nota && "familia" in it.actividadIds })
        assertTrue(datos.any { "bailar" in it.nota && "personal:Bailé" in it.actividadIds })
        assertTrue(datos.all { it.nota.length in 20..500 })
        assertTrue(datos.filter { "contento" in it.nota }.all { it.puntuacionAnimo >= 7 })
    }
}
