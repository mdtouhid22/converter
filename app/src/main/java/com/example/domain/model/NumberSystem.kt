package com.example.domain.model

enum class NumberSystem(
    val base: Int,
    val displayName: String,
    val shortLabel: String,
    val subscript: String,
    val allowedDigitsLabel: String
) {
    BINARY(
        base = 2,
        displayName = "Binary",
        shortLabel = "BIN",
        subscript = "₂",
        allowedDigitsLabel = "0–1"
    ),
    DECIMAL(
        base = 10,
        displayName = "Decimal",
        shortLabel = "DEC",
        subscript = "₁₀",
        allowedDigitsLabel = "0–9"
    ),
    OCTAL(
        base = 8,
        displayName = "Octal",
        shortLabel = "OCT",
        subscript = "₈",
        allowedDigitsLabel = "0–7"
    ),
    HEXADECIMAL(
        base = 16,
        displayName = "Hexadecimal",
        shortLabel = "HEX",
        subscript = "₁₆",
        allowedDigitsLabel = "0–9, A–F"
    );

    fun charToDigit(ch: Char): Int? {
        val upper = ch.uppercaseChar()
        val value = when (upper) {
            in '0'..'9' -> upper - '0'
            in 'A'..'F' -> 10 + (upper - 'A')
            else -> return null
        }
        return if (value < base) value else null
    }

    fun digitToChar(digit: Int): Char {
        require(digit in 0 until base) { "Digit $digit out of range for base $base" }
        return if (digit < 10) ('0' + digit) else ('A' + (digit - 10))
    }

    fun isValidDigitChar(ch: Char): Boolean = charToDigit(ch) != null

    companion object {
        fun fromBase(base: Int): NumberSystem =
            entries.firstOrNull { it.base == base } ?: DECIMAL
    }
}
