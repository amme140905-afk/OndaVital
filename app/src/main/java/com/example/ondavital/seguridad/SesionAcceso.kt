package com.example.ondavital.seguridad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Sobrevive rotaciones, pero nunca restaura una sesión abierta tras cerrar el proceso. */
class SesionAcceso : ViewModel() {
    var abierto by mutableStateOf(false)
    var ultimoFondo: Long = 0L
    var bienvenidaTerminada by mutableStateOf(false)
    var inicioBienvenida: Long = 0L
    var solicitudBiometrica: String? = null
    var promptBiometricoLanzado: Boolean = false
    var mensajeBiometrico by mutableStateOf("")
    var biometriaActivada by mutableStateOf(false)
}
