package com.example.ondavital.model

/** La posición de cada estado forma la escala descriptiva 1–8 del diario. */
enum class EstadoAnimo(val puntuacion: Int, val id: String, val etiqueta: String) {
    ABRUMADO(1, "abrumado", "Abrumado"),
    TRISTE(2, "triste", "Triste"),
    FRUSTRADO(3, "frustrado", "Frustrado"),
    PREOCUPADO(4, "preocupado", "Preocupado"),
    NEUTRAL(5, "neutral", "Neutral"),
    EN_CALMA(6, "calma", "En calma"),
    ALEGRE(7, "alegre", "Alegre"),
    ILUSIONADO(8, "ilusionado", "Ilusionado")
}

data class RegistroDiario(
    val fechaIso: String,
    val horaRegistroEpochMs: Long,
    val estado: EstadoAnimo,
    val energia: Int,
    val nota: String,
    val actividadIds: Set<String>
) {
    init {
        require(energia in 1..10)
        require(nota.length <= 500)
    }

    val puntuacionAnimo: Int get() = estado.puntuacion
}
