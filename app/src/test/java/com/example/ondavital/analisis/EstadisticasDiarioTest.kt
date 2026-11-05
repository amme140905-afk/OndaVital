package com.example.ondavital.analisis

import com.example.ondavital.model.EstadoAnimo
import com.example.ondavital.model.RegistroDiario
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class EstadisticasDiarioTest {
    private fun registro(fecha: String, energia: Int = 6, estado: EstadoAnimo = EstadoAnimo.ALEGRE,
                         actividades: Set<String> = emptySet()) = RegistroDiario(fecha, 1L, estado, energia, "", actividades)

    @Test fun variosMomentosNoDanMasPesoAUnDia() {
        val datos = List(3) { registro("2026-09-28", 10, EstadoAnimo.ILUSIONADO) } +
            registro("2026-09-29", 2, EstadoAnimo.ABRUMADO)
        val r = resumirDiario(datos, "2026-09-28", "2026-10-04")
        assertEquals(6.0, r.energia!!, .00001)
        assertEquals(4.5, r.animo!!, .00001)
        assertEquals(2, r.dias.size)
        assertEquals(4, r.registros.size)
        assertEquals(3, r.frecuencias.first().second)
    }

    @Test fun vaciosYPeriodosSinDatosNoSonCero() {
        val r = resumirDiario(listOf(registro("2026-08-01")), "2026-09-28", "2026-10-04")
        assertNull(r.energia); assertNull(r.animo)
        assertTrue(r.frecuencias.isEmpty()); assertTrue(r.actividades.isEmpty())
    }

    @Test fun intervaloIncluyeAmbosExtremos() {
        val datos = listOf("2026-09-27", "2026-09-28", "2026-10-04", "2026-10-05").map { registro(it) }
        assertEquals(listOf("2026-09-28", "2026-10-04"), resumirDiario(datos, "2026-09-28", "2026-10-04").dias.map { it.fecha })
    }

    @Test fun actividadPersonalCuentaDiasUnicosYConservaEtiqueta() {
        val id = "personal:Hice japonés"
        val datos = listOf(registro("2026-09-28", actividades = setOf(id)), registro("2026-09-28", actividades = setOf(id)))
        val r = resumirDiario(datos, "2026-09-28", "2026-10-04")
        assertEquals(1, r.actividades.single().diasCon)
        assertNull(r.actividades.single().diferencia)
        assertEquals("Hice japonés", etiquetaActividad(id))
    }

    @Test fun asociacionExigeCatorceDiasYCincoPorGrupo() {
        val id = "caminata"
        val con = (0..4).map { registro(moverDia("2026-09-28", it), 8, actividades = setOf(id)) }
        val sin = (5..13).map { registro(moverDia("2026-09-28", it), 4) }
        val completo = resumirDiario(con + sin, "2026-09-28", "2026-10-11").actividades.single()
        assertEquals(4.0, completo.diferencia!!, .00001)
        assertEquals(8.0 - 76.0 / 14, completo.diferenciaGeneralEnergia, .00001)
        val repetidos = resumirDiario(con + sin + List(9) { con.first() }, "2026-09-28", "2026-10-11").actividades.single()
        assertEquals(5, repetidos.diasCon)
        assertEquals(14, repetidos.registrosCon)
        assertEquals(9, repetidos.registrosSin)
        assertEquals(completo.diferencia, repetidos.diferencia)
        assertNull(resumirDiario(con + sin.take(8), "2026-09-28", "2026-10-11").actividades.single().diferencia)
        val sinExtra = sin + registro("2026-10-12", 4)
        assertNull(resumirDiario(con.take(4) + sinExtra, "2026-09-28", "2026-10-12").actividades.single().diferencia)
    }

    @Test fun empatesDeFrecuenciaNoInventanUnGanador() {
        val r = resumirDiario(listOf(registro("2026-09-28", estado = EstadoAnimo.TRISTE),
            registro("2026-09-29", estado = EstadoAnimo.ALEGRE)), "2026-09-28", "2026-10-04")
        assertEquals(setOf(EstadoAnimo.TRISTE, EstadoAnimo.ALEGRE), r.estadosFrecuentes.toSet())
    }

    @Test fun semanasCruzanMesYAnio() {
        assertEquals("2026-09-28", lunesSemana("2026-10-04"))
        assertEquals("2026-12-28", lunesSemana("2027-01-01"))
        assertEquals("2027-01-03", moverDia("2026-12-28", 6))
        assertEquals("2024-02-29", moverDia("2024-02-28", 1))
    }

    @Test fun cambiosDeHorarioMantienenDiasLocales() {
        val anterior = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals("2026-03-09", moverDia("2026-03-07", 2))
            assertEquals("2026-11-02", moverDia("2026-10-31", 2))
        } finally { TimeZone.setDefault(anterior) }
    }
}
