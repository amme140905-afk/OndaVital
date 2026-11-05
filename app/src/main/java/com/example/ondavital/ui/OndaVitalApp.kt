package com.example.ondavital.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ondavital.R
import com.example.ondavital.data.DiarioLocal
import com.example.ondavital.data.prepararDemostracion
import com.example.ondavital.data.DatosUsuarioLocal
import com.example.ondavital.data.escribirReporteSemanalPdf
import com.example.ondavital.data.escribirReporteMensualPdf
import com.example.ondavital.data.guardarPdfEnDescargas
import android.os.Build
import com.example.ondavital.analisis.*
import com.example.ondavital.model.EstadoAnimo
import com.example.ondavital.model.RegistroDiario
import com.example.ondavital.ui.components.*
import com.example.ondavital.ui.registro.RegistroScreen
import com.example.ondavital.ui.theme.LetraDiario
import com.example.ondavital.ui.theme.OndaColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OndaVitalApp(pinActivo: Boolean, biometriaActiva: Boolean,
                 biometriaDisponible: Boolean, onSeguridad: (String) -> Unit,
                 onDatosBorrados: () -> Unit = {}, estadoBiometria: String? = null) {
    val contexto = LocalContext.current.applicationContext
    val diario = remember(contexto) { DiarioLocal(contexto) }
    val scope = rememberCoroutineScope()
    var registros by remember { mutableStateOf<List<RegistroDiario>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var falloLectura by remember { mutableStateOf(false) }
    var reintento by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var pantalla by rememberSaveable { mutableStateOf("inicio") }
    val borradores = rememberSaveableStateHolder()
    val entrada = remember(pantalla) { Animatable(0f) }
    LaunchedEffect(pantalla) { entrada.animateTo(1f, tween(150)) }
    var recuerdoGuardado by remember { mutableStateOf(false) }
    var revisionDatos by rememberSaveable { mutableIntStateOf(0) }
    var esDemo by remember { mutableStateOf(!contexto.getSharedPreferences("ondavital_demo_entrega", Context.MODE_PRIVATE).getBoolean("borrado_por_usuario", false)) }
    var origen by rememberSaveable { mutableStateOf("inicio") }
    var detalleId by rememberSaveable { mutableLongStateOf(0L) }
    var mesVisible by rememberSaveable { mutableStateOf(formato("yyyy-MM")) }
    var diaElegido by rememberSaveable { mutableStateOf(formato("yyyy-MM-dd")) }
    var diasAnalisis by rememberSaveable { mutableIntStateOf(30) }
    var semanaReporte by rememberSaveable { mutableStateOf(lunesSemana(hoyDiario())) }
    var semanaExportar by rememberSaveable { mutableStateOf(semanaReporte) }
    var uriPdf by rememberSaveable { mutableStateOf<String?>(null) }
    var uriDatos by rememberSaveable { mutableStateOf<String?>(null) }
    var mesReporte by rememberSaveable { mutableStateOf(hoyDiario().take(7)) }
    var mesExportar by rememberSaveable { mutableStateOf(mesReporte) }
    var uriMensual by rememberSaveable { mutableStateOf<String?>(null) }
    var alternativaPdf by remember { mutableStateOf<String?>(null) }
    var tarea by remember { mutableStateOf<String?>(null) }
    val preferencias = remember(contexto) { contexto.getSharedPreferences("ondavital_perfil", Context.MODE_PRIVATE) }
    var nombre by remember { mutableStateOf(preferencias.getString("nombre", "").orEmpty()) }
    var personaje by remember { mutableIntStateOf(preferencias.getInt("personaje", 0).coerceIn(0, 2)) }
    val destinos = listOf("inicio", "calendario", "analisis", "perfil")
    val crearPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uriPdf = uri?.toString()
    }
    val crearDatos = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uriDatos = uri?.toString() }
    val crearMensual = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uriMensual = uri?.toString() }
    LaunchedEffect(uriMensual, cargando, falloLectura) {
        val destino = uriMensual
        if (destino != null && !cargando && !falloLectura) {
            tarea = "Preparando tu mes…"
            try {
                val resumen = withContext(Dispatchers.Default) { estadisticaPeriodo(registros, "$mesExportar-01", finMes(mesExportar)) }
                withContext(Dispatchers.IO) {
                    requireNotNull(contexto.contentResolver.openOutputStream(Uri.parse(destino), "w")).use {
                        escribirReporteMensualPdf(contexto, it, resumen, if (esDemo) "Demostración" else nombre)
                    }
                }
                Toast.makeText(contexto, "Tu mes quedó guardado en PDF", Toast.LENGTH_LONG).show()
            } catch (cancelacion: CancellationException) { throw cancelacion
            } catch (_: Exception) { error = "No pudimos completar el PDF mensual. Puedes volver a intentarlo."
            } finally { uriMensual = null; tarea = null }
        }
    }
    LaunchedEffect(uriDatos, cargando, falloLectura) {
        val destino = uriDatos
        if (destino != null && !cargando && !falloLectura) {
            tarea = "Preparando tus páginas…"
            try {
                withContext(Dispatchers.IO) {
                    val json = DatosUsuarioLocal(contexto).exportar().toString(2)
                    requireNotNull(contexto.contentResolver.openOutputStream(Uri.parse(destino), "w")).bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                }
                Toast.makeText(contexto, "Tus datos se guardaron en la ubicación elegida", Toast.LENGTH_LONG).show()
            } catch (cancelacion: CancellationException) { throw cancelacion
            } catch (_: Exception) { error = "No pudimos completar la exportación. El archivo puede estar incompleto; puedes intentarlo de nuevo."
            } finally { uriDatos = null; tarea = null }
        }
    }

    LaunchedEffect(uriPdf, cargando, falloLectura) {
        val destino = uriPdf
        if (destino != null && !cargando && !falloLectura) {
            tarea = "Preparando tu reporte…"
            try {
                val (actual, anterior) = withContext(Dispatchers.Default) {
                    resumirDiario(registros, semanaExportar, moverDia(semanaExportar, 6)) to
                        resumirDiario(registros, moverDia(semanaExportar, -7), moverDia(semanaExportar, -1))
                }
                withContext(Dispatchers.IO) {
                    requireNotNull(contexto.contentResolver.openOutputStream(Uri.parse(destino), "w")).use { salida ->
                        escribirReporteSemanalPdf(contexto, salida, actual, anterior, if (esDemo) "Demostración" else nombre)
                    }
                }
                Toast.makeText(contexto, "Reporte guardado en la ubicación elegida", Toast.LENGTH_LONG).show()
            } catch (cancelacion: CancellationException) { throw cancelacion
            } catch (_: Exception) {
                error = "No se pudo completar el PDF. Puedes volver a guardarlo y elegir otra ubicación."
            } finally { tarea = null; uriPdf = null }
        }
    }


    LaunchedEffect(reintento) {
        cargando = true
        try {
            registros = withContext(Dispatchers.IO) {
                prepararDemostracion(contexto)
                diario.leer()
            }
            falloLectura = false
        } catch (cancelacion: CancellationException) { throw cancelacion
        } catch (_: Exception) { falloLectura = true
        } finally { cargando = false }
    }
    BackHandler(enabled = pantalla != "inicio" || guardando || tarea != null) {
        if (!guardando && tarea == null) pantalla = when (pantalla) {
            in destinos -> "inicio"
            "reporte", "reporte_mensual" -> "analisis"
            else -> origen
        }
    }
    val abrirDetalle: (RegistroDiario) -> Unit = { r ->
        origen = pantalla
        detalleId = r.horaRegistroEpochMs
        pantalla = "detalle"
    }
    val nuevo: () -> Unit = { origen = pantalla; pantalla = "registro" }

    FondoPapel {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            when {
                cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OndaColor.Rosa)
                }
                falloLectura -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    Titulo("Tu diario está aquí")
                    Texto("No pudimos abrirlo. Tus datos no se han reemplazado. Intenta cargarlo otra vez.")
                    Accion("Volver a intentar") { reintento++ }
                }
                pantalla == "registro" -> Column(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    Volver { pantalla = origen }
                    borradores.SaveableStateProvider("registro") {
                    RegistroScreen(onGuardar = { registro ->
                        if (!guardando) {
                            guardando = true
                            scope.launch {
                                try {
                                    // Identificador único incluso ante dos guardados con el mismo milisegundo.
                                    val hora = maxOf(registro.horaRegistroEpochMs,
                                        (registros.maxOfOrNull { it.horaRegistroEpochMs } ?: 0L) + 1L)
                                    val nuevos = (registros + registro.copy(horaRegistroEpochMs = hora))
                                        .sortedByDescending { it.horaRegistroEpochMs }
                                    withContext(Dispatchers.IO) { diario.guardar(nuevos) }
                                    registros = nuevos
                                    pantalla = "inicio"
                                    borradores.removeState("registro")
                                    recuerdoGuardado = true
                                } catch (cancelacion: CancellationException) { throw cancelacion
                                } catch (_: Exception) {
                                    error = "No se pudo guardar. Tu texto sigue en la pantalla; vuelve a intentarlo."
                                } finally { guardando = false }
                            }
                        }
                    })
                    }
                }
                pantalla == "detalle" -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    Volver { pantalla = origen }
                    val registro = registros.firstOrNull { it.horaRegistroEpochMs == detalleId }
                    if (registro != null) Detalle(registro) else Texto("No encontramos este recuerdo.")
                }
                pantalla == "calendario" -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    Calendario(registros, mesVisible, diaElegido,
                        onMes = { mesVisible = it; diaElegido = "$it-01" },
                        onDia = { diaElegido = it }, onAbrir = abrirDetalle, onNuevo = nuevo)
                }
                pantalla == "analisis" -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    PantallaAnalisis(registros, diasAnalisis, demostracionGlobal = esDemo, onReporteMensual = { pantalla = "reporte_mensual" }, onDias = { diasAnalisis = it }, onAbrir = abrirDetalle, onReporte = {
                        semanaReporte = lunesSemana(hoyDiario())
                        pantalla = "reporte"
                    })
                }
                pantalla == "reporte_mensual" -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    PantallaReporteMensual(registros, mesReporte, onMes = { mesReporte = it },
                        onVolver = { pantalla = "analisis" }, onPdf = {
                            mesExportar = mesReporte
                            try { crearMensual.launch("OndaVital-mes-$mesReporte.pdf") }
                            catch (_: Exception) { alternativaPdf = "mensual" }
                        })
                }
                pantalla == "reporte" -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    PantallaReporte(registros, semanaReporte, onSemana = { semanaReporte = it },
                        onVolver = { pantalla = "analisis" }, onPdf = {
                            semanaExportar = semanaReporte
                            try { crearPdf.launch("OndaVital-semana-$semanaReporte.pdf") }
                            catch (_: Exception) { alternativaPdf = "semanal" }
                        })
                }
                pantalla == "perfil" -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    key(revisionDatos) {
                    PantallaPerfil(registros, nombre, personaje, pinActivo, biometriaActiva,
                        biometriaDisponible, onSeguridad, estadoBiometria = estadoBiometria,
                        onExportar = { crearDatos.launch("OndaVital-datos-${hoyDiario()}.json") },
                        onBorrar = {
                            if (tarea == null) scope.launch {
                                tarea = "Cerrando estas páginas…"
                                try {
                                    withContext(Dispatchers.IO) { DatosUsuarioLocal(contexto).borrarTodo() }
                                    registros = emptyList(); nombre = ""; personaje = 0; esDemo = false
                                    borradores.removeState("registro"); revisionDatos++
                                    recuerdoGuardado = false; onDatosBorrados()
                                    Toast.makeText(contexto, "Tus datos de la app se borraron", Toast.LENGTH_LONG).show()
                                } catch (cancelacion: CancellationException) { throw cancelacion
                                } catch (_: Exception) { error = "No pudimos completar el borrado. Al volver a abrir OndaVital se intentará terminarlo."
                                } finally { tarea = null }
                            }
                        }) { nuevoNombre, nuevaImagen ->
                        if (tarea == null) scope.launch {
                            tarea = "Guardando tu espacio…"
                            try {
                                val exito = withContext(Dispatchers.IO) {
                                    preferencias.edit().putString("nombre", nuevoNombre)
                                        .putInt("personaje", nuevaImagen).commit()
                                }
                                check(exito)
                                nombre = nuevoNombre
                                personaje = nuevaImagen
                                Toast.makeText(contexto, "Tu espacio está guardado", Toast.LENGTH_SHORT).show()
                            } catch (cancelacion: CancellationException) { throw cancelacion
                            } catch (_: Exception) { error = "No se pudo guardar tu perfil. Vuelve a intentarlo."
                            } finally { tarea = null }
                        }
                    }
                    }
                }
                else -> Pagina(Modifier.weight(1f).graphicsLayer { alpha = entrada.value }) {
                    Inicio(registros, nombre, nuevo, abrirDetalle) { pantalla = "calendario" }
                }
            }
            if (!cargando && !falloLectura && pantalla in destinos) {
                BarraInferiorConGatito {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                        .bordeLapiz(OndaColor.Rosa, OndaColor.Papel, true).selectableGroup()
                        .padding(6.dp)) {
                        listOf("inicio" to "Inicio", "calendario" to "Calendario", "analisis" to "Análisis", "perfil" to "Perfil").forEach { (ruta, nombre) ->
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                                .background(if (pantalla == ruta) OndaColor.Lavanda.copy(alpha = .55f) else Color.Transparent)
                                .selectable(pantalla == ruta, role = Role.Tab, onClick = { pantalla = ruta })
                                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    IconoPestana(ruta)
                                    Texto(nombre, 14)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (guardando || tarea != null) Dialog(onDismissRequest = {}) {
        TarjetaPapel(color = OndaColor.Papel) {
            Texto(tarea ?: "Guardando tu recuerdo…")
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp), color = OndaColor.Rosa)
        }
    }
    error?.let { mensaje ->
        AlertDialog(onDismissRequest = { error = null }, text = { Texto(mensaje) },
            confirmButton = { TextButton(onClick = { error = null }) { Texto("Entendido") } })
    }
    if (recuerdoGuardado) Dialog(onDismissRequest = { recuerdoGuardado = false }) {
        TarjetaPapel(color = OndaColor.Papel) {
            Image(painterResource(R.drawable.mascota_saluda), null, Modifier.size(90.dp).align(Alignment.CenterHorizontally))
            Texto("Tu momento ya tiene una página", 27)
            Texto("Gracias por darte un ratito para ti.", 20)
            TextButton(onClick = { recuerdoGuardado = false }) { Texto("Seguir con mi diario", 19) }
        }
    }
    alternativaPdf?.let { tipo ->
        AlertDialog(onDismissRequest = { alternativaPdf = null },
            title = { Texto("El selector de archivos no pudo abrirse", 24) },
            text = { Texto(if (Build.VERSION.SDK_INT >= 29) "Puedes guardar el PDF directamente en Descargas/OndaVital."
                else "Este teléfono no pudo abrir el selector. Habilita su aplicación Archivos o Documentos e inténtalo otra vez.") },
            dismissButton = { TextButton(onClick = { alternativaPdf = null }) { Texto("Volver") } },
            confirmButton = {
                if (Build.VERSION.SDK_INT >= 29) TextButton(onClick = {
                    alternativaPdf = null
                    tarea = "Preparando tu reporte…"
                    scope.launch {
                        try {
                            if (tipo == "mensual") {
                                val resumen = withContext(Dispatchers.Default) { estadisticaPeriodo(registros, "$mesExportar-01", finMes(mesExportar)) }
                                withContext(Dispatchers.IO) { guardarPdfEnDescargas(contexto, "OndaVital-mes-$mesExportar.pdf") {
                                    escribirReporteMensualPdf(contexto, it, resumen, if (esDemo) "Demostración" else nombre)
                                } }
                            } else {
                                val (actual, anterior) = withContext(Dispatchers.Default) {
                                    resumirDiario(registros, semanaExportar, moverDia(semanaExportar, 6)) to
                                        resumirDiario(registros, moverDia(semanaExportar, -7), moverDia(semanaExportar, -1))
                                }
                                withContext(Dispatchers.IO) { guardarPdfEnDescargas(contexto, "OndaVital-semana-$semanaExportar.pdf") {
                                    escribirReporteSemanalPdf(contexto, it, actual, anterior, if (esDemo) "Demostración" else nombre)
                                } }
                            }
                            Toast.makeText(contexto, "PDF guardado en Descargas/OndaVital", Toast.LENGTH_LONG).show()
                        } catch (cancelacion: CancellationException) { throw cancelacion
                        } catch (_: Exception) { error = "No pudimos guardar el PDF. Tus registros siguen en el diario."
                        } finally { tarea = null }
                    }
                }) { Texto("Guardar en Descargas") }
                else TextButton(onClick = { alternativaPdf = null }) { Texto("Entendido") }
            })
    }
}

