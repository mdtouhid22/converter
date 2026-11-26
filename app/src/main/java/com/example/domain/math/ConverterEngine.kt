package com.example.domain.math

import com.example.domain.model.ConversionResult
import com.example.domain.model.NumberSystem
import com.example.domain.model.ValidationResult
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object MathUnicode {
    private val superscriptMap = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '-' to '⁻', '+' to '⁺'
    )

    private val subscriptMap = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉'
    )

    fun toSuperscript(value: Int): String =
        value.toString().map { superscriptMap[it] ?: it }.joinToString("")

    fun toSubscript(value: Int): String =
        value.toString().map { subscriptMap[it] ?: it }.joinToString("")

    fun formatWithBase(number: String, system: NumberSystem): String =
        "($number)${system.subscript}"
}

class ConverterEngine {

    fun validateInput(rawInput: String, system: NumberSystem): ValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Please enter a ${system.displayName} number.")
        }

        var working = trimmed
        var isNegative = false
        if (working.startsWith("-")) {
            isNegative = true
            working = working.substring(1)
        } else if (working.startsWith("+")) {
            working = working.substring(1)
        }

        if (working.isEmpty()) {
            return ValidationResult.Invalid("Enter digits after the sign.")
        }

        if (working.contains('+') || working.contains('-')) {
            return ValidationResult.Invalid("Sign (+/-) is only allowed at the beginning.")
        }

        val dotCount = working.count { it == '.' }
        if (dotCount > 1) {
            return ValidationResult.Invalid("Only one decimal point (.) is allowed.")
        }

        val parts = working.split('.')
        val rawInt = parts[0]
        val rawFrac = if (parts.size > 1) parts[1] else ""

        if (parts.size > 1 && rawFrac.isEmpty()) {
            return ValidationResult.Invalid("Enter at least one fractional digit after '.'.")
        }
        if (rawInt.isEmpty() && rawFrac.isEmpty()) {
            return ValidationResult.Invalid("Please enter a valid ${system.displayName} number.")
        }

        val invalidChars = (rawInt + rawFrac)
            .filter { !system.isValidDigitChar(it) }
            .toList()
            .distinct()

        if (invalidChars.isNotEmpty()) {
            val formatted = invalidChars.joinToString(", ") { "'$it'" }
            return ValidationResult.Invalid(
                "Invalid character $formatted for ${system.displayName} (allowed: ${system.allowedDigitsLabel})."
            )
        }

        val normalizedInt = rawInt.uppercase().trimStart('0').ifEmpty { "0" }
        val normalizedFrac = rawFrac.uppercase()

        val allZero = normalizedInt == "0" && normalizedFrac.all { it == '0' }
        val effectiveNegative = if (allZero) false else isNegative

        val normalizedInput = buildString {
            if (effectiveNegative) append('-')
            append(normalizedInt)
            if (normalizedFrac.isNotEmpty()) {
                append('.')
                append(normalizedFrac)
            }
        }

        return ValidationResult.Valid(
            normalizedInput = normalizedInput,
            isNegative = effectiveNegative,
            integerPart = normalizedInt,
            fractionPart = normalizedFrac
        )
    }

    /**
     * Quick live validation while the user types (allows incomplete trailing '.' without nagging
     * unless there are invalid characters or malformed structure).
     */
    fun validateLiveInput(rawInput: String, system: NumberSystem): String? {
        if (rawInput.isEmpty()) return null
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        var working = trimmed
        if (working.startsWith("-") || working.startsWith("+")) {
            working = working.substring(1)
        }
        if (working.isEmpty()) return null
        if (working.contains('+') || working.contains('-')) {
            return "Sign (+/-) is only allowed at the beginning."
        }
        if (working.count { it == '.' } > 1) {
            return "Only one decimal point (.) is allowed."
        }
        val invalidChars = working.filter { it != '.' && !system.isValidDigitChar(it) }
            .toList()
            .distinct()
        if (invalidChars.isNotEmpty()) {
            val formatted = invalidChars.joinToString(", ") { "'$it'" }
            return "Invalid digit $formatted for ${system.displayName} (allowed: ${system.allowedDigitsLabel})."
        }
        return null
    }

    fun convert(
        rawInput: String,
        source: NumberSystem,
        target: NumberSystem,
        decimalPrecision: Int = 8,
        maxFractionDigits: Int = 12
    ): Result<ConversionResult> {
        val valid = when (val validation = validateInput(rawInput, source)) {
            is ValidationResult.Invalid -> return Result.failure(IllegalArgumentException(validation.message))
            is ValidationResult.Valid -> validation
        }

        val precisionLimit = if (target == NumberSystem.DECIMAL) {
            maxFractionDigits.coerceIn(4, 32)
        } else {
            decimalPrecision.coerceIn(2, maxFractionDigits.coerceIn(4, 32))
        }

        // Convert integer part to exact BigInteger
        val intValue = parseIntegerInBase(valid.integerPart, source.base)

        // Convert fraction part to exact rational fracNum / fracDen
        val (fracNum, fracDen) = parseFractionRational(valid.fractionPart, source.base)

        // Compute target integer string
        val targetIntStr = formatIntegerInBase(intValue, target.base)

        // Compute target fraction string using exact rational multiplication
        val fracConversion = convertFractionRationalToBase(
            fracNum = fracNum,
            fracDen = fracDen,
            targetBase = target.base,
            maxDigits = precisionLimit
        )

        val trimmedFrac = fracConversion.digits.trimEnd('0')
        val convertedUnsigned = buildString {
            append(targetIntStr)
            if (trimmedFrac.isNotEmpty()) {
                append('.')
                append(trimmedFrac)
            }
        }
        val convertedValue = if (valid.isNegative && convertedUnsigned != "0") {
            "-${convertedUnsigned}"
        } else {
            convertedUnsigned
        }

        // Compute exact intermediate decimal value (always terminating because bases are 2, 8, 10, 16)
        val exactDecimalUnsigned = computeExactDecimalString(intValue, fracNum, fracDen)
        val exactDecimalSigned = if (valid.isNegative && exactDecimalUnsigned != "0") {
            "-$exactDecimalUnsigned"
        } else {
            exactDecimalUnsigned
        }

        val explanation = ExplanationEngine.generate(
            valid = valid,
            source = source,
            target = target,
            convertedValue = convertedValue,
            exactDecimalUnsigned = exactDecimalUnsigned,
            exactDecimalSigned = exactDecimalSigned,
            isExact = fracConversion.isExact,
            precisionLimitUsed = precisionLimit,
            rawFractionDigits = fracConversion.digits,
            intValue = intValue,
            fracNum = fracNum,
            fracDen = fracDen
        )

        return Result.success(
            ConversionResult(
                rawInput = rawInput,
                normalizedInput = valid.normalizedInput,
                sourceSystem = source,
                targetSystem = target,
                convertedValue = convertedValue,
                isNegative = valid.isNegative,
                isExact = fracConversion.isExact,
                precisionLimitUsed = precisionLimit,
                explanation = explanation
            )
        )
    }

    internal data class FractionConversion(
        val digits: String,
        val isExact: Boolean
    )

    internal fun parseIntegerInBase(integerPart: String, base: Int): BigInteger {
        if (integerPart.isEmpty() || integerPart == "0") return BigInteger.ZERO
        var acc = BigInteger.ZERO
        val b = BigInteger.valueOf(base.toLong())
        for (ch in integerPart) {
            val d = NumberSystem.fromBase(base).charToDigit(ch) ?: 0
            acc = acc.multiply(b).add(BigInteger.valueOf(d.toLong()))
        }
        return acc
    }

    internal fun parseFractionRational(fractionPart: String, base: Int): Pair<BigInteger, BigInteger> {
        val trimmed = fractionPart.trimEnd('0')
        if (trimmed.isEmpty()) return BigInteger.ZERO to BigInteger.ONE
        val b = BigInteger.valueOf(base.toLong())
        var num = BigInteger.ZERO
        var den = BigInteger.ONE
        val sys = NumberSystem.fromBase(base)
        for (ch in trimmed) {
            val d = sys.charToDigit(ch) ?: 0
            num = num.multiply(b).add(BigInteger.valueOf(d.toLong()))
            den = den.multiply(b)
        }
        val gcd = num.gcd(den)
        return if (gcd > BigInteger.ONE) {
            num.divide(gcd) to den.divide(gcd)
        } else {
            num to den
        }
    }

    internal fun formatIntegerInBase(value: BigInteger, base: Int): String {
        if (value == BigInteger.ZERO) return "0"
        val sys = NumberSystem.fromBase(base)
        val b = BigInteger.valueOf(base.toLong())
        var cur = value
        val sb = StringBuilder()
        while (cur > BigInteger.ZERO) {
            val (q, r) = cur.divideAndRemainder(b)
            sb.append(sys.digitToChar(r.toInt()))
            cur = q
        }
        return sb.reverse().toString()
    }

    internal fun convertFractionRationalToBase(
        fracNum: BigInteger,
        fracDen: BigInteger,
        targetBase: Int,
        maxDigits: Int
    ): FractionConversion {
        if (fracNum == BigInteger.ZERO) {
            return FractionConversion(digits = "", isExact = true)
        }
        val sys = NumberSystem.fromBase(targetBase)
        val b = BigInteger.valueOf(targetBase.toLong())
        var rem = fracNum
        val sb = StringBuilder()
        var steps = 0
        while (rem > BigInteger.ZERO && steps < maxDigits) {
            val prod = rem.multiply(b)
            val (digit, nextRem) = prod.divideAndRemainder(fracDen)
            sb.append(sys.digitToChar(digit.toInt()))
            rem = nextRem
            steps++
        }
        return FractionConversion(
            digits = sb.toString(),
            isExact = (rem == BigInteger.ZERO)
        )
    }

    /**
     * Exact terminating decimal representation for any fraction whose denominator only has factors 2 and 5.
     */
    internal fun computeExactDecimalString(
        intValue: BigInteger,
        fracNum: BigInteger,
        fracDen: BigInteger
    ): String {
        if (fracNum == BigInteger.ZERO) return intValue.toString()
        // Determine exact decimal scale needed: max(power of 2, power of 5) in fracDen
        var d = fracDen
        var twos = 0
        var fives = 0
        val two = BigInteger.valueOf(2)
        val five = BigInteger.valueOf(5)
        while (d.mod(two) == BigInteger.ZERO) {
            d = d.divide(two)
            twos++
        }
        while (d.mod(five) == BigInteger.ZERO) {
            d = d.divide(five)
            fives++
        }
        val scale = maxOf(twos, fives)
        val decInt = BigDecimal(intValue)
        val decFrac = if (d == BigInteger.ONE) {
            BigDecimal(fracNum).divide(BigDecimal(fracDen), scale, RoundingMode.UNNECESSARY)
        } else {
            BigDecimal(fracNum).divide(BigDecimal(fracDen), 32, RoundingMode.HALF_UP)
        }
        val combined = decInt.add(decFrac).stripTrailingZeros()
        return combined.toPlainString()
    }
}
