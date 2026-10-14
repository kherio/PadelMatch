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

    /** Marcadores (ese equipo, el rival) tras fijar [value] para un equipo. */
    fun complementary(value: Int, target: Int): Pair<Int, Int> {
        val v = value.coerceIn(0, target)
        return v to (target - v)
    }

    /** Valor tras pulsar + o −; sin marcador previo se parte de 0. */
    fun step(current: Int?, delta: Int, target: Int): Int =
        ((current ?: 0) + delta).coerceIn(0, target)

    /**
     * Interpreta el texto del campo numérico. Devuelve null si está vacío.
     *
     * Solo cuentan los dígitos. Si el texto ya tenía el máximo de dígitos y se
     * teclea otro más, se empieza un número nuevo con lo tecleado (así, con
     * "17" en pantalla, teclear un 5 deja "5" en lugar de obligar a borrar
     * antes). Nunca se pasa de la puntuación objetivo.
     */
    fun parse(raw: String, target: Int): Int? {
        val maxDigits = target.toString().length
        var digits = raw.filter { it.isDigit() }
        if (digits.length > maxDigits) digits = digits.drop(maxDigits).take(maxDigits)
        val value = digits.toIntOrNull() ?: return null
        return value.coerceAtMost(target)
    }
}