@Composable
private fun Inicio(registros: List<RegistroDiario>, nombre: String, onNuevo: () -> Unit,
                   onAbrir: (RegistroDiario) -> Unit, onCalendario: () -> Unit) {
    val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val saludo = when (hora) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }
    Texto("ONDA VITAL  ·  TU PEQUEÑO REFUGIO", 13, OndaColor.TintaSuave)
    Titulo(if (nombre.isBlank()) "$saludo, qué lindo verte" else "$saludo, $nombre")
    Texto(formato("EEEE d 'de' MMMM"), 17, OndaColor.TintaSuave)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.mascota_saluda), "La lunita te da la bienvenida",
            Modifier.size(188.dp))
    }
    Text("Un ratito para ti", Modifier.fillMaxWidth(), fontFamily = LetraDiario,
        fontSize = 29.sp, color = OndaColor.Tinta, textAlign = TextAlign.Center)
    Text("Tu día merece una página, tal como fue.", Modifier.fillMaxWidth(),
        fontFamily = LetraDiario, fontSize = 18.sp, color = OndaColor.TintaSuave,
        textAlign = TextAlign.Center)
    val hoy = registros.filter { it.fechaIso == formato("yyyy-MM-dd") }
    TarjetaPapel(color = Color(0xFFFFF5DF)) {
        Texto("TU MOMENTO DE HOY", 13, OndaColor.TintaSuave)
        Texto(if (hoy.isEmpty()) "Una página por descubrir" else "Ya guardaste un pedacito de hoy", 24)
        Spacer(Modifier.height(8.dp))
        Accion(if (hoy.isEmpty()) "＋ Contar mi día" else "＋ Añadir otro momento", onNuevo)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Tu semana", Modifier.weight(1f), fontFamily = LetraDiario, fontSize = 24.sp, color = OndaColor.Tinta)
        TextButton(onClick = onCalendario) { Texto("Ver calendario", 16) }
    }
    val lunes = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
    }
    TarjetaPapel {
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, letra ->
                val dia = (lunes.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
                val r = registros.firstOrNull { it.fechaIso == formato("yyyy-MM-dd", dia.time) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Texto(letra, 14, OndaColor.TintaSuave)
                    Box(Modifier.padding(top = 5.dp).size(32.dp).clip(RoundedCornerShape(12.dp))
                        .background(r?.let { tono(it.estado) } ?: Color(0xFFF3EDF6)), contentAlignment = Alignment.Center) {
                        Texto(dia.get(Calendar.DAY_OF_MONTH).toString(), 16)
                    }
                    Texto(if (r == null) "·" else "♥", 13, OndaColor.Rosa)
                }
            }
        }
        Texto("El color muestra el último estado de cada día.", 13, OndaColor.TintaSuave)
    }
    Titulo("Recuerdos recientes", 25)
    if (registros.isEmpty()) {
        TarjetaPapel {
            Texto("Aquí crecerá tu pequeño diario", 22)
            Texto("Cuando guardes tu primer día, podrás volver a leerlo aquí.", 17, OndaColor.TintaSuave)
        }
    } else registros.take(3).forEach { Ficha(it) { onAbrir(it) } }
    TarjetaPapel(color = Color(0xFFF1EDF9)) {
        Texto("Una pausa para ti", 21)
        Texto("¿Qué pequeño momento te gustaría recordar?", 18, OndaColor.TintaSuave)
    }
}

