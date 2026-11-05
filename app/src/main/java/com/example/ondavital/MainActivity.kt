package com.example.ondavital

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.ondavital.seguridad.SeguridadLocal
import com.example.ondavital.seguridad.SesionAcceso
import com.example.ondavital.ui.AccesoOndaVital
import com.example.ondavital.ui.ArranqueOndaVital
import com.example.ondavital.seguridad.AccesoBiometrico
import android.os.Build
import com.example.ondavital.ui.theme.OndaVitalTheme

/** La sesión vive solo en memoria. Un proceso nuevo siempre vuelve a pedir el PIN. */
class MainActivity : FragmentActivity() {
    private lateinit var seguridad: SeguridadLocal
    private lateinit var sesion: SesionAcceso
    lateinit var accesoBiometrico: AccesoBiometrico
        private set

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31) splashScreen.setOnExitAnimationListener { it.remove() }
        seguridad = SeguridadLocal(applicationContext)
        sesion = ViewModelProvider(this)[SesionAcceso::class.java]
        accesoBiometrico = AccesoBiometrico(this, seguridad, sesion)
        accesoBiometrico.reanudarPendiente()
        if (!seguridad.hayPin) sesion.abierto = true
        actualizarCapturas()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            OndaVitalTheme {
                ArranqueOndaVital(this, sesion) {
                    AccesoOndaVital(this, seguridad, sesion, ::actualizarCapturas)
                }
            }
        }
    }

    fun actualizarCapturas() {
        // Entrega de demostración: permitir grabar el video. El PIN sigue controlando el acceso.
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onStart() {
        super.onStart()
        if (::sesion.isInitialized && ::seguridad.isInitialized && seguridad.hayPin &&
            sesion.ultimoFondo > 0L && SystemClock.elapsedRealtime() - sesion.ultimoFondo >= 120_000L
        ) sesion.abierto = false
        if (::sesion.isInitialized) sesion.ultimoFondo = 0L
    }

    override fun onStop() {
        if (::sesion.isInitialized) sesion.ultimoFondo = SystemClock.elapsedRealtime()
        super.onStop()
    }
}
