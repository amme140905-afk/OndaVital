package com.example.ondavital.analisis

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun moverMes(mes: String, cantidad: Int): String {
    require(Regex("\\d{4}-(0[1-9]|1[0-2])").matches(mes))
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val c = Calendar.getInstance().apply { time = requireNotNull(formato.parse("$mes-01")); set(Calendar.HOUR_OF_DAY, 12); add(Calendar.MONTH, cantidad) }
    return SimpleDateFormat("yyyy-MM", Locale.US).format(c.time)
}

fun finMes(mes: String): String = moverDia("${moverMes(mes, 1)}-01", -1)
