package com.example.ondavital.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ondavital.R
import com.example.ondavital.analisis.*
import com.example.ondavital.model.RegistroDiario
import com.example.ondavital.ui.components.TarjetaPapel
import com.example.ondavital.ui.theme.OndaColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PaginaPatrones(val resumen: ResumenDiario, val semana: EstadisticaPeriodo,
    val mes: EstadisticaPeriodo, val puntos: List<PuntoTendencia>, val pistas: List<ExplicacionLocal>)

@Composable
fun PantallaAnalisis(registros: List<RegistroDiario>, dias: Int, onDias: (Int) -> Unit,
    onReporte: () -> Unit, onAbrir: (RegistroDiario) -> Unit = {}, demostracionGlobal: Boolean = false,
    onReporteMensual: (() -> Unit)? = null) {
    var demo by rememberSaveable { mutableStateOf(false) }
    var escenarioIndice by rememberSaveable { mutableIntStateOf(0) }
    var elegirEscenario by rememberSaveable { mutableStateOf(false) }
    val escenario = EscenarioDemo.values()[escenarioIndice]
    val hoy = hoyDiario()
    CabeceraCuaderno("Tus pequeñas mareas", "Una mirada amable a tus días.", R.drawable.mascota_lee)
    Letra("OndaVital no realiza diagnósticos médicos ni sustituye atención profesional.", 16)
    if (!demostracionGlobal) TextButton(onClick = { demo = !demo }) { Letra(if (demo) "Salir de demostración" else "Explorar datos de demostración", 18) }
    if (demo) {
        TarjetaPapel(color = OndaColor.PapelRosa) {
            Letra("DEMOSTRACIÓN · datos sintéticos", 23)
            Letra("Semilla 20271 · tu diario personal permanece separado.", 16)
            TextButton(onClick = { elegirEscenario = !elegirEscenario }) {
                Letra("Escenario: ${escenario.etiqueta} ${if (elegirEscenario) "▴" else "▾"}", 18)
            }
            if (elegirEscenario) EscenarioDemo.values().forEachIndexed { i, e ->
                TextButton(onClick = { escenarioIndice = i; elegirEscenario = false }) { Letra("${if (i == escenarioIndice) "●" else "○"} ${e.etiqueta}", 17) }
            }
        }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        listOf(7, 30, 90).forEach { n ->
            FilterChip(selected = dias == n, onClick = { onDias(n) }, label = { Letra("$n días", 18) }, modifier = Modifier.padding(end = 8.dp))
        }
    }
    // Al cambiar la fuente o el rango se descarta el cálculo anterior, incluso durante la carga.
    key(registros, dias, demo, escenario, hoy) {
        val pagina by produceState<PaginaPatrones?>(initialValue = null) {
            value = withContext(Dispatchers.Default) {
                val fuente = if (demo) generarDemostracion(escenario, hoy) else registros
                val r = resumirDiario(fuente, moverDia(hoy, 1 - dias), hoy)
                PaginaPatrones(r, estadisticaPeriodo(fuente, lunesSemana(hoy), hoy),
                    estadisticaPeriodo(fuente, hoy.take(7) + "-01", hoy), tendencia(r),
                    r.actividades.map { explicarActividad(r, it) })
            }
        }
        val p = pagina
        if (p == null) {
            Letra("Dibujando tus mareas…", 18)
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = OndaColor.Rosa)
        } else {
            val r = p.resumen
            Letra("${fechaBonita(r.inicio)} — ${fechaBonita(r.fin)}", 16)
            TarjetaPapel(color = OndaColor.PapelCeleste) {
                Letra("${r.dias.size} días · ${r.registros.size} registros", 24)
                Letra("Cada momento que guardaste nos ayuda a conocer un poquito mejor tus días.", 18)
            }
            var estadisticas by remember { mutableStateOf(false) }
            BotonCuaderno(if (estadisticas) "Cerrar detalles" else "Ver detalles de mi semana y mi mes") { estadisticas = !estadisticas }
            if (estadisticas) {
                DescriptivasCuaderno("Semana actual (lunes a hoy)", p.semana)
                DescriptivasCuaderno("Mes actual (día 1 a hoy)", p.mes)
            }
            if (r.dias.isEmpty()) {
                TarjetaPapel {
                    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.mascota_diario_abierto), null, Modifier.size(100.dp))
                    Letra("Tus mareas aún están por dibujarse", 24); Letra("Guarda un momento en Inicio para comenzar.")
                }
            } else {
                // Selección editorial: más cobertura en ambos grupos, sin atribuir significación.
                val destacadas = remember(r) {
                    r.actividades.mapIndexed { i, a -> i to a }
                        .filter { (_, a) -> a.suficiente && minOf(a.diasCon, a.diasSin) >= 7 &&
                            maxOf(kotlin.math.abs(a.diferencia!!), kotlin.math.abs(a.diferenciaAnimo!!)) >= 1.0 }
                        .sortedWith(compareByDescending<Pair<Int, ActividadResumen>> { minOf(it.second.diasCon, it.second.diasSin) }
                            .thenBy { it.second.id })
                        .take(3)
                }
                TarjetaPapel(color = OndaColor.PapelRosa) {
                    Letra("Tus pistas más claras", 24)
                    Letra("Esto es lo que aparece en tus registros. Puede ser una pista para conocerte mejor, no una explicación de por qué ocurrió.", 17)
                    if (destacadas.isEmpty()) Letra("Por ahora no hay una pista suficientemente clara para destacar. Puedes seguir registrando tus días a tu ritmo.")
                }
                destacadas.forEach { (i, a) ->
                    TarjetaPapel(color = OndaColor.Papel) {
                        Letra(etiquetaActividad(a.id), 23)
                        Letra(p.pistas[i].texto)
                        Letra("${a.diasCon} días con esta actividad y ${a.diasSin} sin ella · ${a.registrosCon + a.registrosSin} momentos guardados.", 15)
                        var ver by remember { mutableStateOf(false) }
                        TextButton(onClick = { ver = !ver }) { Letra("¿Por qué veo esto?", 18) }
                        if (ver) {
                            Letra(p.pistas[i].evidencia, 16)
                            var grupoCon by remember { mutableStateOf(true) }
                            TextButton(onClick = { grupoCon = !grupoCon }) { Letra(if (grupoCon) "Ver días sin actividad" else "Ver días con actividad", 17) }
                            val fechas = r.dias.filter { (a.id in it.actividades) == grupoCon }.map { it.fecha }.toSet()
                            Letra(if (grupoCon) "Días con actividad:" else "Días sin actividad:", 17)
                            r.registros.filter { it.fechaIso in fechas }.forEach { registro ->
                                val texto = "${registro.fechaIso} · ${registro.estado.etiqueta} · energía ${registro.energia}"
                                if (demo) Letra(texto, 16)
                                else TextButton(onClick = { onAbrir(registro) }) { Letra(texto, 16) }
                            }
                        }
                    }
                }
                var verGraficas by remember { mutableStateOf(false) }
                BotonCuaderno(if (verGraficas) "Cerrar mis gráficas" else "Ver cómo fueron mis días") { verGraficas = !verGraficas }
                if (verGraficas) {
                TarjetaPapel {
                    Letra("¿Cómo han cambiado mis días?", 24)
                    var cadaDia by remember(dias) { mutableStateOf(dias <= 30) }
                    TextButton(onClick = { cadaDia = !cadaDia }) { Letra(if (cadaDia) "Ver solo el recorrido suave" else "Ver también cada día", 17) }
                    LineaPatrones(p.puntos, false, cadaDia)
                    LineaPatrones(p.puntos, true, cadaDia)
                    Letra((if (cadaDia) "Rosa/celeste: cómo estuvo cada día. " else "") + "Violeta: una mirada más suave a los últimos siete días. Los espacios vacíos son días que no registraste.", 15)
                    var tabla by remember { mutableStateOf(false) }
                    TextButton(onClick = { tabla = !tabla }) { Letra("${if (tabla) "Ocultar" else "Consultar"} valores y días de cada ventana", 16) }
                    if (tabla) p.puntos.forEach { punto ->
                        Letra("${punto.fecha}: ánimo ${numeroDiario(punto.animo)}, energía ${numeroDiario(punto.energia)}; móvil ${numeroDiario(punto.mediaAnimo)} / ${numeroDiario(punto.mediaEnergia)} (${punto.diasVentana} días disponibles).", 16)
                    }
                }
                FrecuenciasCuaderno(r)
                DispersionCuaderno(r)
                }

            }
        }
    }
    if (!demo) BotonCuaderno("Abrir mi reporte semanal", onReporte)
    if (!demo && onReporteMensual != null) BotonCuaderno("Mi mes en PDF", onReporteMensual)
}