@Composable
private fun Calendario(registros: List<RegistroDiario>, mes: String, elegido: String,
                       onMes: (String) -> Unit, onDia: (String) -> Unit,
                       onAbrir: (RegistroDiario) -> Unit, onNuevo: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Titulo("Tu jardín de días")
            Texto("Cada recuerdo tiene su lugar.", 19, OndaColor.TintaSuave)
        }
        Image(painterResource(R.drawable.mascota_saluda), "La lunita saluda desde el calendario",
            Modifier.size(82.dp))
    }
    val fecha = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.YEAR, mes.substring(0, 4).toInt())
        set(Calendar.MONTH, mes.substring(5, 7).toInt() - 1)
    }
    val inicio = (fecha.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val cantidad = fecha.getActualMaximum(Calendar.DAY_OF_MONTH)
    val porDia = registros.groupBy { it.fechaIso }
    TarjetaPapel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                val anterior = (fecha.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                onMes(formato("yyyy-MM", anterior.time))
            }, modifier = Modifier.semantics { contentDescription = "Mes anterior" }) { Texto("‹", 28) }
            Text(formato("MMMM yyyy", fecha.time), Modifier.weight(1f), fontFamily = LetraDiario,
                fontSize = 22.sp, color = OndaColor.Tinta, textAlign = TextAlign.Center)
            TextButton(onClick = {
                val siguiente = (fecha.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                onMes(formato("yyyy-MM", siguiente.time))
            }, modifier = Modifier.semantics { contentDescription = "Mes siguiente" }) { Texto("›", 28) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = LetraDiario, color = OndaColor.TintaSuave)
            }
        }
        Column(Modifier.selectableGroup()) {
            repeat((inicio + cantidad + 6) / 7) { semana ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { columna ->
                        val numero = semana * 7 + columna - inicio + 1
                        if (numero !in 1..cantidad) Spacer(Modifier.weight(1f).height(52.dp)) else {
                            val clave = "$mes-${numero.toString().padStart(2, '0')}"
                            val lista = porDia[clave].orEmpty()
                            val ultimo = lista.firstOrNull()
                            Column(Modifier.weight(1f).heightIn(min = 52.dp).padding(2.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .bordeLapiz(if (clave == elegido) OndaColor.Rosa else Color.Transparent,
                                    ultimo?.let { tono(it.estado) } ?: Color(0xFFF7F3FA), true)
                                .selectable(clave == elegido, role = Role.RadioButton, onClick = { onDia(clave) })
                                .semantics { contentDescription = "$clave, ${lista.size} registros" }
                                .padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Texto(numero.toString(), 17)
                                Texto(if (ultimo != null) "♥" else "·", 11, OndaColor.Rosa)
                            }
                        }
                    }
                }
            }
        }
        Texto("Color: último estado del día · ♥: hay recuerdos", 14, OndaColor.TintaSuave)
    }
    TextButton(onClick = { onMes(formato("yyyy-MM")); onDia(formato("yyyy-MM-dd")) }) { Texto("Volver a hoy", 17) }
    Titulo("Recuerdos del ${elegido.substring(8, 10)}/${elegido.substring(5, 7)}", 25)
    val delDia = porDia[elegido].orEmpty()
    if (delDia.isEmpty()) {
        TarjetaPapel {
            Texto("Una página en blanco", 23)
            Texto("No hay registros para este día.", 18, OndaColor.TintaSuave)
            if (elegido == formato("yyyy-MM-dd")) Accion("＋ Contar mi día", onNuevo)
        }
    } else delDia.forEach { Ficha(it) { onAbrir(it) } }
}

