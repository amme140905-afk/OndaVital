package com.example.ondavital

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ondavital.analisis.*
import com.example.ondavital.data.escribirReporteMensualPdf
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ReporteMensualTest {
    @Test fun exportaMesVacioYDensoSinNotas() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val datos = generarDiarioEjemplo("2026-10-31").flatMap { r -> List(3) { i -> r.copy(horaRegistroEpochMs = r.horaRegistroEpochMs + i * 1000, nota = "NOTA_PRIVADA_NO_INCLUIR") } }
        for ((nombre, registros) in listOf("mensual-ejemplo.pdf" to datos, "mensual-vacio.pdf" to emptyList())) {
            val resumen = estadisticaPeriodo(registros, "2026-10-01", "2026-10-31")
            if (registros.isNotEmpty()) { assertEquals(31, resumen.resumen.dias.size); assertEquals(93, resumen.resumen.registros.size) }
            val f = File(c.getExternalFilesDir(null), nombre)
            f.outputStream().use { escribirReporteMensualPdf(c, it, resumen, "Demostración") }
            assertTrue(f.length() > 1000)
            ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { pdf -> assertEquals(2, pdf.pageCount) }
            }
        }
    }
}
