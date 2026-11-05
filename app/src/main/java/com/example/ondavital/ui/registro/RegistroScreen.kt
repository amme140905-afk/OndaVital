package com.example.ondavital.ui.registro

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ondavital.R
import com.example.ondavital.model.EstadoAnimo
import com.example.ondavital.model.RegistroDiario
import com.example.ondavital.ui.components.*
import com.example.ondavital.ui.theme.LetraDiario
import com.example.ondavital.ui.theme.OndaColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Opcion(val id: String, val texto: String)
private val estados = EstadoAnimo.values().toList()
// Posiciones en el atlas original; Neutral tiene una ilustración propia.
private val dibujosEstado = listOf(7, 5, 6, 4, -1, 1, 0, 3)
private val actividades = listOf(
    Opcion("estudio", "Estudié"), Opcion("trabajo", "Trabajé"),
    Opcion("caminata", "Caminé"), Opcion("ejercicio", "Ejercicio"),
    Opcion("amistades", "Vi amigos"), Opcion("familia", "Familia"),
    Opcion("descanso", "Descansé"), Opcion("juegos", "Jugué")
)
private const val PREFS_ACTIVIDADES = "ondavital_actividades"
private const val KEY_ACTIVIDADES_PERSONALES = "actividades_personales"
private val tonos = listOf(
    Color(0xFFECE2FA), Color(0xFFDFEAFF), Color(0xFFFFDFE6), Color(0xFFFFE5D9),
    Color(0xFFF4EBFA), Color(0xFFE0F2FA), Color(0xFFFFEEC4), Color(0xFFFFF0C8)
)

