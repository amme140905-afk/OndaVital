package com.example.ondavital.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Operaciones locales de privacidad. Ejecutar en Dispatchers.IO. Nunca exporta PIN, sal o hash. */
class DatosUsuarioLocal(private val context: Context) {
    private fun preferencias(nombre: String) = context.getSharedPreferences(nombre, Context.MODE_PRIVATE)

    fun exportar(): JSONObject {
        val perfil = preferencias("ondavital_perfil")
        val actividades = preferencias("ondavital_actividades")
        return JSONObject().put("formato", "OndaVital-exportacion").put("version", 1)
            .put("diario", jsonDiario(DiarioLocal(context).leer()))
            .put("perfil", JSONObject().put("nombre", perfil.getString("nombre", ""))
                .put("personaje", perfil.getInt("personaje", 0)))
            .put("actividadesPersonales", JSONArray(actividades.getStringSet("actividades_personales", emptySet()).orEmpty().sorted()))
            .put("demostracion", preferencias("ondavital_demo_entrega").getBoolean("preparada_v2", false))
    }

    /** Marca primero la intención: si se interrumpe, el siguiente arranque termina el borrado. */
    fun borrarTodo() {
        val demo = preferencias("ondavital_demo_entrega")
        check(demo.edit().putBoolean("borrado_en_curso", true).putBoolean("borrado_por_usuario", true).commit())
        DiarioLocal(context).guardar(emptyList())
        listOf("ondavital_perfil", "ondavital_actividades", "ondavital_seguridad").forEach {
            check(preferencias(it).edit().clear().commit())
        }
        check(demo.edit().clear().putBoolean("borrado_por_usuario", true).commit())
    }
}
