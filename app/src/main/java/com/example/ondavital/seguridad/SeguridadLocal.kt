package com.example.ondavital.seguridad

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

/** Acceso local. El PIN se comprueba fuera del hilo de UI; nunca se guarda en claro. */
class SeguridadLocal(context: Context) {
    private val preferencias = context.getSharedPreferences("ondavital_seguridad", Context.MODE_PRIVATE)
    private val aleatorio = SecureRandom()

    val hayPin: Boolean get() = preferencias.contains("hash") || preferencias.contains("sal")
    val biometria: Boolean get() = hayPin && preferencias.getBoolean("biometria", false)

    fun esperaRestante(): Long =
        (preferencias.getLong("bloqueadoHasta", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    /** Devuelve false también ante preferencias dañadas: el acceso falla cerrado. */
    fun verificar(pin: String): Boolean {
        if (esperaRestante() > 0L) return false
        val sal = try { Base64.decode(preferencias.getString("sal", ""), Base64.NO_WRAP) } catch (_: Exception) { byteArrayOf() }
        val esperado = try { Base64.decode(preferencias.getString("hash", ""), Base64.NO_WRAP) } catch (_: Exception) { byteArrayOf() }
        val rondas = preferencias.getInt("rondas", PinDerivacion.RONDAS)
        val correcto = try { PinDerivacion.coincide(pin, sal, esperado, rondas) } catch (_: Exception) { false }
        if (correcto) {
            preferencias.edit().putInt("fallos", 0).putLong("bloqueadoHasta", 0L).commit()
        } else if (hayPin) {
            val fallos = preferencias.getInt("fallos", 0) + 1
            val espera = when {
                fallos >= 15 -> 30 * 60_000L
                fallos >= 10 -> 5 * 60_000L
                fallos >= 5 -> 30_000L
                else -> 0L
            }
            preferencias.edit().putInt("fallos", fallos)
                .putLong("bloqueadoHasta", if (espera == 0L) 0L else System.currentTimeMillis() + espera)
                .commit()
        }
        return correcto
    }

    fun crear(pin: String): Boolean {
        if (hayPin || !pinValido(pin)) return false
        val sal = ByteArray(16).also(aleatorio::nextBytes)
        val hash = PinDerivacion.derivar(pin, sal)
        return preferencias.edit().putString("sal", Base64.encodeToString(sal, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt("rondas", PinDerivacion.RONDAS).putBoolean("biometria", false)
            .putInt("fallos", 0).putLong("bloqueadoHasta", 0L).commit()
    }

    fun cambiar(actual: String, nuevo: String): Boolean {
        if (!pinValido(nuevo) || !verificar(actual)) return false
        val sal = ByteArray(16).also(aleatorio::nextBytes)
        val hash = PinDerivacion.derivar(nuevo, sal)
        return preferencias.edit().putString("sal", Base64.encodeToString(sal, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt("rondas", PinDerivacion.RONDAS).putInt("fallos", 0)
            .putLong("bloqueadoHasta", 0L).commit()
    }

    fun quitar(actual: String): Boolean = verificar(actual) && preferencias.edit().clear().commit()
    fun guardarBiometria(habilitar: Boolean): Boolean = hayPin && preferencias.edit().putBoolean("biometria", habilitar).commit()

    private fun pinValido(pin: String) = pin.length == 4 && pin.all { it in '0'..'9' }
}