@Composable
fun RegistroScreen(onGuardar: (RegistroDiario) -> Unit, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val preferencias = remember(contexto) {
        contexto.getSharedPreferences(PREFS_ACTIVIDADES, Context.MODE_PRIVATE)
    }
    var actividadesPersonales by remember {
        mutableStateOf(preferencias.getStringSet(KEY_ACTIVIDADES_PERSONALES, emptySet()).orEmpty().toList().sorted())
    }
    var mostrarDialogoActividad by rememberSaveable { mutableStateOf(false) }
    var estadoValor by rememberSaveable { mutableIntStateOf(0) }
    var energia by rememberSaveable { mutableIntStateOf(0) }
    var nota by rememberSaveable { mutableStateOf("") }
    var actividadIds by rememberSaveable { mutableStateOf("") }
    val seleccionadas = actividadIds.split('|').filter { it.isNotEmpty() }.toSet()
    val opcionesActividad = actividades + actividadesPersonales.map { nombre -> Opcion("personal:$nombre", nombre) }
    val fechaVisible = remember {
        SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "MX")).format(Date())
    }
    FondoPapel {
        Column(
            modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("¿Cómo estuvo tu día?", fontFamily = LetraDiario,
                        fontSize = 29.sp, lineHeight = 31.sp, color = OndaColor.Tinta)
                    Text(fechaVisible, fontFamily = LetraDiario, fontSize = 17.sp,
                        color = OndaColor.TintaSuave)
                }
                Image(
                    painter = painterResource(R.drawable.mascota_saluda),
                    contentDescription = "La mascota de OndaVital te saluda",
                    modifier = Modifier.size(64.dp)
                )
            }
            TarjetaPapel {
                Encabezado("¿Cómo te sentiste?", if (estadoValor == 0) "Elige un estado" else "$estadoValor / 8")
                Column(Modifier.selectableGroup()) {
                    estados.chunked(4).forEachIndexed { fila, opciones ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            opciones.forEachIndexed { columna, opcion ->
                                val indice = fila * 4 + columna
                                val elegido = estadoValor == opcion.puntuacion
                                Column(
                                    Modifier.weight(1f).respuestaElegida(elegido).clip(RoundedCornerShape(15.dp))
                                        .then(if (elegido) Modifier.bordeLapiz(Color(0xFFEB927B), tonos[indice]) else Modifier)
                                        .selectable(elegido, role = Role.RadioButton, onClick = {
                                            estadoValor = opcion.puntuacion
                                        }).padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (dibujosEstado[indice] == -1) {
                                        NeutralIlustrado(Modifier.size(51.dp))
                                    } else {
                                        EmocionIlustrada(dibujosEstado[indice], Modifier.size(51.dp))
                                    }
                                    Text(opcion.etiqueta, fontFamily = LetraDiario, fontSize = 14.sp,
                                        color = OndaColor.Tinta, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
            TarjetaPapel {
                Encabezado(
                    "Energía",
                    if (energia == 0) "Toca o desliza" else "$energia / 10",
                    when (energia) {
                        in 1..3 -> R.drawable.mascota_descansa
                        in 4..6 -> R.drawable.mascota_lee
                        in 7..10 -> R.drawable.mascota_saluda
                        else -> null
                    }
                )
                EnergiaEstrellas(energia, onValorChange = { energia = it })
            }
            TarjetaPapel {
                Encabezado("Unas palabras para ti", "", R.drawable.mascota_lee)
                Box(Modifier.fillMaxWidth().bordeLapiz(fondo = Color(0xFFFFFCF5)).padding(10.dp)) {
                    if (nota.isEmpty()) Text("Algo que quieras recordar de hoy…", fontFamily = LetraDiario,
                        fontSize = 19.sp, color = OndaColor.TintaSuave)
                    BasicTextField(value = nota, onValueChange = { if (it.length <= 500) nota = it },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Nota personal" },
                        minLines = 3, maxLines = 6,
                        textStyle = TextStyle(fontFamily = LetraDiario, fontSize = 19.sp,
                            lineHeight = 23.sp, color = OndaColor.Tinta))
                }
                Text("${nota.length}/500 · Opcional", Modifier.align(Alignment.End),
                    fontFamily = LetraDiario, fontSize = 14.sp, color = OndaColor.TintaSuave)
            }
            TarjetaPapel {
                Encabezado("¿Qué estuvo presente?", "${seleccionadas.size} seleccionadas")
                opcionesActividad.chunked(3).forEach { fila ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        fila.forEach { opcion ->
                            val elegido = opcion.id in seleccionadas
                            Box(Modifier.weight(1f).respuestaElegida(elegido).heightIn(min = 48.dp)
                                .semantics { contentDescription = opcion.texto }
                                .toggleable(value = elegido, role = Role.Checkbox, onValueChange = { _ ->
                                    val siguiente = if (elegido) seleccionadas - opcion.id else seleccionadas + opcion.id
                                    actividadIds = siguiente.sorted().joinToString("|")
                                }), contentAlignment = Alignment.Center) {
                                Text((if (elegido) "✓ " else "") + opcion.texto,
                                    Modifier.fillMaxWidth().bordeLapiz(
                                        if (elegido) OndaColor.Rosa else OndaColor.Linea,
                                        if (elegido) Color(0xFFF9E0EB) else Color(0xFFEAF4FF), true
                                    ).padding(horizontal = 5.dp, vertical = 6.dp),
                                    fontFamily = LetraDiario, fontSize = 16.sp,
                                    textAlign = TextAlign.Center, color = OndaColor.Tinta)
                            }
                        }
                        repeat(3-fila.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterStart) {
                    Text("＋ Añadir actividad", Modifier.clip(RoundedCornerShape(14.dp))
                        .bordeLapiz(OndaColor.Linea, Color(0xFFFFF8E8), true)
                        .clickable { mostrarDialogoActividad = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                        fontFamily = LetraDiario, fontSize = 17.sp, color = OndaColor.Tinta)
                }
            }
            BotonGuardar(estadoValor != 0 && energia != 0, onClick = {
                val ahora = System.currentTimeMillis()
                onGuardar(RegistroDiario(
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ahora)),
                    ahora, estados[estadoValor - 1], energia, nota.trim(), seleccionadas
                ))
            })
            if (estadoValor == 0 || energia == 0) Text("Elige cómo te sentiste y cuánta energía tuviste.",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontFamily = LetraDiario,
                color = OndaColor.TintaSuave, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
        }
    }
    if (mostrarDialogoActividad) {
        Dialog(onDismissRequest = { mostrarDialogoActividad = false }) {
            DialogoActividadPersonal(
                onCancelar = { mostrarDialogoActividad = false },
                onAgregar = { nombre ->
                    val nuevo = nombre.replace('|', ' ').trim().replace(Regex("\\s+"), " ").take(40)
                    val existente = actividadesPersonales.firstOrNull { it.equals(nuevo, ignoreCase = true) }
                    val elegido = existente ?: nuevo
                    if (elegido.isNotBlank()) {
                        val predefinida = actividades.firstOrNull { it.texto.equals(elegido, ignoreCase = true) }
                        if (predefinida == null && existente == null) {
                            actividadesPersonales = (actividadesPersonales + elegido).distinct().sorted()
                            preferencias.edit().putStringSet(KEY_ACTIVIDADES_PERSONALES, actividadesPersonales.toSet()).apply()
                        }
                        val id = predefinida?.id ?: "personal:$elegido"
                        actividadIds = (seleccionadas + id).sorted().joinToString("|")
                    }
                    mostrarDialogoActividad = false
                }
            )
        }
    }
}

@Composable
private fun DialogoActividadPersonal(onCancelar: () -> Unit, onAgregar: (String) -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    TarjetaPapel(
        modifier = Modifier.imePadding().verticalScroll(rememberScrollState()),
        color = OndaColor.Papel
    ) {
        Text("Una actividad tuya", fontFamily = LetraDiario, fontSize = 24.sp, color = OndaColor.Tinta)
        Text("Ponle un nombre para encontrarla después.",
            fontFamily = LetraDiario, fontSize = 17.sp, color = OndaColor.TintaSuave)
        BasicTextField(
            value = nombre,
            onValueChange = { nombre = it.take(40) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                .bordeLapiz(fondo = Color(0xFFFFFCF5)).padding(12.dp)
                .semantics { contentDescription = "Nombre de la actividad" },
            singleLine = true,
            textStyle = TextStyle(fontFamily = LetraDiario, fontSize = 19.sp, color = OndaColor.Tinta),
            decorationBox = { campo ->
                Box {
                    if (nombre.isEmpty()) Text("Nombre de la actividad", fontFamily = LetraDiario,
                        fontSize = 19.sp, color = OndaColor.TintaSuave)
                    campo()
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Cancelar", Modifier.clickable(onClick = onCancelar).padding(10.dp),
                fontFamily = LetraDiario, fontSize = 17.sp, color = OndaColor.TintaSuave)
            Spacer(Modifier.width(4.dp))
            Text("Guardar", Modifier.clip(RoundedCornerShape(13.dp)).bordeLapiz(
                OndaColor.Rosa, Color(0xFFFFE5EC), true
            ).clickable(enabled = nombre.replace('|', ' ').isNotBlank()) { onAgregar(nombre) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
                fontFamily = LetraDiario, fontSize = 18.sp, color = OndaColor.Tinta)
        }
    }
}

@Composable
private fun Encabezado(titulo: String, detalle: String, ilustracion: Int? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, Modifier.weight(1f), fontFamily = LetraDiario,
            fontSize = 22.sp, lineHeight = 24.sp, color = OndaColor.Tinta)
        Spacer(Modifier.width(6.dp))
        if (detalle.isNotEmpty()) {
            Text(detalle, fontFamily = LetraDiario, fontSize = 14.sp, color = OndaColor.TintaSuave)
        }
        if (ilustracion != null) {
            Image(painterResource(ilustracion), contentDescription = null, modifier = Modifier.size(42.dp))
        }
    }
}
