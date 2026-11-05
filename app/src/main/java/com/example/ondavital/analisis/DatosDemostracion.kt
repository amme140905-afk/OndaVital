package com.example.ondavital.analisis

import com.example.ondavital.model.EstadoAnimo
import com.example.ondavital.model.RegistroDiario
import java.util.Random
import java.text.SimpleDateFormat
import java.util.Locale

enum class EscenarioDemo(val etiqueta: String) {
    POSITIVA("Asociación positiva"), NEGATIVA("Asociación negativa"),
    SIN_ASOCIACION("Sin asociación"), INSUFICIENTE("Datos insuficientes"),
    HUECOS("Huecos y varios momentos"), CONSTANTE("Valores constantes y actividad rara")
}

/** No conoce el repositorio ni escribe archivos. Fechas, semilla y hora base reproducibles. */
fun generarDemostracion(escenario: EscenarioDemo, fin: String, semilla: Long = 20271): List<RegistroDiario> {
    val azar = Random(semilla)
    val cantidad = if (escenario == EscenarioDemo.INSUFICIENTE) 8 else 90
    val ruidoPorPar = List((cantidad + 1) / 2) { azar.nextInt(3) - 1 }
    val reloj = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
    return (0 until cantidad).flatMap { i ->
        if (escenario == EscenarioDemo.HUECOS && i % 11 == 0) return@flatMap emptyList()
        val con = i % 2 == 0
        // Por pares: el mismo ruido en ambos grupos mantiene el escenario nulo equilibrado.
        val ruido = ruidoPorPar[i / 2]
        val cambio = when (escenario) {
            EscenarioDemo.POSITIVA, EscenarioDemo.HUECOS -> if (con) 2 else -1
            EscenarioDemo.NEGATIVA -> if (con) -2 else 1
            else -> 0
        }
        val variacion = if (escenario == EscenarioDemo.CONSTANTE) 0 else ruido
        val animo = (5 + cambio + variacion).coerceIn(1, 8)
        val energia = (6 + cambio + variacion).coerceIn(1, 10)
        val actividades = buildSet {
            if (con) add("caminata")
            if (i == 5 || i == 43) add("personal:Actividad poco frecuente")
        }
        val fecha = moverDia(fin, i - cantidad + 1)
        val r = RegistroDiario(fecha, requireNotNull(reloj.parse("$fecha 12:00")).time,
            EstadoAnimo.values().first { it.puntuacion == animo }, energia, "Dato sintético de demostración", actividades)
        if (escenario == EscenarioDemo.HUECOS && i % 7 == 0) listOf(r, r.copy(horaRegistroEpochMs = r.horaRegistroEpochMs + 1000))
        else listOf(r)
    }
}
