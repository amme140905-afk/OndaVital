package com.example.ondavital.data

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.ondavital.R
import com.example.ondavital.analisis.*
import java.io.OutputStream

/** Dos páginas A4 de agregados; nunca incorpora el texto de las notas. */
fun escribirReporteMensualPdf(context: Context, salida: OutputStream, estadistica: EstadisticaPeriodo, nombre: String) {
    val r = estadistica.resumen
    val documento = PdfDocument()
    val fondo = requireNotNull(BitmapFactory.decodeResource(context.resources, R.drawable.papel_reporte))
    val fuente = ResourcesCompat.getFont(context, R.font.patrick_hand) ?: Typeface.DEFAULT
    val tinta = Color.rgb(63, 54, 91)
    val violeta = Color.rgb(109, 81, 150)
    val rosa = Color.rgb(181, 93, 143)
    val celeste = Color.rgb(51, 126, 137)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var abierta: PdfDocument.Page? = null
    fun texto(c: Canvas, s: String, x: Float, y: Float, tam: Float = 13f, color: Int = tinta) {
        p.color = color; p.textSize = tam; p.typeface = fuente; p.style = Paint.Style.FILL
        c.drawText(s, x, y, p)
    }
    fun parrafo(c: Canvas, s: String, inicio: Float, tam: Float = 13f): Float {
        var resto = s; var y = inicio
        p.textSize = tam; p.typeface = fuente
        while (resto.isNotEmpty()) {
            var n = p.breakText(resto, true, 507f, null).coerceAtLeast(1)
            if (n < resto.length) resto.lastIndexOf(' ', n - 1).takeIf { it > 0 }?.let { n = it }
            texto(c, resto.take(n), 44f, y, tam)
            resto = resto.drop(n).trimStart(); y += tam + 4
        }
        return y + 5
    }
    fun nueva(numero: Int): Canvas {
        abierta = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, numero).create())
        val c = abierta!!.canvas
        c.drawBitmap(fondo, null, RectF(0f, 0f, 595f, 842f), null)
        texto(c, "Un mes en tu diario", 44f, 66f, 27f)
        texto(c, "${fechaBonita(r.inicio)} — ${fechaBonita(r.fin)}", 44f, 91f, 15f)
        texto(c, "${r.dias.size} días registrados · ${r.registros.size} momentos", 44f, 112f, 14f)
        texto(c, "OndaVital · $numero / 2", 44f, 796f, 11f)
        return c
    }
    fun cerrar() { abierta?.let { documento.finishPage(it) }; abierta = null }
    fun linea(c: Canvas, energia: Boolean, arriba: Float) {
        val max = if (energia) 10 else 8
        val puntos = tendencia(r)
        val izq = 75f; val ancho = 470f; val alto = 105f
        texto(c, if (energia) "Tu energía · 1–10" else "Tu ánimo · 1–8", 44f, arriba, 18f)
        fun x(i: Int) = izq + ancho * i / (puntos.size - 1).coerceAtLeast(1)
        fun y(v: Double) = arriba + 20 + alto * (1 - (v - 1) / (max - 1)).toFloat()
        p.strokeWidth = .5f; p.color = Color.rgb(219, 207, 230)
        for (v in listOf(1.0, (max + 1) / 2.0, max.toDouble())) c.drawLine(izq, y(v), izq + ancho, y(v), p)
        texto(c, "$max", 49f, arriba + 25, 11f); texto(c, "1", 49f, arriba + 126, 11f)
        for (movil in listOf(false, true)) {
            fun valor(t: PuntoTendencia): Double? = if (energia) { if (movil) t.mediaEnergia else t.energia } else { if (movil) t.mediaAnimo else t.animo }
            puntos.forEachIndexed { i, t -> valor(t)?.let { v ->
                p.color = if (movil) violeta else if (energia) celeste else rosa
                p.strokeWidth = if (movil) 1.8f else 1f
                if (i > 0) valor(puntos[i - 1])?.let { previo -> c.drawLine(x(i - 1), y(previo), x(i), y(v), p) }
                c.drawCircle(x(i), y(v), if (movil) 1.6f else 2.2f, p)
            } }
        }
        texto(c, r.inicio, izq, arriba + 147, 11f); texto(c, r.fin, 477f, arriba + 147, 11f)
    }
    try {
        var c = nueva(1)
        var y = 145f
        if (nombre.isNotBlank()) y = parrafo(c, "El diario de $nombre", y, 17f)
        texto(c, "Ánimo: media ${numeroDiario(estadistica.animo.media)}, mediana ${numeroDiario(estadistica.animo.mediana)}, desviación ${numeroDiario(estadistica.animo.desviacion)} / 8", 44f, y)
        texto(c, "Energía: media ${numeroDiario(estadistica.energia.media)}, mediana ${numeroDiario(estadistica.energia.mediana)}, desviación ${numeroDiario(estadistica.energia.desviacion)} / 10", 44f, y + 20)
        texto(c, "Racha más larga del mes: ${estadistica.rachaMaxima} días", 44f, y + 40)
        fun modas(d: Descriptiva) = when {
            d.modas.isEmpty() -> "sin datos"
            d.modas.size > 3 -> "${d.modas.size} valores empatados"
            else -> d.modas.joinToString(", ") { numeroDiario(it) }
        }
        parrafo(c, "Modas diarias: ánimo ${modas(estadistica.animo)}; energía ${modas(estadistica.energia)}.", y + 58, 11f)
        if (r.dias.isEmpty()) {
            parrafo(c, "Este mes todavía no tiene recuerdos guardados. Los espacios vacíos no se cuentan como cero.", 270f, 20f)
        } else {
            linea(c, false, 250f); linea(c, true, 430f)
        }
        if (r.dias.isNotEmpty()) parrafo(c, "Rosa/celeste: cada día. Violeta: recorrido suavizado de hasta siete fechas. Los huecos cortan las líneas. Cada día registrado pesa lo mismo.", 622f)
        parrafo(c, "El ánimo es una escala descriptiva y ordinal; las medias y la dispersión son aproximaciones, no una evaluación clínica.", 670f, 12f)
        cerrar()
        c = nueva(2)
        texto(c, "Lo que estuvo presente", 44f, 154f, 22f)
        if (r.actividades.isNotEmpty()) texto(c, "Energía de 0 a 10 · violeta: días con actividad · celeste: sin ella", 44f, 180f, 12f)
        val actividades = r.actividades.take(6)
        actividades.forEachIndexed { i, a ->
            val arriba = 208f + i * 56
            texto(c, etiquetaActividad(a.id).take(24), 44f, arriba + 10, 12f)
            p.color = violeta; c.drawRect(210f, arriba, 210f + 32f * a.energiaCon.toFloat(), arriba + 9, p)
            a.energiaSin?.let { p.color = celeste; c.drawRect(210f, arriba + 12, 210f + 32f * it.toFloat(), arriba + 21, p) }
            texto(c, "Con: ${a.diasCon} días (${a.registrosCon} registros) · Sin: ${a.diasSin} días (${a.registrosSin} registros)", 44f, arriba + 38, 11f)
        }
        if (actividades.isNotEmpty()) {
            for (v in listOf(0, 5, 10)) texto(c, "$v", 210f + 32f * v, 551f, 11f)
        }
        y = if (actividades.isEmpty()) 220f else 584f
        val pistas = r.actividades.filter { it.suficiente }.take(2)
        if (pistas.isEmpty()) y = parrafo(c, if (r.dias.isEmpty()) "Sin registros este mes." else "Aún no hay datos suficientes para destacar una asociación: hacen falta 14 días registrados y cinco con y cinco sin la actividad.", y)
        else pistas.forEach { a -> y = parrafo(c, explicarActividad(r, a).texto, y, 12f) }
        parrafo(c, "Las actividades pueden coincidir con otros factores. Explorar muchas comparaciones puede destacar coincidencias por azar. Estas diferencias no demuestran causas.", y.coerceAtMost(690f), 11f)
        cerrar()
        documento.writeTo(salida)
    } finally { cerrar(); documento.close(); fondo.recycle() }
}
