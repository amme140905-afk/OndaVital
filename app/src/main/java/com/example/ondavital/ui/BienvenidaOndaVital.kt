package com.example.ondavital.ui

import android.animation.ValueAnimator
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.fragment.app.FragmentActivity
import com.example.ondavital.R
import com.example.ondavital.seguridad.SesionAcceso
import com.example.ondavital.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ArranqueOndaVital(activity: FragmentActivity, sesion: SesionAcceso, destino: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val animaciones = remember {
        if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
        else Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    if (sesion.bienvenidaTerminada) {
        destino()
    } else {
        DisposableEffect(activity) {
            val controlador = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controlador.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controlador.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { controlador.show(WindowInsetsCompat.Type.systemBars()) }
        }
        BackHandler { activity.finish() }
        val entrada = remember { Animatable(if (animaciones) 0f else 1f) }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            if (sesion.inicioBienvenida == 0L) sesion.inicioBienvenida = SystemClock.uptimeMillis()
            if (animaciones) {
                entrada.animateTo(1f, tween(220))
                delay((1750L - (SystemClock.uptimeMillis() - sesion.inicioBienvenida)).coerceAtLeast(0L))
                entrada.animateTo(0f, tween(250))
            }
            sesion.bienvenidaTerminada = true
        }
        BienvenidaIlustrada(Modifier.graphicsLayer { alpha = entrada.value })
    }
}

@Composable
fun BienvenidaIlustrada(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.bienvenida_cuento),
        "La lunita lee su diario bajo un cielo de estrellas",
        modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.Center)
}