@Composable
private fun Detalle(r: RegistroDiario) {
    Titulo("Un pedacito de tu historia")
    Texto("${r.fechaIso} · ${formato("HH:mm", Date(r.horaRegistroEpochMs))}", 18, OndaColor.TintaSuave)
    TarjetaPapel(color = tono(r.estado)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DibujoEstado(r.estado, Modifier.size(84.dp))
            Column(Modifier.weight(1f)) {
                Texto("Así te sentiste", 17, OndaColor.TintaSuave)
                Texto(r.estado.etiqueta, 30)
                Texto("Estado ${r.puntuacionAnimo} de 8", 15, OndaColor.TintaSuave)
            }
        }
    }
    TarjetaPapel {
        Texto("La luz de ese momento", 23)
        Texto("Energía: ${r.energia} / 10", 19)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            repeat(10) { i ->
                Text(if (i < r.energia) "★" else "☆", Modifier.weight(1f),
                    fontSize = 24.sp, color = if (i < r.energia) Color(0xFFBA8427) else OndaColor.Linea,
                    textAlign = TextAlign.Center)
            }
        }
    }
    TarjetaPapel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.mascota_lee), null, Modifier.size(62.dp))
            Texto("Palabras que guardaste", 23)
        }
        Texto(r.nota.ifBlank { "Este día quedó guardado sin una nota." }, 20)
    }
    TarjetaPapel {
        Texto("Lo que estuvo presente", 23)
        if (r.actividadIds.isEmpty()) Texto("Sin actividades seleccionadas.", 18, OndaColor.TintaSuave)
        else r.actividadIds.sorted().forEach { Texto("♡  ${nombreActividad(it)}", 19) }
    }
    Texto("Cada día puede sentirse distinto. Este también forma parte de ti.", 18, OndaColor.TintaSuave)
}