@Composable
private fun DescriptivasCuaderno(titulo: String, p: EstadisticaPeriodo) {
    TarjetaPapel {
        Letra(titulo, 24)
        Letra("${p.resumen.inicio} — ${p.resumen.fin} · ${p.resumen.dias.size} días, ${p.resumen.registros.size} registros", 16)
        listOf("Ánimo / 8" to p.animo, "Energía / 10" to p.energia).forEach { (nombre, d) ->
            Letra(nombre, 21)
            Letra("Media ${numeroDiario(d.media)} · mediana ${numeroDiario(d.mediana)} · desviación ${numeroDiario(d.desviacion)}", 17)
            Letra("Moda(s) de medias diarias: ${d.modas.joinToString { numeroDiario(it) }.ifEmpty { "—" }}", 16)
        }
        Letra("Racha máxima: ${p.rachaMaxima} días · al cierre: ${p.rachaAlCierre} días. Un hueco corta la racha.", 17)
        Letra("Desviación poblacional de los días observados. Si todas las frecuencias empatan, no hay moda única. La escala de ánimo es ordinal; sus medias y dispersión son una aproximación descriptiva.", 15)
    }
}

@Composable
private fun LineaPatrones(puntos: List<PuntoTendencia>, energia: Boolean, cadaDia: Boolean) {
    val maximo = if (energia) 10 else 8
    val titulo = if (energia) "Energía · 1–10" else "Ánimo · 1–8"
    // El celeste oscuro ya usado por la gráfica anterior conserva contraste sobre papel.
    val color = if (energia) Color(0xFF4C989E) else OndaColor.Rosa
    val descripcion = puntos.joinToString { "${it.fecha}: ${numeroDiario(if (energia) it.energia else it.animo)}, móvil ${numeroDiario(if (energia) it.mediaEnergia else it.mediaAnimo)}, n=${it.diasVentana} días" }
    Letra(titulo, 19)
    Row {
        Column(Modifier.height(130.dp).width(24.dp), verticalArrangement = Arrangement.SpaceBetween) { Letra("$maximo", 13); Letra("1", 13) }
        Canvas(Modifier.weight(1f).height(130.dp).semantics { contentDescription = "$titulo. $descripcion" }) {
            val margen = 6.dp.toPx()
            fun punto(i: Int, v: Double) = Offset(margen + (size.width - 2 * margen) * i / (puntos.size - 1).coerceAtLeast(1),
                margen + (size.height - 2 * margen) * (1 - (v - 1) / (maximo - 1)).toFloat())
            listOf(1.0, (maximo + 1) / 2.0, maximo.toDouble()).forEach { v ->
                drawLine(OndaColor.Linea.copy(alpha = .25f), punto(0, v), punto(puntos.lastIndex, v))
            }
            (if (cadaDia) listOf(false, true) else listOf(true)).forEach { movil ->
                fun valor(p: PuntoTendencia) = if (energia) { if (movil) p.mediaEnergia else p.energia } else { if (movil) p.mediaAnimo else p.animo }
                puntos.forEachIndexed { i, p -> valor(p)?.let { v ->
                    val c = if (movil) OndaColor.Tinta else color
                    if (i > 0) valor(puntos[i - 1])?.let { previo -> drawLine(c, punto(i - 1, previo), punto(i, v), (if (movil) 1.5.dp else 2.5.dp).toPx()) }
                    drawCircle(c, (if (movil) 1.8.dp else 3.dp).toPx(), punto(i, v))
                } }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Letra(fechaBonita(puntos.first().fecha), 14); Letra(fechaBonita(puntos.last().fecha), 14) }
}

@Composable
private fun DispersionCuaderno(r: ResumenDiario) {
    TarjetaPapel(color = OndaColor.PapelCeleste) {
        Letra("¿Cómo coinciden ánimo y energía?", 24)
        Letra("Cada punto es un día. n=${r.dias.size} días, ${r.registros.size} registros. Los puntos iguales se superponen.", 16)
        val descripcion = r.dias.joinToString { "${it.fecha}: ánimo ${numeroDiario(it.animo)}, energía ${numeroDiario(it.energia)}" }
        Letra("Energía ↑ 1–10", 16)
        Row {
        Column(Modifier.height(150.dp).width(24.dp), verticalArrangement = Arrangement.SpaceBetween) { Letra("10", 13); Letra("1", 13) }
        Canvas(Modifier.weight(1f).height(150.dp).semantics { contentDescription = "Dispersión. $descripcion" }) {
            val m = 8.dp.toPx()
            fun xy(a: Double, e: Double) = Offset(m + (size.width - 2*m) * ((a-1)/7).toFloat(), m + (size.height - 2*m) * (1-(e-1)/9).toFloat())
            drawLine(OndaColor.Tinta, xy(1.0, 1.0), xy(8.0, 1.0))
            drawLine(OndaColor.Tinta, xy(1.0, 1.0), xy(1.0, 10.0))
            r.dias.forEach { drawCircle(OndaColor.TintaSuave, 4.dp.toPx(), xy(it.animo, it.energia)) }
        }
        }
        Row(Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Letra("1", 16); Letra("Ánimo →", 16); Letra("8", 16)
        }
        Letra("Ubica coincidencias; esta gráfica no establece causas.", 15)
    }
}
