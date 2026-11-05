package com.example.ondavital.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ondavital.R
import com.example.ondavital.analisis.*
import com.example.ondavital.model.RegistroDiario
import com.example.ondavital.ui.components.TarjetaPapel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PantallaReporteMensual(registros: List<RegistroDiario>, mes: String, onMes: (String) -> Unit,
                          onVolver: () -> Unit, onPdf: () -> Unit) {
    TextButton(onClick = onVolver) { Letra("‹  Volver a Análisis", 18) }
    CabeceraCuaderno("Un mes en tu diario", "Tus recuerdos, reunidos en papel.", R.drawable.mascota_lee)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { onMes(moverMes(mes, -1)) }) { Letra("‹ Mes anterior", 18) }
        TextButton(onClick = { onMes(moverMes(mes, 1)) }, enabled = mes < hoyDiario().take(7)) { Letra("Mes siguiente ›", 18) }
    }
    Letra("${fechaBonita("$mes-01")} — ${fechaBonita(finMes(mes))}", 22)
    key(registros, mes) {
        val estado by produceState<EstadisticaPeriodo?>(null) {
            value = withContext(Dispatchers.Default) { estadisticaPeriodo(registros, "$mes-01", finMes(mes)) }
        }
        val datos = estado
        if (datos == null) Letra("Reuniendo tus páginas…")
        else {
            TarjetaPapel {
                Letra("${datos.resumen.dias.size} días · ${datos.resumen.registros.size} momentos", 25)
                Letra(if (datos.resumen.dias.isEmpty()) "Este mes aún no tiene recuerdos. También puedes guardar una página vacía de tu historia."
                    else "El PDF incluye tu recorrido de ánimo y energía, actividades y pequeñas pistas.")
                Letra("Tus notas completas no se incluyen. Las comparaciones solo aparecen cuando hay días suficientes.", 16)
            }
            BotonCuaderno("Guardar mi mes en PDF", onPdf)
        }
    }
}
