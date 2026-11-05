package com.example.ondavital.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ondavital.R
import com.example.ondavital.ui.theme.OndaColor
import com.example.ondavital.ui.theme.LetraDiario
import kotlin.math.*

/** Contorno con varias pasadas de lápiz; se recalcula solo cuando cambia el tamaño. */
fun Modifier.bordeLapiz(
    tinta: Color = OndaColor.Linea,
    fondo: Color = Color.White.copy(alpha = .64f),
    redondo: Boolean = false
): Modifier = drawWithCache {
    val inset = 3.dp.toPx()
    val w = size.width - 2 * inset
    val h = size.height - 2 * inset
    val radio = min(if (redondo) h / 2 else 15.dp.toPx(), min(w, h) / 2)
    val puntos = mutableListOf<Offset>()
    val centros = listOf(
        Offset(inset + w - radio, inset + radio),
        Offset(inset + w - radio, inset + h - radio),
        Offset(inset + radio, inset + h - radio),
        Offset(inset + radio, inset + radio)
    )
    centros.forEachIndexed { esquina, centro ->
        for (paso in 0..20) {
            val a = (-90 + esquina * 90 + paso * 4.5) * PI / 180
            puntos += Offset(centro.x + radio * cos(a).toFloat(), centro.y + radio * sin(a).toFloat())
        }
    }
    val trazos = (0..2).map { capa ->
        Path().apply {
            puntos.forEachIndexed { i, p ->
                val ruido = sin(i * 1.71 + capa * 2.4).toFloat() * .55.dp.toPx()
                val x = p.x + ruido + capa * .25.dp.toPx()
                val y = p.y + cos(i * 1.21 + capa).toFloat() * .5.dp.toPx()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
    onDrawBehind {
        drawPath(trazos[0], fondo)
        trazos.forEachIndexed { index, path ->
            drawPath(path, tinta.copy(alpha = if (index == 0) .40f else .22f),
                style = Stroke(width = if (index == 0) 2.dp.toPx() else .8.dp.toPx()))
        }
    }
}

@Composable
fun FondoPapel(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.fondo_cuento), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        content()
    }
}

@Composable
fun TarjetaPapel(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = .84f),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().bordeLapiz(fondo = color).padding(12.dp), content = content)
}

@Composable
fun BotonGuardar(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp)
            .bordeLapiz(
                tinta = if (enabled) Color(0xFFE7A06C) else OndaColor.Linea,
                fondo = if (enabled) Color(0xFFFFE6A0) else Color(0xFFF0E9F4),
                redondo = true
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Guardar en mi diario", color = if (enabled) OndaColor.Tinta else OndaColor.TintaSuave,
            fontFamily = LetraDiario, fontSize = 23.sp)
    }
}

/** Diez destellos seleccionables con los gestos y accesibilidad del Slider. */
@Composable
fun EnergiaEstrellas(valor: Int, onValorChange: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            (1..10).forEach { paso ->
                Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.estrella_doodle),
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (paso == valor) 33.dp else 26.dp)
                            .alpha(if (paso <= valor) 1f else .26f)
                    )
                }
            }
        }
        Slider(
            value = if (valor == 0) 1f else valor.toFloat(),
            onValueChange = { onValorChange(it.roundToInt().coerceIn(1, 10)) },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "Nivel de energía: ${if (valor == 0) "sin elegir" else "$valor de 10"}"
            },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

/** Atlas original sin editar: Compose toma la región correspondiente al dibujar. */
@Composable
fun EmocionIlustrada(indice: Int, modifier: Modifier = Modifier) {
    val atlas = ImageBitmap.imageResource(R.drawable.emociones_crayon)
    val regiones = listOf(
        intArrayOf(0, 300, 327, 340), intArrayOf(327, 353, 312, 280),
        intArrayOf(639, 351, 294, 280), intArrayOf(935, 307, 319, 328),
        intArrayOf(22, 648, 298, 296), intArrayOf(331, 647, 293, 301),
        intArrayOf(639, 649, 284, 295), intArrayOf(925, 639, 329, 337)
    )
    val r = regiones[indice.coerceIn(0, 7)]
    Canvas(modifier) {
        val factor = min(size.width / r[2], size.height / r[3])
        val ancho = (r[2] * factor).roundToInt()
        val alto = (r[3] * factor).roundToInt()
        drawImage(atlas, srcOffset = IntOffset(r[0], r[1]), srcSize = IntSize(r[2], r[3]),
            dstOffset = IntOffset(((size.width-ancho)/2).roundToInt(), ((size.height-alto)/2).roundToInt()),
            dstSize = IntSize(ancho, alto), filterQuality = FilterQuality.Medium)
    }
}

@Composable
fun NeutralIlustrado(modifier: Modifier = Modifier) {
    val imagen = ImageBitmap.imageResource(R.drawable.estado_neutral)
    Canvas(modifier) {
        drawImage(
            imagen,
            srcOffset = IntOffset(278, 322),
            srcSize = IntSize(700, 640),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium
        )
    }
}
