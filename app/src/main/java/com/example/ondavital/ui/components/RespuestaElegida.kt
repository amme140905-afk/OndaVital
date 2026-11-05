package com.example.ondavital.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Respuesta breve al cambio de selección, sin bucles; respeta MotionDurationScale. */
@Composable
fun Modifier.respuestaElegida(elegido: Boolean): Modifier {
    val escala by animateFloatAsState(if (elegido) 1.025f else 1f, tween(130), label = "seleccion")
    return graphicsLayer { scaleX = escala; scaleY = escala }
}
