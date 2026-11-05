package com.example.ondavital.analisis

import com.example.ondavital.model.RegistroDiario
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.roundToInt

data class Descriptiva(val nDias: Int, val media: Double?, val mediana: Double?,
                       val modas: List<Double>, val desviacion: Double?)

/** Desviación poblacional del periodo observado; no estimación de una población clínica. */
fun descriptiva(valores: List<Double>): Descriptiva {
    if (valores.isEmpty()) return Descriptiva(0, null, null, emptyList(), null)
    val orden = valores.sorted()
    val media = valores.average()
    val frecuencias = valores.groupingBy { it }.eachCount()
    val mayor = frecuencias.values.max()
    return Descriptiva(valores.size, media, (orden[(orden.size - 1) / 2] + orden[orden.size / 2]) / 2,
        frecuencias.filterValues { it == mayor }.keys.sorted(),
        sqrt(valores.sumOf { (it - media) * (it - media) } / valores.size))
}

data class EstadisticaPeriodo(val resumen: ResumenDiario, val animo: Descriptiva,
    val energia: Descriptiva, val rachaMaxima: Int, val rachaAlCierre: Int)

fun estadisticaPeriodo(registros: List<RegistroDiario>, inicio: String, fin: String): EstadisticaPeriodo {
    val resumen = resumirDiario(registros, inicio, fin)
    var racha = 0
    var maxima = 0
    var previa: String? = null
    resumen.dias.forEach { dia ->
        racha = if (previa?.let { moverDia(it, 1) } == dia.fecha) racha + 1 else 1
        maxima = maxOf(maxima, racha)
        previa = dia.fecha
    }
    return EstadisticaPeriodo(resumen, descriptiva(resumen.dias.map { it.animo }),
        descriptiva(resumen.dias.map { it.energia }), maxima, if (previa == fin) racha else 0)
}

data class PuntoTendencia(val fecha: String, val animo: Double?, val energia: Double?,
    val mediaAnimo: Double?, val mediaEnergia: Double?, val diasVentana: Int)

/** Ventana retrospectiva de 7 fechas, recortada al rango. Hueco = null, nunca cero.
 * No se dibuja media en fechas sin registro; los días disponibles pesan por igual. */
fun tendencia(resumen: ResumenDiario): List<PuntoTendencia> {
    val porFecha = resumen.dias.associateBy { it.fecha }
    return generateSequence(resumen.inicio) { moverDia(it, 1) }.takeWhile { it <= resumen.fin }.map { fecha ->
        val dia = porFecha[fecha]
        val inicio = maxOf(resumen.inicio, moverDia(fecha, -6))
        val ventana = resumen.dias.filter { it.fecha in inicio..fecha }
        PuntoTendencia(fecha, dia?.animo, dia?.energia,
            if (dia != null) ventana.map { it.animo }.average() else null,
            if (dia != null) ventana.map { it.energia }.average() else null, ventana.size)
    }.toList()
}

enum class EstadoPista { INSUFICIENTE, PEQUENA, OBSERVADA }
data class ExplicacionLocal(val estado: EstadoPista, val texto: String, val evidencia: String)

/** Módulo D: reglas y plantillas locales, sin entrenamiento ni inferencia clínica. */
fun explicarActividad(resumen: ResumenDiario, a: ActividadResumen): ExplicacionLocal {
    val estado = when {
        !a.suficiente -> EstadoPista.INSUFICIENTE
        abs(a.diferencia!!) < .5 && abs(a.diferenciaAnimo!!) < .5 -> EstadoPista.PEQUENA
        else -> EstadoPista.OBSERVADA
    }
    val texto = when (estado) {
        EstadoPista.INSUFICIENTE -> "Aún no hay datos suficientes para encontrar una pista con esta actividad. Puedes seguir guardando tus días, sin prisa."
        EstadoPista.PEQUENA -> "Con o sin esta actividad, tu ánimo y tu energía fueron muy parecidos. Por ahora no hay una diferencia que destacar."
        EstadoPista.OBSERVADA -> {
            fun contar(valor: Double): String = if (abs(abs(valor) - abs(valor).roundToInt()) < 1e-9) abs(valor).roundToInt().toString() else numeroDiario(abs(valor))
            fun cambio(nombre: String, diferencia: Double): String =
                if (abs(diferencia) < .5) "tu $nombre se mantuvo casi igual"
                else "tu $nombre fue ${contar(diferencia)} ${if (abs(diferencia) == 1.0) "punto" else "puntos"} ${if (diferencia > 0) "mayor" else "menor"}"
            "Los días que marcaste «${etiquetaActividad(a.id)}», ${cambio("energía", a.diferencia!!)} y ${cambio("ánimo", a.diferenciaAnimo!!)}, en comparación con los días sin esa actividad."
        }
    }
    val evidencia = "${resumen.inicio} a ${resumen.fin}. ${resumen.dias.size} días y ${resumen.registros.size} registros. " +
        "Con actividad: ${a.diasCon} días, ${a.registrosCon} registros; sin actividad: ${a.diasSin} días, ${a.registrosSin} registros. " +
        "Ánimo con/sin: ${numeroDiario(a.animoCon)} / ${numeroDiario(a.animoSin)}; energía con/sin: ${numeroDiario(a.energiaCon)} / ${numeroDiario(a.energiaSin)}. " +
        "Media general: ánimo ${numeroDiario(a.animoGeneral)} / 8; energía ${numeroDiario(a.energiaGeneral)} / 10. " +
        "Diferencia frente a la media general: ánimo ${numeroDiario(a.diferenciaGeneralAnimo)}, energía ${numeroDiario(a.diferenciaGeneralEnergia)} puntos. " +
        "Método: diferencia de medias diarias, cada día pesa igual; una actividad cuenta si aparece en cualquier registro del día. " +
        "Umbral práctico de 0.5 puntos, no validado clínicamente. El ánimo es ordinal: promediarlo es una aproximación. " +
        "Pueden coexistir actividades y otros factores. Explorar muchas comparaciones puede destacar coincidencias por azar. " +
        "Los mínimos no garantizan confiabilidad. No hay prueba de significación ni evidencia de causalidad."
    return ExplicacionLocal(estado, texto, evidencia)
}