@Composable
private fun Ficha(r: RegistroDiario, onClick: () -> Unit) {
    TarjetaPapel(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DibujoEstado(r.estado, Modifier.size(55.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Texto(r.estado.etiqueta, 23)
                Texto("${r.fechaIso} · ${formato("HH:mm", Date(r.horaRegistroEpochMs))} · Energía ${r.energia}", 14, OndaColor.TintaSuave)
            }
            Texto("›", 28)
        }
        if (r.nota.isNotBlank()) Text(r.nota, maxLines = 2, overflow = TextOverflow.Ellipsis,
            fontFamily = LetraDiario, fontSize = 18.sp, color = OndaColor.TintaSuave)
    }
}

@Composable
private fun DibujoEstado(estado: EstadoAnimo, modifier: Modifier) {
    val indice = listOf(7, 5, 6, 4, -1, 1, 0, 3)[estado.puntuacion - 1]
    if (indice < 0) NeutralIlustrado(modifier) else EmocionIlustrada(indice, modifier)
}

@Composable
private fun Pagina(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun Texto(texto: String, tamano: Int = 19, color: Color = OndaColor.Tinta) {
    Text(texto, fontFamily = LetraDiario, fontSize = tamano.sp, color = color)
}

@Composable
private fun Titulo(texto: String, tamano: Int = 30) = Texto(texto, tamano)

@Composable
private fun Accion(texto: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(24.dp))
        .bordeLapiz(Color(0xFFE7A06C), Color(0xFFFFE6A0), true)
        .clickable(role = Role.Button, onClick = onClick).padding(12.dp), contentAlignment = Alignment.Center) {
        Texto(texto, 22)
    }
}

