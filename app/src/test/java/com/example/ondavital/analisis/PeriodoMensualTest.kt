package com.example.ondavital.analisis

import org.junit.Assert.*
import org.junit.Test

class PeriodoMensualTest {
    @Test fun limitesBisiestosYCambioDeAnio() {
        assertEquals("2024-02-29", finMes("2024-02"))
        assertEquals("2026-02-28", finMes("2026-02"))
        assertEquals("2027-01", moverMes("2026-12", 1))
        assertEquals("2025-12", moverMes("2026-01", -1))
    }
    @Test(expected = IllegalArgumentException::class) fun rechazaMesInvalido() { finMes("2026-13") }
}
