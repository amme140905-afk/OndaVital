package com.example.ondavital.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.ondavital.R

object OndaColor {
    val Papel = Color(0xFFFFFAF2)
    val PapelRosa = Color(0xFFFFF3F5)
    val PapelCeleste = Color(0xFFF0F9FC)
    val Tinta = Color(0xFF3F365B)
    val TintaSuave = Color(0xFF625875)
    val Rosa = Color(0xFFD574A2)
    val RosaClaro = Color(0xFFF9DCE9)
    val Celeste = Color(0xFF9DDCE7)
    val Lavanda = Color(0xFFDBCBF4)
    val Amarillo = Color(0xFFFFD969)
    val Linea = Color(0xFF9D80B1)
}

val LetraDiario = FontFamily(Font(R.font.patrick_hand))

private val esquema = lightColorScheme(
    primary = OndaColor.Tinta,
    onPrimary = Color.White,
    secondary = OndaColor.Rosa,
    background = OndaColor.Papel,
    onBackground = OndaColor.Tinta,
    surface = OndaColor.Papel,
    onSurface = OndaColor.Tinta
)

@Composable
fun OndaVitalTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = Typography(), content = content)
}
