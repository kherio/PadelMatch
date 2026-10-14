package com.kherio.padelmatch.data

/**
 * Reglas de entrada de marcadores, sin dependencias de Compose para poder
 * probarlas aparte.
 *
 * En el americano los dos marcadores de un partido siempre suman la
 * puntuación objetivo del torneo, así que al fijar el de un equipo el del
 * otro se completa solo.
 */
object ScoreInput {

    /** Resultado de pulsar una tecla del teclado numérico. */
    data class KeypadResult(
        /** Dígitos que se muestran mientras el valor aún puede alargarse. */
        val digits: String,
        /** Marcador ya definitivo (se cierra el teclado), o null si hay que seguir. */
        val confirmed: Int?
    )

    /** Marcadores (ese equipo, el rival) tras fijar [value] para un equipo. */
    fun complementary(value: Int, target: Int): Pair<Int, Int> {
        val v = value.coerceIn(0, target)
        return v to (target - v)
    }

    /** Valor tras pulsar + o −; sin marcador previo se parte de 0. */
    fun step(current: Int?, delta: Int, target: Int): Int =
        ((current ?: 0) + delta).coerceIn(0, target)

    /**
     * Teclado numérico rápido: en cuanto el valor ya no puede alargarse con
     * otro dígito sin pasarse de la puntuación objetivo, se confirma solo.
     * Con objetivo 21: "5" -> 5 al instante, "1" espera (puede ser 1, 10..19),
     * "2" espera (2 o 20, 21), "0" -> 0 al instante.
     */
    fun appendDigit(digits: String, digit: Int, target: Int): KeypadResult {
        val maxDigits = target.toString().length
        val next = if (digits.length >= maxDigits) digit.toString() else digits + digit
        if (next == "0") return KeypadResult(next, 0)
        val value = next.toInt()
        val done = next.length >= maxDigits || value * 10 > target
        return if (done) KeypadResult(next, value.coerceAtMost(target))
        else KeypadResult(next, null)
    }
}
