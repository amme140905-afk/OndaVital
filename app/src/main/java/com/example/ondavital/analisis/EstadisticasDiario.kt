package com.example.ondavital.analisis

import com.example.ondavital.model.EstadoAnimo
import com.example.ondavital.model.RegistroDiario
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Fechas locales del diario. Se opera al mediodía para respetar los cambios de horario. */
fun hoyDiario(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun calendarioDia(fecha: String): Calendar {
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    return Calendar.getInstance().apply {
        time = requireNotNull(formato.parse(fecha))
        set(Calendar.HOUR_OF_DAY, 12)
    }
}

fun moverDia(fecha: String, dias: Int): String {
    val calendario = calendarioDia(fecha).apply { add(Calendar.DAY_OF_MONTH, dias) }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendario.time)
}

fun lunesSemana(fecha: String): String {
    val calendario = calendarioDia(fecha)
    return moverDia(fecha, -((calendario.get(Calendar.DAY_OF_WEEK) + 5) % 7))
}

fun fechaBonita(fecha: String): String =
    SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(calendarioDia(fecha).time)

fun numeroDiario(valor: Double?): String = valor?.let { String.format(Locale("es", "MX"), "%.1f", it) } ?: "—"

fun etiquetaActividad(id: String): String = when (id) {
    "estudio" -> "Estudié"; "trabajo" -> "Trabajé"; "caminata" -> "Caminé"
    "ejercicio" -> "Ejercicio"; "amistades" -> "Vi amigos"; "familia" -> "Familia"
    "descanso" -> "Descansé"; "juegos" -> "Jugué"; else -> id.removePrefix("personal:")
}

data class MediaDia(val fecha: String, val animo: Double, val energia: Double,
                    val registros: Int, val actividades: Set<String>)
data class ActividadResumen(val id: String, val diasCon: Int, val diasSin: Int,
                            val energiaCon: Double, val energiaSin: Double?,
                            val animoCon: Double, val animoSin: Double?,
                            val registrosCon: Int, val registrosSin: Int,
                            val energiaGeneral: Double, val animoGeneral: Double) {
    val suficiente: Boolean get() = diasCon + diasSin >= 14 && diasCon >= 5 && diasSin >= 5
    val diferencia: Double? get() = if (suficiente) energiaSin?.let { energiaCon - it } else null
    val diferenciaAnimo: Double? get() = if (suficiente) animoSin?.let { animoCon - it } else null
    val diferenciaGeneralEnergia: Double get() = energiaCon - energiaGeneral
    val diferenciaGeneralAnimo: Double get() = animoCon - animoGeneral
}
data class ResumenDiario(val inicio: String, val fin: String, val registros: List<RegistroDiario>,
                        val dias: List<MediaDia>, val frecuencias: List<Pair<EstadoAnimo, Int>>,
                        val actividades: List<ActividadResumen>) {
    val animo: Double? get() = dias.takeIf { it.isNotEmpty() }?.map { it.animo }?.average()
    val energia: Double? get() = dias.takeIf { it.isNotEmpty() }?.map { it.energia }?.average()
    val estadosFrecuentes: List<EstadoAnimo> get() {
        val mayor = frecuencias.maxOfOrNull { it.second } ?: return emptyList()
        return frecuencias.filter { it.second == mayor }.map { it.first }
    }
}

/** Cada día pesa lo mismo en medias y asociaciones. Días vacíos no cuentan como cero. */
fun resumirDiario(registros: List<RegistroDiario>, inicio: String, fin: String): ResumenDiario {
    require(inicio <= fin)
    val filtrados = registros.filter { it.fechaIso >= inicio && it.fechaIso <= fin }
        .sortedByDescending { it.horaRegistroEpochMs }
    val dias = filtrados.groupBy { it.fechaIso }.toSortedMap().map { (fecha, lista) ->
        MediaDia(fecha, lista.map { it.puntuacionAnimo }.average(), lista.map { it.energia }.average(),
            lista.size, lista.flatMap { it.actividadIds }.toSet())
    }
    val frecuencias = EstadoAnimo.values().map { estado -> estado to filtrados.count { it.estado == estado } }
        .filter { it.second > 0 }.sortedByDescending { it.second }
    val actividades = dias.flatMap { it.actividades }.distinct().map { id ->
        val con = dias.filter { id in it.actividades }
        val sin = dias.filter { id !in it.actividades }
        ActividadResumen(id, con.size, sin.size, con.map { it.energia }.average(),
            sin.takeIf { it.isNotEmpty() }?.map { it.energia }?.average(),
            con.map { it.animo }.average(), sin.takeIf { it.isNotEmpty() }?.map { it.animo }?.average(),
            con.sumOf { it.registros }, sin.sumOf { it.registros },
            dias.map { it.energia }.average(), dias.map { it.animo }.average())
    }.sortedWith(compareByDescending<ActividadResumen> { it.diasCon }.thenBy { it.id })
    return ResumenDiario(inicio, fin, filtrados, dias, frecuencias, actividades)
}
