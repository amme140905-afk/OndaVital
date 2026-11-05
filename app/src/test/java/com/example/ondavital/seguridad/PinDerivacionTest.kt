package com.example.ondavital.seguridad

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinDerivacionTest {
    @Test fun pinCorrectoYEquivocado() {
        val sal = ByteArray(16) { it.toByte() }
        val hash = PinDerivacion.derivar("4827", sal)
        assertTrue(PinDerivacion.coincide("4827", sal, hash, PinDerivacion.RONDAS))
        assertFalse(PinDerivacion.coincide("4828", sal, hash, PinDerivacion.RONDAS))
    }

    @Test fun salDistintaDaHashDistinto() {
        val primera = PinDerivacion.derivar("1234", ByteArray(16) { 1.toByte() })
        val segunda = PinDerivacion.derivar("1234", ByteArray(16) { 2.toByte() })
        assertFalse(primera.contentEquals(segunda))
    }

    @Test fun rechazaLongitudYNumerosNoAscii() {
        val sal = ByteArray(16)
        val esperado = PinDerivacion.derivar("1234", sal)
        assertFalse(PinDerivacion.coincide("123", sal, esperado, PinDerivacion.RONDAS))
        assertFalse(PinDerivacion.coincide("１２３４", sal, esperado, PinDerivacion.RONDAS))
    }
}
