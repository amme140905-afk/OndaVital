package com.example.ondavital.seguridad

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** SHA-1 aquí es la PRF de PBKDF2 disponible desde el minSdk 24; no cifra el diario. */
internal object PinDerivacion {
    const val RONDAS = 180_000
    fun derivar(pin: String, sal: ByteArray, rondas: Int = RONDAS): ByteArray {
        require(pin.length == 4 && pin.all { it in '0'..'9' } && rondas in 100_000..1_000_000)
        require(sal.size >= 16)
        val caracteres = pin.toCharArray()
        val especificacion = PBEKeySpec(caracteres, sal, rondas, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
            caracteres.fill('\u0000')
        }
    }

    fun coincide(pin: String, sal: ByteArray, esperado: ByteArray, rondas: Int): Boolean =
        esperado.size == 32 && pin.length == 4 && pin.all { it in '0'..'9' } &&
            MessageDigest.isEqual(derivar(pin, sal, rondas), esperado)
}
