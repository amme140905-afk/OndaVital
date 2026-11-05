package com.example.ondavital.ui

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.withResumed
import com.example.ondavital.R
import com.example.ondavital.MainActivity
import com.example.ondavital.seguridad.AccesoBiometrico
import android.content.Intent
import android.provider.Settings
import android.os.Build
import com.example.ondavital.seguridad.SeguridadLocal
import com.example.ondavital.seguridad.SesionAcceso
import com.example.ondavital.ui.theme.LetraDiario
import com.example.ondavital.ui.theme.OndaColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** No se compone el diario ni se lee su JSON mientras esté cerrada la sesión. */
@Composable
fun AccesoOndaVital(activity: MainActivity, seguridad: SeguridadLocal,
                   sesion: SesionAcceso, actualizarCapturas: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    var revisionSensor by remember { mutableIntStateOf(0) }
    var modo by remember { mutableStateOf<String?>(null) }
    val estadoApp = rememberSaveableStateHolder()
    DisposableEffect(activity) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) revisionSensor++
        }
        activity.lifecycle.addObserver(observador)
        onDispose { activity.lifecycle.removeObserver(observador) }
    }
    val pinActivo = remember(revision) { seguridad.hayPin }
    val bioActiva = remember(revision) { seguridad.biometria }
    val codigoBio = remember(activity, revisionSensor, revision) {
        BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }
    val bioDisponible = codigoBio == BiometricManager.BIOMETRIC_SUCCESS
    LaunchedEffect(sesion.biometriaActivada) {
        if (sesion.biometriaActivada) {
            sesion.biometriaActivada = false
            sesion.mensajeBiometrico = ""
            revision++
            modo = null
        }
    }
    SideEffect {
        actualizarCapturas()
    }
    if (!sesion.abierto && pinActivo) {
        PantallaDesbloqueo(seguridad, sesion, activity, bioActiva)
    } else {
        estadoApp.SaveableStateProvider("diario") {
            OndaVitalApp(pinActivo, bioActiva, bioDisponible,
                onSeguridad = { solicitado ->
                    sesion.mensajeBiometrico = ""
                    when (solicitado) {
                        "revisar_bio" -> revisionSensor++
                        "configurar_huella" -> activity.startActivity(if (Build.VERSION.SDK_INT >= 30)
                            Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            else Intent(Settings.ACTION_SECURITY_SETTINGS))
                        "bloquear" -> sesion.abierto = false
                        else -> modo = solicitado
                    }
                },
                estadoBiometria = AccesoBiometrico.mensajeDisponibilidad(codigoBio),
                onDatosBorrados = { revision++; sesion.abierto = true; sesion.solicitudBiometrica = null; sesion.mensajeBiometrico = ""; sesion.biometriaActivada = false; modo = null; actualizarCapturas() })
        }
        if (modo != null) {
            Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false, securePolicy = SecureFlagPolicy.Inherit)) {
                key(modo) {
                    ConfigurarAcceso(seguridad, modo!!, mensajeSensor = sesion.mensajeBiometrico, limpiarMensajeSensor = { sesion.mensajeBiometrico = "" }, onActivarBiometria = { activity.accesoBiometrico.solicitar(activar = true) }, onCancelar = { modo = null }, onTerminar = {
                        revision++
                        modo = null
                    })
                }
            }
        }
    }
}

