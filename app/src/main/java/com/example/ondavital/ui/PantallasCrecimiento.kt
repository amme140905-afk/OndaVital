package com.example.ondavital.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ondavital.R
import com.example.ondavital.analisis.*
import com.example.ondavital.model.RegistroDiario
import com.example.ondavital.ui.components.TarjetaPapel
import com.example.ondavital.ui.components.bordeLapiz
import com.example.ondavital.ui.theme.LetraDiario
import com.example.ondavital.ui.theme.OndaColor
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PantallaReporte(registros: List<RegistroDiario>, semana: String,
                    onSemana: (String) -> Unit, onVolver: () -> Unit, onPdf: () -> Unit) {
    TextButton(onClick = onVolver) { Letra("‹  Volver a Análisis", 18) }
    CabeceraCuaderno("Una semana en tu diario", "Un recuerdo de cómo la viviste.", R.drawable.mascota_saluda)
    val fin = moverDia(semana, 6)
    val calculo = remember(registros, semana) { mutableStateOf<Pair<ResumenDiario, ResumenDiario>?>(null) }
    LaunchedEffect(registros, semana) {
        calculo.value = withContext(Dispatchers.Default) {
            resumirDiario(registros, semana, fin) to
                resumirDiario(registros, moverDia(semana, -7), moverDia(semana, -1))
        }
    }
    val resultado = calculo.value
    if (resultado == null) {
        Letra("Preparando tu semana…", 18)
        return
    }
    val (actual, anterior) = resultado
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onSemana(moverDia(semana, -7)) }, modifier = Modifier.semantics { contentDescription = "Semana anterior" }) { Letra("‹", 28) }
        Text("${fechaBonita(semana)}\n${fechaBonita(fin)}", Modifier.weight(1f), textAlign = TextAlign.Center,
            fontFamily = LetraDiario, fontSize = 19.sp, color = OndaColor.Tinta)
        TextButton(onClick = { onSemana(moverDia(semana, 7)) }, enabled = semana < lunesSemana(hoyDiario()),
            modifier = Modifier.semantics { contentDescription = "Semana siguiente" }) { Letra("›", 28) }
    }
    if (fin >= hoyDiario() && semana <= hoyDiario()) Letra("Semana en curso · el resumen crecerá contigo", 15, OndaColor.TintaSuave)
    TarjetaPapel(color = Color(0xFFFFF4DF)) {
        Letra("${actual.dias.size} de 7 días guardados", 27)
        Letra("${actual.registros.size} momentos para recordar", 18, OndaColor.TintaSuave)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            DatoCuaderno("Estado medio", "${numeroDiario(actual.animo)} / 8", Modifier.weight(1f))
            DatoCuaderno("Energía media", "${numeroDiario(actual.energia)} / 10", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(7) { i ->
                val guardado = actual.dias.any { it.fecha == moverDia(semana, i) }
                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (guardado) OndaColor.Lavanda else Color(0xFFF0E9DF)), contentAlignment = Alignment.Center) {
                    Letra(if (guardado) "♥" else "·", 18)
                }
            }
        }
        Letra("L · M · X · J · V · S · D", 14, OndaColor.TintaSuave)
    }
    if (actual.registros.isEmpty()) {
        TarjetaPapel { Letra("Esta semana aún no tiene recuerdos", 24); Letra("Puedes explorar otra semana o comenzar tu diario desde Inicio.") }
    } else {
        FrecuenciasCuaderno(actual)
        TarjetaPapel {
            Letra("De una semana a otra", 24)
            if (actual.dias.size >= 3 && anterior.dias.size >= 3) {
                Letra("Energía: ${numeroDiario(anterior.energia)} → ${numeroDiario(actual.energia)} / 10", 22)
                Letra("Estado: ${numeroDiario(anterior.animo)} → ${numeroDiario(actual.animo)} / 8", 22)
                Letra("${anterior.dias.size} días registrados antes y ${actual.dias.size} esta semana. Son medias de los días disponibles.", 15, OndaColor.TintaSuave)
            } else {
                Letra("Para comparar necesitamos al menos 3 días registrados en cada semana.", 18)
                Letra("Semana anterior: ${anterior.dias.size} días · Esta: ${actual.dias.size} días", 15, OndaColor.TintaSuave)
            }
        }
        TarjetaPapel(color = Color(0xFFF1EDF9)) {
            Letra("Los pequeños ingredientes", 25)
            if (actual.actividades.isEmpty()) Letra("No seleccionaste actividades esta semana.", 18)
            actual.actividades.take(6).forEach { Letra("♡ ${etiquetaActividad(it.id)} · ${it.diasCon} días", 19) }
        }
        BotonCuaderno("Guardar mi reporte en PDF", onPdf)
        Letra("El PDF incluye el resumen y las actividades. Tus notas personales permanecen en el diario.", 15, OndaColor.TintaSuave)
    }
}

