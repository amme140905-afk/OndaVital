package com.example.ondavital.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.ondavital.R
import com.example.ondavital.analisis.*
import java.io.OutputStream

/** Exporta únicamente datos agregados; nunca incorpora las notas personales. */
fun escribirReporteSemanalPdf(context: Context, salida: OutputStream, actual: ResumenDiario,
                              anterior: ResumenDiario, nombre: String) {
    val documento = PdfDocument()
    val lapiz = Paint(Paint.ANTI_ALIAS_FLAG)
    val imagen = BitmapFactory.decodeResource(context.resources, R.drawable.mascota_lee)
    var pagina: PdfDocument.Page? = null
    var numero = 0
    var y = 0f
    fun cerrarPagina() {
        pagina?.let { p ->
            lapiz.color = Color.rgb(108, 94, 129); lapiz.textSize = 10f; lapiz.typeface = Typeface.DEFAULT
            p.canvas.drawText("OndaVital  ·  Tu pequeño diario", 42f, 811f, lapiz)
            p.canvas.drawText(numero.toString(), 540f, 811f, lapiz)
            documento.finishPage(p)
        }
        pagina = null
    }
    fun nuevaPagina() {
        cerrarPagina()
        numero++
        pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, numero).create())
        val lienzo: Canvas = pagina!!.canvas
        lienzo.drawColor(Color.rgb(255, 250, 242))
        lapiz.color = Color.rgb(223, 207, 239)
        lienzo.drawRoundRect(RectF(28f, 24f, 567f, 120f), 18f, 18f, lapiz)
        lapiz.color = Color.rgb(63, 54, 91); lapiz.textSize = 24f; lapiz.typeface = Typeface.DEFAULT_BOLD
        lienzo.drawText("Una semana en tu diario", 42f, 65f, lapiz)
        lapiz.textSize = 11f; lapiz.typeface = Typeface.DEFAULT
        lienzo.drawText("${fechaBonita(actual.inicio)} — ${fechaBonita(actual.fin)}", 43f, 92f, lapiz)
        if (imagen != null) lienzo.drawBitmap(imagen, null, RectF(469f, 31f, 556f, 114f), null)
        y = 148f
    }
    fun espacio(alto: Float) { if (y + alto > 775f) nuevaPagina() }
    fun parrafo(texto: String, titulo: Boolean = false) {
        val tamano = if (titulo) 17f else 12f
        val alto = if (titulo) 24f else 18f
        // Rompe también palabras muy largas para evitar desbordes en actividades propias.
        var resto = texto
        while (resto.isNotEmpty()) {
            espacio(alto)
            lapiz.textSize = tamano
            lapiz.typeface = if (titulo) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            lapiz.color = Color.rgb(63, 54, 91)
            var cantidad = lapiz.breakText(resto, true, 507f, null).coerceAtLeast(1)
            if (cantidad < resto.length) {
                val corte = resto.lastIndexOf(' ', cantidad - 1)
                if (corte > 0) cantidad = corte
            }
            pagina!!.canvas.drawText(resto.substring(0, cantidad), 44f, y, lapiz)
            resto = resto.substring(cantidad).trimStart()
            y += alto
        }
        y += if (titulo) 3f else 7f
    }
    try {
        nuevaPagina()
        if (nombre.isNotBlank()) parrafo("El diario de $nombre", true)
        if (actual.fin >= hoyDiario() && actual.inicio <= hoyDiario()) parrafo("Semana en curso: resultados hasta hoy.")
        parrafo("${actual.dias.size} de 7 días registrados · ${actual.registros.size} momentos", true)
        parrafo("Estado medio: ${numeroDiario(actual.animo)} / 8     Energía media: ${numeroDiario(actual.energia)} / 10")
        parrafo("Cada día registrado pesa lo mismo. Los días sin registro no cuentan como cero.")
        parrafo("Tus días, uno a uno", true)
        repeat(7) { i ->
            val fecha = moverDia(actual.inicio, i)
            val dia = actual.dias.firstOrNull { it.fecha == fecha }
            parrafo(if (dia == null) "${fechaBonita(fecha)}: sin registro"
            else "${fechaBonita(fecha)}: estado ${numeroDiario(dia.animo)}/8 · energía ${numeroDiario(dia.energia)}/10 · ${dia.registros} registros")
        }
        parrafo("Los estados que aparecieron", true)
        if (actual.frecuencias.isEmpty()) parrafo("Sin registros esta semana.")
        actual.frecuencias.forEach { (estado, cantidad) -> parrafo("${estado.etiqueta}: $cantidad de ${actual.registros.size} registros") }
        parrafo("De una semana a otra", true)
        if (actual.dias.size >= 3 && anterior.dias.size >= 3) {
            parrafo("Energía anterior: ${numeroDiario(anterior.energia)}/10. Esta semana: ${numeroDiario(actual.energia)}/10.")
            parrafo("Estado anterior: ${numeroDiario(anterior.animo)}/8. Esta semana: ${numeroDiario(actual.animo)}/8.")
            parrafo("Base: ${anterior.dias.size} días la semana anterior y ${actual.dias.size} esta semana.")
        } else parrafo("Se necesitan al menos 3 días registrados en cada semana para comparar. Anterior: ${anterior.dias.size}; actual: ${actual.dias.size}.")
        parrafo("Actividades presentes", true)
        if (actual.actividades.isEmpty()) parrafo("Sin actividades seleccionadas.")
        actual.actividades.forEach { parrafo("${etiquetaActividad(it.id)}: ${it.diasCon} días") }
        parrafo("Cómo leer este resumen", true)
        parrafo("La escala de estado 1–8 es descriptiva. Las frecuencias conservan el significado de cada emoción. Las coincidencias no demuestran causalidad.")
        parrafo("Este PDF contiene solo el resumen y las actividades; no incluye notas personales.")
        cerrarPagina()
        documento.writeTo(salida)
    } finally {
        // Si falló el dibujo, cerrar primero cualquier página aún abierta.
        if (pagina != null) cerrarPagina()
        documento.close()
        imagen?.recycle()
    }
}
