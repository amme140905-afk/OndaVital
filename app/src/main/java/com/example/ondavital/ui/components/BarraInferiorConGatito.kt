package com.example.ondavital.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.ondavital.R

/** Solo la barra ocupa espacio: el gatito se dibuja encima del contenido desplazable. */
@Composable
fun BarraInferiorConGatito(modifier: Modifier = Modifier, barra: @Composable () -> Unit) {
    Layout(modifier = modifier.fillMaxWidth(), content = {
        Image(painterResource(R.drawable.gatito_barra), contentDescription = null,
            contentScale = ContentScale.Fit)
        Box(Modifier.fillMaxWidth()) { barra() }
    }) { elementos, limites ->
        val menu = elementos[1].measure(limites.copy(minHeight = 0))
        val gato = elementos[0].measure(Constraints.fixed(72.dp.roundToPx(), 56.dp.roundToPx()))
        layout(menu.width, menu.height) {
            gato.placeRelative((menu.width - gato.width - 28.dp.roundToPx()).coerceAtLeast(0), -50.dp.roundToPx())
            // La barra se dibuja después y tapa solo la base de las patitas.
            menu.placeRelative(0, 0)
        }
    }
}
