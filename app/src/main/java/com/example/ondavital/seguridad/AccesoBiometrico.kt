package com.example.ondavital.seguridad

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Un prompt por Activity, creado en onCreate y reconectado tras rotar. Nunca rebaja la clase. */
class AccesoBiometrico(private val activity: FragmentActivity, private val seguridad: SeguridadLocal,
                      private val sesion: SesionAcceso) {
    private val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val solicitud = sesion.solicitudBiometrica
                sesion.solicitudBiometrica = null
                sesion.promptBiometricoLanzado = false
                if (!seguridad.hayPin) return
                if (solicitud == "abrir" && seguridad.biometria) sesion.abierto = true
                if (solicitud == "activar" && sesion.abierto) sesion.viewModelScope.launch {
                    try {
                        val guardado = withContext(Dispatchers.IO) { seguridad.guardarBiometria(true) }
                        if (guardado) sesion.biometriaActivada = true
                        else sesion.mensajeBiometrico = "No se pudo guardar la activación. Puedes volver a intentarlo."
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) { sesion.mensajeBiometrico = "No se pudo guardar la activación. Puedes volver a intentarlo." }
                }
            }
            override fun onAuthenticationFailed() {
                sesion.mensajeBiometrico = "No se reconoció la huella. Inténtalo otra vez o usa tu PIN."
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                sesion.solicitudBiometrica = null
                sesion.promptBiometricoLanzado = false
                sesion.mensajeBiometrico = when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> "Puedes seguir usando tu PIN."
                    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Android bloqueó temporalmente la huella. Desbloquea el teléfono con su clave y usa tu PIN aquí."
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> "Primero registra una huella en los ajustes del teléfono."
                    else -> "El lector no está disponible ahora. Puedes usar tu PIN y volver a intentarlo."
                }
            }
        })

    fun solicitar(activar: Boolean = false) {
        if (!seguridad.hayPin || (activar && !sesion.abierto) || (!activar && !seguridad.biometria)) return
        if (sesion.solicitudBiometrica != null && sesion.promptBiometricoLanzado) return // El prompt anterior sobrevive a la rotación.
        val disponible = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (disponible != BiometricManager.BIOMETRIC_SUCCESS) {
            sesion.mensajeBiometrico = mensajeDisponibilidad(disponible)
            return
        }
        sesion.mensajeBiometrico = ""
        sesion.solicitudBiometrica = if (activar) "activar" else "abrir"
        activity.lifecycleScope.launch {
            activity.lifecycle.withResumed {
                sesion.promptBiometricoLanzado = true
                prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                    .setTitle(if (activar) "Activar huella en OndaVital" else "Abrir OndaVital")
                    .setSubtitle(if (activar) "Confirma con la huella registrada en tu teléfono" else "Tus recuerdos te esperan")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText(if (activar) "Ahora no" else "Usar PIN").build())
            }
        }
    }

    fun reanudarPendiente() {
        if (sesion.solicitudBiometrica != null && !sesion.promptBiometricoLanzado)
            solicitar(activar = sesion.solicitudBiometrica == "activar")
    }

    companion object {
        fun mensajeDisponibilidad(codigo: Int): String = when (codigo) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Tu teléfono tiene una huella o biometría compatible."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Primero registra una huella y un bloqueo de pantalla en los ajustes del teléfono."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "El lector no está disponible ahora. Puedes volver a comprobarlo."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Android pide una actualización de seguridad para utilizar esta biometría."
            else -> "Android no ofrece una biometría compatible en este momento. Revisa los ajustes o vuelve a comprobarlo."
        }
    }
}