@Composable
private fun Volver(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Texto("‹  Volver", 20) }
}

private fun formato(patron: String, fecha: Date = Date()): String =
    SimpleDateFormat(patron, Locale("es", "MX")).format(fecha)

private fun tono(estado: EstadoAnimo): Color = listOf(
    Color(0xFFECE2FA), Color(0xFFDFEAFF), Color(0xFFFFDFE6), Color(0xFFFFE5D9),
    Color(0xFFF4EBFA), Color(0xFFE0F2FA), Color(0xFFFFEEC4), Color(0xFFFFF0C8)
)[estado.puntuacion - 1]

private fun nombreActividad(id: String): String = when (id) {
    "estudio" -> "Estudié"; "trabajo" -> "Trabajé"; "caminata" -> "Caminé"
    "ejercicio" -> "Ejercicio"; "amistades" -> "Vi amigos"; "familia" -> "Familia"
    "descanso" -> "Descansé"; "juegos" -> "Jugué"; else -> id.removePrefix("personal:")
}

/** Pequeños iconos pastel; sin una dependencia adicional de Material Icons. */
@Composable
private fun IconoPestana(ruta: String) {
    Canvas(Modifier.size(25.dp)) {
        val w = size.width
        val h = size.height
        val tinta = Color(0xFF9D80B1)
        val trazo = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        if (ruta == "inicio") {
            val casa = Path().apply {
                moveTo(w * .12f, h * .43f); lineTo(w * .5f, h * .1f)
                lineTo(w * .88f, h * .43f); lineTo(w * .88f, h * .9f)
                lineTo(w * .12f, h * .9f); close()
            }
            drawPath(casa, Color(0xFFBDE4EF))
            drawPath(casa, tinta, style = trazo)
            drawRoundRect(Color(0xFFFFE6A0), Offset(w * .4f, h * .57f),
                Size(w * .2f, h * .33f), CornerRadius(3.dp.toPx()))
        } else if (ruta == "calendario") {
            val inicio = Offset(w * .15f, h * .22f)
            val tamano = Size(w * .7f, h * .68f)
            drawRoundRect(Color(0xFFF9DCE9), inicio, tamano, CornerRadius(4.dp.toPx()))
            drawRoundRect(tinta, inicio, tamano, CornerRadius(4.dp.toPx()), style = trazo)
            for (x in listOf(.32f, .68f)) drawLine(tinta,
                Offset(w * x, h * .1f), Offset(w * x, h * .32f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(tinta, Offset(w * .29f, h * .5f), Offset(w * .7f, h * .5f), 2.dp.toPx())
            drawLine(tinta, Offset(w * .29f, h * .69f), Offset(w * .55f, h * .69f), 2.dp.toPx())
        } else if (ruta == "analisis") {
            listOf(.48f, .7f, .88f).forEachIndexed { i, alto ->
                val x = w * (.12f + i * .29f)
                val inicio = Offset(x, h * (1f - alto))
                val tamano = Size(w * .16f, h * (alto - .08f))
                drawRoundRect(if (i == 1) Color(0xFFBDE4EF) else Color(0xFFDBCBF4), inicio, tamano, CornerRadius(3.dp.toPx()))
                drawRoundRect(tinta, inicio, tamano, CornerRadius(3.dp.toPx()), style = Stroke(1.3.dp.toPx()))
            }
        } else {
            drawCircle(Color(0xFFFFE6A0), w * .18f, Offset(w * .5f, h * .26f))
            drawCircle(tinta, w * .18f, Offset(w * .5f, h * .26f), style = trazo)
            drawRoundRect(Color(0xFFF9DCE9), Offset(w * .2f, h * .54f), Size(w * .6f, h * .36f), CornerRadius(w * .15f))
            drawRoundRect(tinta, Offset(w * .2f, h * .54f), Size(w * .6f, h * .36f), CornerRadius(w * .15f), style = trazo)
        }
    }
}

data class Cuenta(val nombre: String?)

fun mostrarCuenta(cuenta: Cuenta?) {
    // Si no existe la cuenta, terminamos.
    cuenta ?: return

    // Si falta el nombre, usamos "Anónimo".
    val alias = cuenta.nombre?.trim() ?: "Anónimo"

    println(alias)
}