@Composable
fun PantallaPerfil(registros: List<RegistroDiario>, nombre: String, personaje: Int,
                   pinActivo: Boolean, biometriaActiva: Boolean, biometriaDisponible: Boolean,
                   onSeguridad: (String) -> Unit,
                   onExportar: () -> Unit = {}, onBorrar: () -> Unit = {},
                   estadoBiometria: String? = null,
                   onGuardar: (String, Int) -> Unit) {
    var confirmarBorrado by remember { mutableStateOf(false) }
    var borrador by rememberSaveable(nombre) { mutableStateOf(nombre) }
    var elegido by rememberSaveable(personaje) { mutableIntStateOf(personaje) }
    val imagenes = listOf(R.drawable.mascota_saluda, R.drawable.mascota_lee, R.drawable.mascota_descansa)
    CabeceraCuaderno("Tu pequeño espacio", "Un diario que se parece a ti.", imagenes[elegido.coerceIn(0, 2)])
    TarjetaPapel(color = Color(0xFFF1EDF9)) {
        Letra("¿Cómo quieres que te saludemos?", 24)
        OutlinedTextField(value = borrador, onValueChange = { borrador = it.take(30).replace('\n', ' ') },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), singleLine = true,
            label = { Letra("Tu nombre o apodo", 16) },
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LetraDiario, fontSize = 21.sp, color = OndaColor.Tinta),
            shape = RoundedCornerShape(17.dp))
        Letra("Elige tu lunita de perfil", 21)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            imagenes.forEachIndexed { i, recurso ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (elegido == i) OndaColor.Papel else Color.Transparent)
                    .selectable(elegido == i, role = Role.RadioButton, onClick = { elegido = i })
                    .padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(recurso), null, Modifier.size(66.dp))
                    Letra(listOf("Saludando", "Leyendo", "Soñando")[i], 15)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BotonCuaderno("Guardar mi espacio") { onGuardar(borrador.trim(), elegido) }
    }
    TarjetaPapel(color = Color(0xFFFFF4DF)) {
        Letra("Lo que has ido construyendo", 24)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            DatoCuaderno("Días en tu diario", registros.map { it.fechaIso }.distinct().size.toString(), Modifier.weight(1f))
            DatoCuaderno("Momentos", registros.size.toString(), Modifier.weight(1f))
        }
        registros.minByOrNull { it.fechaIso }?.let { Letra("Tu primera página: ${fechaBonita(it.fechaIso)}", 16, OndaColor.TintaSuave) }
        if (registros.isEmpty()) Letra("Tu primera página puede empezar hoy.", 18, OndaColor.TintaSuave)
    }
    TarjetaPapel(color = Color(0xFFF1EDF9)) {
        Letra("Una llave para tu diario", 25)
        Letra(if (pinActivo) "Tu PIN de 4 números está activo." else "Puedes pedir un PIN de 4 números al abrir la app.", 18)
        if (!pinActivo) {
            BotonCuaderno("Crear mi PIN") { onSeguridad("crear") }
        } else {
            BotonCuaderno("Cambiar mi PIN") { onSeguridad("cambiar") }
            TextButton(onClick = { onSeguridad("quitar") }) { Letra("Quitar el PIN", 18) }
            TextButton(onClick = { onSeguridad("bloquear") }) { Letra("Bloquear mi diario", 18) }
        }
        Letra("Tu huella también puede ser tu llave", 23)
        Letra(estadoBiometria ?: if (biometriaDisponible) "Tu teléfono tiene biometría compatible." else "Puedes comprobar la biometría en los ajustes del teléfono.", 16)
        if (!pinActivo) {
            Letra("Primero crea un PIN de respaldo; después podrás activar la huella.", 17)
            TextButton(onClick = { onSeguridad("crear") }) { Letra("Crear PIN para usar mi huella", 18) }
        } else if (biometriaActiva) {
            TextButton(onClick = { onSeguridad("desactivar_bio") }) { Letra("Desactivar huella o biometría", 18) }
        } else if (biometriaDisponible) {
            BotonCuaderno("Activar huella o biometría") { onSeguridad("activar_bio") }
            Letra("Te pediremos el PIN y una lectura de tu huella para comprobar que funciona.", 16)
        }
        if (!biometriaDisponible) TextButton(onClick = { onSeguridad("configurar_huella") }) { Letra("Configurar huella en mi teléfono", 18) }
        TextButton(onClick = { onSeguridad("revisar_bio") }) { Letra("Volver a comprobar mi huella", 17) }
        Letra("Al volver después de 2 minutos, te pediremos acceso. El PIN protege la entrada a la app; tus notas locales no están cifradas.", 15, OndaColor.TintaSuave)
        if (pinActivo) Letra("Si lo olvidas, no podemos recuperarlo. Borrar los datos de la app también elimina tu diario.", 15, OndaColor.TintaSuave)
    }
    TarjetaPapel {
        Letra("A tu ritmo", 25)
        Letra("No hace falta escribir todos los días ni encontrar las palabras perfectas. Puedes volver cuando quieras.", 19)
    }
    TarjetaPapel(color = Color(0xFFEDF7F5)) {
        Letra("Sobre tu diario", 23)
        Letra("Tus registros se guardan en este dispositivo. Puedes conservar un resumen semanal en PDF desde Análisis.", 17)
        Letra("Al desinstalar la app o borrar sus datos se elimina el diario local.", 15, OndaColor.TintaSuave)
        BotonCuaderno("Exportar todos mis datos", onExportar)
        Letra("Incluye tus notas, fechas, actividades y perfil en un archivo JSON legible, sin cifrado. El PIN y sus secretos no se incluyen. Guárdalo en un lugar que tú elijas.", 16)
        TextButton(onClick = { confirmarBorrado = true }) { Letra("Borrar mis datos de la app", 18) }
    }
    if (confirmarBorrado) AlertDialog(onDismissRequest = { confirmarBorrado = false },
        title = { Letra("¿Borrar estas páginas?", 25) },
        text = { Letra("Se borrarán tus registros, notas, perfil, actividades propias y configuración de PIN y biometría. No se puede deshacer. Los archivos que guardaste fuera de la app permanecen en su ubicación. Los ejemplos no volverán a cargarse automáticamente.", 18) },
        confirmButton = { TextButton(onClick = { confirmarBorrado = false; onBorrar() }) { Letra("Sí, borrar mis datos", 18) } },
        dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Letra("Conservar mi diario", 18) } })
}