@Composable
private fun PantallaDesbloqueo(seguridad: SeguridadLocal, sesion: SesionAcceso,
                               activity: MainActivity, mostrarBio: Boolean) {
    val scope = rememberCoroutineScope()
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    BackHandler { activity.finish() }
    val abrirBiometria: () -> Unit = { activity.accesoBiometrico.solicitar() }
    LaunchedEffect(mostrarBio) {
        if (mostrarBio) activity.lifecycle.withResumed { abrirBiometria() }
    }
    VistaPin("Hola, qué bonito verte", "Tu diario te espera", mensaje.ifBlank { sesion.mensajeBiometrico }, procesando,
        onPin = { pin ->
            procesando = true
            scope.launch {
                try {
                    val correcto = withContext(Dispatchers.IO) { seguridad.verificar(pin) }
                    if (correcto) sesion.abierto = true
                    else mensaje = seguridad.esperaRestante().let { espera ->
                        if (espera > 0) "Espera ${(espera + 999) / 1000} s antes de volver a intentar."
                        else "Ese PIN no coincide. Inténtalo de nuevo."
                    }
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { mensaje = "No pudimos comprobar el PIN. Inténtalo otra vez."
                } finally { procesando = false }
            }
        }, onBio = if (mostrarBio) abrirBiometria else null)
}

@Composable
private fun ConfigurarAcceso(seguridad: SeguridadLocal, modo: String,
                            mensajeSensor: String, limpiarMensajeSensor: () -> Unit, onActivarBiometria: () -> Unit,
                            onCancelar: () -> Unit, onTerminar: () -> Unit) {
    val scope = rememberCoroutineScope()
    var paso by remember { mutableIntStateOf(0) }
    var actual by remember { mutableStateOf("") }
    var nuevo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    val requiereActual = modo != "crear"
    val titulo = when {
        requiereActual && paso == 0 -> "Tu llave de ahora"
        modo == "crear" && paso == 0 -> "Elige tu PIN"
        paso == 1 && requiereActual -> "Un PIN nuevo para ti"
        else -> "Repitamos esos números"
    }
    val subtitulo = when (modo) {
        "quitar" -> "Escribe tu PIN para quitarlo"
        "activar_bio" -> "Confirma tu PIN para activar biometría"
        "desactivar_bio" -> "Confirma tu PIN para desactivar biometría"
        else -> "Cuatro números que puedas recordar"
    }
    BackHandler(onBack = onCancelar)
    VistaPin(titulo, subtitulo, mensajeSensor.ifBlank { mensaje }, procesando, onVolver = onCancelar, onPin = { pin ->
        limpiarMensajeSensor()
        when {
            requiereActual && paso == 0 -> {
                procesando = true
                scope.launch {
                    try {
                        val valido = withContext(Dispatchers.IO) { seguridad.verificar(pin) }
                        if (valido) {
                            if (modo == "activar_bio") {
                                mensaje = "Confirma tu huella en la ventana de Android."
                                onActivarBiometria()
                            } else if (modo == "quitar" || modo == "desactivar_bio") {
                                val hecho = withContext(Dispatchers.IO) {
                                    if (modo == "quitar") seguridad.quitar(pin)
                                    else seguridad.guardarBiometria(modo == "activar_bio")
                                }
                                if (hecho) onTerminar() else mensaje = "No se pudo guardar el cambio."
                            } else {
                                actual = pin
                                paso = 1
                                mensaje = ""
                            }
                        } else mensaje = seguridad.esperaRestante().let { espera ->
                            if (espera > 0) "Espera ${(espera + 999) / 1000} s para intentar de nuevo."
                            else "Ese PIN no coincide."
                        }
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) { mensaje = "No se pudo comprobar. Inténtalo otra vez."
                    } finally { procesando = false }
                }
            }
            (modo == "crear" && paso == 0) || (modo == "cambiar" && paso == 1) -> {
                nuevo = pin
                paso++
                mensaje = ""
            }
            else -> {
                if (pin != nuevo) {
                    nuevo = ""
                    mensaje = "No coincidieron. Elige el PIN otra vez."
                    paso = if (modo == "crear") 0 else 1
                } else {
                    procesando = true
                    scope.launch {
                        try {
                            val hecho = withContext(Dispatchers.IO) {
                                if (modo == "crear") seguridad.crear(pin) else seguridad.cambiar(actual, pin)
                            }
                            if (hecho) onTerminar() else {
                                mensaje = "No se pudo guardar el PIN. Elige uno otra vez."
                                paso = if (modo == "crear") 0 else 1
                            }
                        } catch (e: CancellationException) { throw e
                        } catch (_: Exception) {
                            mensaje = "No se pudo guardar el PIN. Elige uno otra vez."
                            paso = if (modo == "crear") 0 else 1
                        } finally { actual = ""; nuevo = ""; procesando = false }
                    }
                }
            }
        }
    })
}

@Composable
private fun VistaPin(titulo: String, subtitulo: String, mensaje: String, procesando: Boolean,
                     onPin: (String) -> Unit, onBio: (() -> Unit)? = null,
                     onVolver: (() -> Unit)? = null) {
    var entrada by remember { mutableStateOf("") }
    val tinta = OndaColor.Tinta
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.fondo_acceso), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (onVolver != null) {
                TextButton(onClick = onVolver, modifier = Modifier.align(Alignment.Start)) {
                    Text("‹  Volver a mi espacio", color = tinta, fontFamily = LetraDiario, fontSize = 18.sp)
                }
            }
            Image(painterResource(R.drawable.icono_ondavital), "OndaVital, lunita de la aplicación",
                Modifier.size(118.dp))
            Text(titulo, color = tinta, fontFamily = LetraDiario, fontSize = 30.sp,
                textAlign = TextAlign.Center, lineHeight = 34.sp)
            Text(subtitulo, color = OndaColor.TintaSuave, fontFamily = LetraDiario, fontSize = 19.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { contentDescription = "${entrada.length} de 4 números escritos" }) {
                repeat(4) { i ->
                    Box(Modifier.size(19.dp).border(2.dp, tinta, CircleShape)
                        .background(if (i < entrada.length) OndaColor.Rosa else Color.White.copy(alpha = .65f), CircleShape))
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.height(35.dp), contentAlignment = Alignment.Center) {
                if (procesando) CircularProgressIndicator(Modifier.size(23.dp), strokeWidth = 2.dp, color = OndaColor.Rosa)
                else Text(mensaje.ifBlank { "Un espacio solo para ti ♡" }, color = tinta,
                    fontFamily = LetraDiario, fontSize = 17.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(7.dp))
            Column(Modifier.clip(RoundedCornerShape(36.dp)).background(Color(0xEFFFF8F1))
                .padding(horizontal = 17.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫")).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        fila.forEach { tecla ->
                            Box(Modifier.size(68.dp).clip(CircleShape)
                                .background(if (tecla.isNotEmpty()) Color.White.copy(alpha = .82f) else Color.Transparent)
                                .then(if (tecla.isNotEmpty()) Modifier.clickable(enabled = !procesando) {
                                    if (tecla == "⌫") entrada = entrada.dropLast(1)
                                    else if (entrada.length < 4) {
                                        entrada += tecla
                                        if (entrada.length == 4) {
                                            val completo = entrada
                                            entrada = ""
                                            onPin(completo)
                                        }
                                    }
                                } else Modifier), contentAlignment = Alignment.Center) {
                                Text(tecla, color = tinta, fontFamily = LetraDiario, fontSize = 29.sp,
                                    modifier = Modifier.semantics {
                                        contentDescription = if (tecla == "⌫") "Borrar último número" else tecla
                                    })
                            }
                        }
                    }
                }
            }
            if (onBio != null) {
                TextButton(onClick = onBio, enabled = !procesando) {
                    Text("◎  Usar huella o biometría", color = tinta, fontFamily = LetraDiario, fontSize = 19.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
