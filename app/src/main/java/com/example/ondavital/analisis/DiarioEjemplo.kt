package com.example.ondavital.analisis

import com.example.ondavital.model.RegistroDiario
import java.util.Random

private data class Escena(val nota: String, val actividades: Set<String>)

/** Diario ficticio para recorrer la app. Los seis escenarios controlados siguen separados. */
fun generarDiarioEjemplo(fin: String, semilla: Long = 20271): List<RegistroDiario> {
    val azar = Random(semilla + 71)
    val alegres = listOf(
        Escena("Hoy fui a bailar con mis amigos, estoy contento. Nos quedamos platicando un buen rato.", setOf("amistades", "personal:Bailé")),
        Escena("Hoy mi mamá hizo guiso, qué rico. Comimos juntos y hasta repetí plato.", setOf("familia", "personal:Comí rico")),
        Escena("Salimos por un helado después de clase. Me dio mucho gusto ver a mis amigos.", setOf("amistades", "personal:Comí helado", "estudio")),
        Escena("Por fin entendí el ejercicio que no me salía. Después jugué un ratito para celebrar.", setOf("estudio", "juegos")),
        Escena("Fui a correr al parque y volví con ganas de hacer cosas. Me gustó ese ratito para mí.", setOf("ejercicio", "personal:Corrí")),
        Escena("Hice un dibujo mientras escuchaba música. Me quedó bonito y me dio mucha ilusión.", setOf("personal:Dibujé", "personal:Escuché música"))
    )
    val tranquilos = listOf(
        Escena("Día tranquilo en casa. Leí unas páginas y preparé algo sencillo de cenar.", setOf("descanso", "personal:Leí", "personal:Cociné")),
        Escena("Fui a clase y adelanté un poco la tarea. Nada especial, pero estuvo bien.", setOf("estudio")),
        Escena("Hoy trabajé por la mañana. En la tarde vi una serie y me tomé las cosas con calma.", setOf("trabajo", "descanso", "personal:Vi una serie")),
        Escena("Ayudé en casa y platiqué con mi mamá mientras cocinábamos. Fue un día sencillo.", setOf("familia", "personal:Cociné")),
        Escena("Me quedé leyendo y luego jugué un rato. Hoy me apetecía estar en casa.", setOf("personal:Leí", "juegos", "descanso"))
    )
    val pesados = listOf(
        Escena("La tarea se me juntó y terminé frustrado. Dejé lo que faltaba para mañana.", setOf("estudio", "descanso")),
        Escena("Hoy el trabajo estuvo pesado. Llegué con ganas de cenar y descansar un rato.", setOf("trabajo", "descanso")),
        Escena("No me salió lo que estaba intentando y me molesté. Puse música y me di un descanso.", setOf("personal:Escuché música", "descanso")),
        Escena("Me costó concentrarme en clase. En la tarde preferí estar tranquilo y acostarme temprano.", setOf("estudio", "descanso"))
    )
    return generarDemostracion(EscenarioDemo.POSITIVA, fin, semilla).map { r ->
        val opciones = when (r.puntuacionAnimo) { in 7..8 -> alegres; in 5..6 -> tranquilos; else -> pesados }
        val escena = opciones[azar.nextInt(opciones.size)]
        val paseo = if ("caminata" in r.actividadIds) " También salí a caminar un rato." else ""
        r.copy(nota = escena.nota + paseo, actividadIds = escena.actividades + r.actividadIds.filter { it == "caminata" })
    }
}