@Composable
internal fun FrecuenciasCuaderno(resumen: ResumenDiario) {
    TarjetaPapel {
        Letra("Los colores que más aparecieron", 24)
        resumen.frecuencias.forEach { (estado, cantidad) ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Letra(estado.etiqueta, 18)
                Letra("$cantidad de ${resumen.registros.size}", 15, OndaColor.TintaSuave)
            }
            Box(Modifier.fillMaxWidth().padding(top = 3.dp).height(7.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0EAF5))) {
                Box(Modifier.fillMaxWidth(cantidad.toFloat() / resumen.registros.size).fillMaxHeight()
                    .background(if (estado.puntuacion % 2 == 0) OndaColor.Celeste else OndaColor.RosaClaro))
            }
        }
        Spacer(Modifier.height(10.dp))
        Letra("Frecuencia por registro: una persona puede guardar varios momentos en un día.", 14, OndaColor.TintaSuave)
    }
}

@Composable
private fun GraficaDiaria(resumen: ResumenDiario, energia: Boolean) {
    val fechas = remember(resumen.inicio, resumen.fin) {
        generateSequence(resumen.inicio) { moverDia(it, 1) }.takeWhile { it <= resumen.fin }.toList()
    }
    val porFecha = resumen.dias.associateBy { it.fecha }
    val valores = fechas.map { fecha -> porFecha[fecha]?.let { if (energia) it.energia else it.animo } }
    val maximo = if (energia) 10 else 8
    val color = if (energia) Color(0xFF4C989E) else Color(0xFFB173A6)
    val titulo = if (energia) "Energía · 1–10" else "Estado de ánimo · 1–8"
    Letra(titulo, 18)
    val descripcion = valores.mapIndexedNotNull { i, valor -> valor?.let { "${fechas[i]}: ${numeroDiario(it)}" } }.joinToString("; ")
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.height(114.dp).width(25.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Letra(maximo.toString(), 12, OndaColor.TintaSuave)
            Letra("1", 12, OndaColor.TintaSuave)
        }
        Canvas(Modifier.weight(1f).height(114.dp).semantics { contentDescription = "$titulo. $descripcion" }) {
            val margen = 6.dp.toPx()
            val ancho = size.width - margen * 2
            val alto = size.height - margen * 2
            for (f in listOf(0f, .5f, 1f)) drawLine(Color(0xFFE6DEEC), Offset(margen, margen + alto * f),
                Offset(size.width - margen, margen + alto * f), 1.dp.toPx())
            fun punto(i: Int, v: Double) = Offset(margen + ancho * i / (fechas.size - 1).coerceAtLeast(1),
                margen + alto * (1f - ((v - 1) / (maximo - 1)).toFloat()))
            valores.forEachIndexed { i, valor ->
                if (valor != null) {
                    val actual = punto(i, valor)
                    val previo = if (i > 0) valores[i - 1] else null
                    if (previo != null) drawLine(color, punto(i - 1, previo), actual, 2.5.dp.toPx(), StrokeCap.Round)
                    drawCircle(color, 3.5.dp.toPx(), actual)
                    drawCircle(Color.White, 1.2.dp.toPx(), actual)
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 25.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Letra(resumen.inicio.substring(5), 12, OndaColor.TintaSuave)
        Letra(resumen.fin.substring(5), 12, OndaColor.TintaSuave)
    }
}

@Composable
internal fun CabeceraCuaderno(titulo: String, subtitulo: String, imagen: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Letra(titulo, 30)
            Letra(subtitulo, 18, OndaColor.TintaSuave)
        }
        Image(painterResource(imagen), null, Modifier.size(82.dp))
    }
}

@Composable
internal fun DatoCuaderno(etiqueta: String, valor: String, modifier: Modifier) {
    Column(modifier.padding(end = 5.dp)) { Letra(valor, 27); Letra(etiqueta, 15, OndaColor.TintaSuave) }
}

@Composable
internal fun BotonCuaderno(texto: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(24.dp))
        .bordeLapiz(Color(0xFFE7A06C), Color(0xFFFFE6A0), true)
        .clickable(role = Role.Button, onClick = onClick).padding(12.dp), contentAlignment = Alignment.Center) {
        Letra(texto, 21)
    }
}

@Composable
internal fun Letra(texto: String, tamano: Int = 19, color: Color = OndaColor.Tinta) {
    Text(texto, fontFamily = LetraDiario, fontSize = tamano.sp, color = color)
}
