package com.example.ondavital.data

import android.content.Context
import com.example.ondavital.analisis.*

/** Sustitución solicitada de los ejemplos anteriores, sin respaldo. En IO, una sola vez. */
fun prepararDemostracion(context: Context, fin: String = hoyDiario()) {
    val preferencias = context.getSharedPreferences("ondavital_demo_entrega", Context.MODE_PRIVATE)
    if (preferencias.getBoolean("borrado_en_curso", false)) {
        DatosUsuarioLocal(context).borrarTodo()
        return
    }
    if (preferencias.getBoolean("borrado_por_usuario", false) || preferencias.getBoolean("preparada_v2", false)) return
    val diario = DiarioLocal(context)
    val ejemplos = generarDiarioEjemplo(fin).sortedByDescending { it.horaRegistroEpochMs }
    diario.guardar(ejemplos)
    val actividades = context.getSharedPreferences("ondavital_actividades", Context.MODE_PRIVATE)
    val personales = ejemplos.flatMap { it.actividadIds }.filter { it.startsWith("personal:") }.map { it.removePrefix("personal:") }.toSet()
    check(actividades.edit().putStringSet("actividades_personales",
        actividades.getStringSet("actividades_personales", emptySet()).orEmpty() + personales).commit())
    check(preferencias.edit().putBoolean("preparada_v2", true).commit())
}
