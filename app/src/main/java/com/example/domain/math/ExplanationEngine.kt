package com.example.domain.math

import com.example.domain.model.ConversionExplanation
import com.example.domain.model.ExplanationStep
import com.example.domain.model.NumberSystem
import com.example.domain.model.ValidationResult
import java.math.BigDecimal
import java.math.BigInteger

internal object ExplanationEngine {

    fun generate(
        valid: ValidationResult.Valid,
        source: NumberSystem,
        target: NumberSystem,
        convertedValue: String,
        exactDecimalUnsigned: String,
        exactDecimalSigned: String,
        isExact: Boolean,
        precisionLimitUsed: Int,
        rawFractionDigits: String,
        intValue: BigInteger,
        fracNum: BigInteger,
        fracDen: BigInteger
    ): ConversionExplanation {
        val problemExpr = "${MathUnicode.formatWithBase(valid.normalizedInput, source)} = (?)${target.subscript}"
        val solvedExpr = "${MathUnicode.formatWithBase(valid.normalizedInput, source)} = ${MathUnicode.formatWithBase(convertedValue, target)}"
        val unsignedTarget = convertedValue.removePrefix("-")

        val precisionNotice = if (!isExact) {
            "Non-terminating fraction in ${target.displayName}: computed to $precisionLimitUsed fractional digits without silent rounding."
        } else if (valid.fractionPart.isNotEmpty() && rawFractionDigits.length >= precisionLimitUsed && target == NumberSystem.DECIMAL) {
            // Check if exact decimal had more digits than precisionLimitUsed
            val fullFracDigits = exactDecimalUnsigned.substringAfter('.', "")
            if (fullFracDigits.length > precisionLimitUsed) {
                "Truncated to $precisionLimitUsed fractional digits (exact ${target.displayName} value has ${fullFracDigits.length} fractional digits)."
            } else null
        } else null

        if (source == target) {
            val step1 = ExplanationStep(
                stepNumber = 1,
                title = "Same Number System (Identity)",
                formula = "(N)${source.subscript} = (N)${target.subscript}",
                description = "Source and target are both ${source.displayName} (Base ${source.base}), so the value remains unchanged after normalization.",
                calculationLines = listOf(
                    "Input: ${MathUnicode.formatWithBase(valid.normalizedInput, source)}",
                    "Output: ${MathUnicode.formatWithBase(convertedValue, target)}"
                ),
                resultHighlight = MathUnicode.formatWithBase(convertedValue, target)
            )
            return ConversionExplanation(
                problemExpression = problemExpr,
                solvedExpression = solvedExpr,
                sourceSystem = source,
                targetSystem = target,
                methodTitle = "Direct Identity (Base ${source.base})",
                introduction = "Since both the source and target systems use Base ${source.base}, no radix conversion is needed.",
                steps = listOf(step1),
                finalAnswer = solvedExpr,
                verificationNote = "Decimal equivalent: ${MathUnicode.formatWithBase(exactDecimalSigned, NumberSystem.DECIMAL)}",
                precisionNotice = precisionNotice
            )
        }

        return when {
            source != NumberSystem.DECIMAL && target == NumberSystem.DECIMAL ->
                explainToDecimal(
                    valid = valid,
                    source = source,
                    convertedValue = convertedValue,
                    unsignedTarget = unsignedTarget,
                    exactDecimalSigned = exactDecimalSigned,
                    problemExpr = problemExpr,
                    solvedExpr = solvedExpr,
                    precisionNotice = precisionNotice
                )

            source == NumberSystem.DECIMAL && target != NumberSystem.DECIMAL ->
                explainFromDecimal(
                    valid = valid,
                    target = target,
                    convertedValue = convertedValue,
                    intValue = intValue,
                    fracNum = fracNum,
                    fracDen = fracDen,
                    precisionLimitUsed = precisionLimitUsed,
                    isExact = isExact,
                    exactDecimalSigned = exactDecimalSigned,
                    problemExpr = problemExpr,
                    solvedExpr = solvedExpr,
                    precisionNotice = precisionNotice
                )

            source == NumberSystem.BINARY && (target == NumberSystem.OCTAL || target == NumberSystem.HEXADECIMAL) ->
                explainBinaryGrouping(
                    valid = valid,
                    target = target,
                    convertedValue = convertedValue,
                    exactDecimalSigned = exactDecimalSigned,
                    problemExpr = problemExpr,
                    solvedExpr = solvedExpr,
                    precisionNotice = precisionNotice
                )

            (source == NumberSystem.OCTAL || source == NumberSystem.HEXADECIMAL) && target == NumberSystem.BINARY ->
                explainToBinaryExpansion(
                    valid = valid,
                    source = source,
                    convertedValue = convertedValue,
                    exactDecimalSigned = exactDecimalSigned,
                    problemExpr = problemExpr,
                    solvedExpr = solvedExpr,
                    precisionNotice = precisionNotice
                )

            else ->
                // Octal <-> Hexadecimal via Binary
                explainOctalHexViaBinary(
                    valid = valid,
                    source = source,
                    target = target,
                    convertedValue = convertedValue,
                    exactDecimalSigned = exactDecimalSigned,
                    problemExpr = problemExpr,
                    solvedExpr = solvedExpr,
                    precisionNotice = precisionNotice
                )
        }
    }

    private fun explainToDecimal(
        valid: ValidationResult.Valid,
        source: NumberSystem,
        convertedValue: String,
        unsignedTarget: String,
        exactDecimalSigned: String,
        problemExpr: String,
        solvedExpr: String,
        precisionNotice: String?
    ): ConversionExplanation {
        val base = source.base
        val baseBd = BigDecimal.valueOf(base.toLong())
        val intDigits = valid.integerPart
        val fracDigits = valid.fractionPart
        val steps = mutableListOf<ExplanationStep>()
        val unsignedInput = valid.normalizedInput.removePrefix("-")

        val termExpressions = mutableListOf<String>()
        val termValues = mutableListOf<String>()
        val letterMappings = mutableListOf<String>()

        for (i in intDigits.indices) {
            val ch = intDigits[i]
            val digit = source.charToDigit(ch) ?: 0
            val power = intDigits.length - 1 - i
            if (source == NumberSystem.HEXADECIMAL && ch in 'A'..'F') {
                letterMappings.add("$ch = $digit")
            }
            termExpressions.add("($digit × $base${MathUnicode.toSuperscript(power)})")
            val termVal = BigDecimal.valueOf(digit.toLong()).multiply(baseBd.pow(power))
            termValues.add(termVal.stripTrailingZeros().toPlainString())
        }

        for (j in fracDigits.indices) {
            val ch = fracDigits[j]
            val digit = source.charToDigit(ch) ?: 0
            val power = -(j + 1)
            if (source == NumberSystem.HEXADECIMAL && ch in 'A'..'F') {
                letterMappings.add("$ch = $digit")
            }
            termExpressions.add("($digit × $base${MathUnicode.toSuperscript(power)})")
            val weight = BigDecimal.ONE.divide(baseBd.pow(-power))
            val termVal = BigDecimal.valueOf(digit.toLong()).multiply(weight)
            termValues.add(termVal.stripTrailingZeros().toPlainString())
        }

        val step1Lines = mutableListOf<String>()
        if (letterMappings.isNotEmpty()) {
            step1Lines.add("Hex digit values: ${letterMappings.distinct().joinToString(", ")}")
        }
        step1Lines.add("($unsignedInput)${source.subscript}")
        step1Lines.add("= ${termExpressions.joinToString(" + ")}")

        steps.add(
            ExplanationStep(
                stepNumber = 1,
                title = "Write Positional Weight Expansion",
                formula = "Value = Σ (digit × ${base}ⁱ)",
                description = "Multiply each ${source.displayName} digit by ${base} raised to its positional power (starting at ${base}⁰ before the decimal point).",
                calculationLines = step1Lines
            )
        )

        val step2Lines = mutableListOf<String>()
        step2Lines.add("= ${termValues.joinToString(" + ")}")
        step2Lines.add("= $unsignedTarget")
        if (valid.isNegative && convertedValue != "0") {
            step2Lines.add("Apply negative sign: -$unsignedTarget")
        }

        steps.add(
            ExplanationStep(
                stepNumber = 2,
                title = "Evaluate Powers and Sum Terms",
                description = "Compute each positional term and add them together to obtain the Decimal value.",
                calculationLines = step2Lines,
                resultHighlight = MathUnicode.formatWithBase(convertedValue, NumberSystem.DECIMAL)
            )
        )

        return ConversionExplanation(
            problemExpression = problemExpr,
            solvedExpression = solvedExpr,
            sourceSystem = source,
            targetSystem = NumberSystem.DECIMAL,
            methodTitle = "Positional Expansion Method (Base $base → Base 10)",
            introduction = "To convert a ${source.displayName} number to Decimal, expand each digit using powers of its base ($base) and sum the resulting values.",
            steps = steps,
            finalAnswer = solvedExpr,
            verificationNote = "Exact check: positional sum equals ${MathUnicode.formatWithBase(exactDecimalSigned, NumberSystem.DECIMAL)}.",
            precisionNotice = precisionNotice
        )
    }

    private fun explainFromDecimal(
        valid: ValidationResult.Valid,
        target: NumberSystem,
        convertedValue: String,
        intValue: BigInteger,
        fracNum: BigInteger,
        fracDen: BigInteger,
        precisionLimitUsed: Int,
        isExact: Boolean,
        exactDecimalSigned: String,
        problemExpr: String,
        solvedExpr: String,
        precisionNotice: String?
    ): ConversionExplanation {
        val base = target.base
        val bBig = BigInteger.valueOf(base.toLong())
        val steps = mutableListOf<ExplanationStep>()
        var stepCounter = 1

        // Step 1: Integer Part (Repeated Division)
        val intLines = mutableListOf<String>()
        val remainders = mutableListOf<Char>()
        if (intValue == BigInteger.ZERO) {
            intLines.add("0 ÷ $base = 0  remainder  0")
            remainders.add('0')
        } else {
            var cur = intValue
            while (cur > BigInteger.ZERO) {
                val (q, r) = cur.divideAndRemainder(bBig)
                val rInt = r.toInt()
                val rChar = target.digitToChar(rInt)
                val hexHint = if (target == NumberSystem.HEXADECIMAL && rInt >= 10) " ($rChar)" else ""
                intLines.add("$cur ÷ $base = $q   remainder $rInt$hexHint")
                remainders.add(rChar)
                cur = q
            }
        }
        val intResult = remainders.reversed().joinToString("")
        intLines.add("Reading remainders bottom-to-top: ($intValue)₁₀ = ($intResult)${target.subscript}")

        steps.add(
            ExplanationStep(
                stepNumber = stepCounter++,
                title = "Convert Integer Part ($intValue) by Repeated Division",
                formula = "Integer ÷ $base → collect remainders bottom-to-top",
                description = "Divide the integer part repeatedly by $base until the quotient becomes 0.",
                calculationLines = intLines,
                resultHighlight = "($intResult)${target.subscript}"
            )
        )

        // Step 2: Fractional Part (if present)
        val unsignedResult = convertedValue.removePrefix("-")
        if (fracNum > BigInteger.ZERO) {
            val fracLines = mutableListOf<String>()
            val collectedDigits = mutableListOf<Char>()
            var rem = fracNum
            var iter = 0
            while (rem > BigInteger.ZERO && iter < precisionLimitUsed) {
                val curDec = BigDecimal(rem).divide(BigDecimal(fracDen)).stripTrailingZeros().toPlainString()
                val prod = rem.multiply(bBig)
                val (digit, nextRem) = prod.divideAndRemainder(fracDen)
                val prodDec = BigDecimal(prod).divide(BigDecimal(fracDen)).stripTrailingZeros().toPlainString()
                val dInt = digit.toInt()
                val dChar = target.digitToChar(dInt)
                val hexHint = if (target == NumberSystem.HEXADECIMAL && dInt >= 10) " ($dChar)" else ""
                fracLines.add("$curDec × $base = $prodDec   →   digit: $dInt$hexHint")
                collectedDigits.add(dChar)
                rem = nextRem
                iter++
            }
            if (isExact) {
                fracLines.add("Fractional remainder reached 0 (exact termination).")
            } else {
                fracLines.add("Stopped at $precisionLimitUsed digits (non-terminating fraction).")
            }
            val fracStr = collectedDigits.joinToString("").trimEnd('0')
            val fracDisplay = if (fracStr.isEmpty()) "0" else "0.$fracStr"
            fracLines.add("Reading digits top-to-bottom: (0.${valid.fractionPart})₁₀ ≈ ($fracDisplay)${target.subscript}")

            steps.add(
                ExplanationStep(
                    stepNumber = stepCounter++,
                    title = "Convert Fractional Part (0.${valid.fractionPart}) by Repeated Multiplication",
                    formula = "Fraction × $base → take integer digit top-to-bottom",
                    description = "Multiply the fractional part repeatedly by $base and record the integer part of each product.",
                    calculationLines = fracLines,
                    resultHighlight = "($fracDisplay)${target.subscript}"
                )
            )
        }

        // Step 3: Combine
        val combineLines = mutableListOf<String>()
        if (fracNum > BigInteger.ZERO) {
            val fracPartOut = unsignedResult.substringAfter('.', "")
            if (fracPartOut.isNotEmpty()) {
                combineLines.add("Integer ($intResult) + Fraction (.$fracPartOut) = ($unsignedResult)${target.subscript}")
            } else {
                combineLines.add("Combined result = ($unsignedResult)${target.subscript}")
            }
        } else {
            combineLines.add("Combined result = ($unsignedResult)${target.subscript}")
        }
        if (valid.isNegative && convertedValue != "0") {
            combineLines.add("Restore negative sign: ($convertedValue)${target.subscript}")
        }

        steps.add(
            ExplanationStep(
                stepNumber = stepCounter,
                title = "Combine Integer and Fractional Parts",
                description = "Join the converted integer and fractional parts to form the final ${target.displayName} number.",
                calculationLines = combineLines,
                resultHighlight = MathUnicode.formatWithBase(convertedValue, target)
            )
        )

        return ConversionExplanation(
            problemExpression = problemExpr,
            solvedExpression = solvedExpr,
            sourceSystem = NumberSystem.DECIMAL,
            targetSystem = target,
            methodTitle = "Repeated Division & Multiplication (Base 10 → Base $base)",
            introduction = "Convert Decimal to ${target.displayName} by dividing the integer part by $base and multiplying any fractional part by $base.",
            steps = steps,
            finalAnswer = solvedExpr,
            verificationNote = if (isExact) {
                "Verified: expanding ${MathUnicode.formatWithBase(convertedValue, target)} by powers of $base yields ${MathUnicode.formatWithBase(exactDecimalSigned, NumberSystem.DECIMAL)}."
            } else {
                "Approximation check: truncated after $precisionLimitUsed fractional digits; original Decimal value is ${MathUnicode.formatWithBase(exactDecimalSigned, NumberSystem.DECIMAL)}."
            },
            precisionNotice = precisionNotice
        )
    }

    private fun explainBinaryGrouping(
        valid: ValidationResult.Valid,
        target: NumberSystem,
        convertedValue: String,
        exactDecimalSigned: String,
        problemExpr: String,
        solvedExpr: String,
        precisionNotice: String?
    ): ConversionExplanation {
        val groupSize = if (target == NumberSystem.OCTAL) 3 else 4
        val weightsLabel = if (groupSize == 3) "4-2-1" else "8-4-2-1"

        val intPadNeeded = (groupSize - (valid.integerPart.length % groupSize)) % groupSize
        val paddedInt = "0".repeat(intPadNeeded) + valid.integerPart
        val intGroups = paddedInt.chunked(groupSize)

        val hasFrac = valid.fractionPart.isNotEmpty()
        val fracPadNeeded = if (hasFrac) (groupSize - (valid.fractionPart.length % groupSize)) % groupSize else 0
        val paddedFrac = if (hasFrac) valid.fractionPart + "0".repeat(fracPadNeeded) else ""
        val fracGroups = if (hasFrac) paddedFrac.chunked(groupSize) else emptyList()

        val step1Lines = mutableListOf<String>()
        step1Lines.add("Integer part (${valid.integerPart}) → pad left with ${intPadNeeded} zero(s): ${intGroups.joinToString(" ")}")
        if (hasFrac) {
            step1Lines.add("Fractional part (.${valid.fractionPart}) → pad right with ${fracPadNeeded} zero(s): .${fracGroups.joinToString(" ")}")
        }

        val step1 = ExplanationStep(
            stepNumber = 1,
            title = "Group Binary Digits into Groups of $groupSize",
            formula = "$groupSize Binary bits = 1 ${target.displayName} digit (${if (groupSize == 3) "2³ = 8" else "2⁴ = 16"})",
            description = "Group integer bits from right to left and fractional bits from left to right into $groupSize-bit blocks.",
            calculationLines = step1Lines
        )

        val step2Lines = mutableListOf<String>()
        val intMapped = intGroups.map { grp ->
            val v = grp.toInt(2)
            val d = target.digitToChar(v)
            step2Lines.add("Integer group $grp  =  $v${if (target == NumberSystem.HEXADECIMAL && v >= 10) " ($d)" else ""}")
            d
        }.joinToString("").trimStart('0').ifEmpty { "0" }

        val fracMapped = if (hasFrac) {
            fracGroups.map { grp ->
                val v = grp.toInt(2)
                val d = target.digitToChar(v)
                step2Lines.add("Fraction group $grp =  $v${if (target == NumberSystem.HEXADECIMAL && v >= 10) " ($d)" else ""}")
                d
            }.joinToString("").trimEnd('0')
        } else ""

        val unsignedResult = if (fracMapped.isNotEmpty()) "$intMapped.$fracMapped" else intMapped
        val step2 = ExplanationStep(
            stepNumber = 2,
            title = "Convert Each $groupSize-Bit Group Using $weightsLabel Weights",
            description = "Evaluate every $groupSize-bit block to its ${target.displayName} digit.",
            calculationLines = step2Lines,
            resultHighlight = "($unsignedResult)${target.subscript}"
        )

        val step3Lines = mutableListOf<String>()
        step3Lines.add("Combine groups: ($unsignedResult)${target.subscript}")
        if (valid.isNegative && convertedValue != "0") {
            step3Lines.add("Apply negative sign: ($convertedValue)${target.subscript}")
        }
        val step3 = ExplanationStep(
            stepNumber = 3,
            title = "Assemble Final ${target.displayName} Number",
            description = "Concatenate the converted digits around the decimal point.",
            calculationLines = step3Lines,
            resultHighlight = MathUnicode.formatWithBase(convertedValue, target)
        )

        return ConversionExplanation(
            problemExpression = problemExpr,
            solvedExpression = solvedExpr,
            sourceSystem = NumberSystem.BINARY,
            targetSystem = target,
            methodTitle = "Direct $groupSize-Bit Grouping Method (Base 2 → Base ${target.base})",
            introduction = "Because ${target.base} = 2${MathUnicode.toSuperscript(groupSize)}, each group of $groupSize binary bits corresponds directly to one ${target.displayName} digit.",
            steps = listOf(step1, step2, step3),
            finalAnswer = solvedExpr,
            verificationNote = "Decimal equivalence check: both ${MathUnicode.formatWithBase(valid.normalizedInput, NumberSystem.BINARY)} and ${MathUnicode.formatWithBase(convertedValue, target)} equal ${MathUnicode.formatWithBase(exactDecimalSigned, NumberSystem.DECIMAL)}.",
            precisionNotice = precisionNotice
        )
    }

    private fun explainToBinaryExpansion(
        valid: ValidationResult.Valid,
        source: NumberSystem,
        convertedValue: String,
        exactDecimalSigned: String,
        problemExpr: String,
        solvedExpr: String,
        precisionNotice: String?
    ): ConversionExplanation {
        val groupSize = if (source == NumberSystem.OCTAL) 3 else 4
        val step1Lines = mutableListOf<String>()

        val intGroups = valid.integerPart.map { ch ->
            val v = source.charToDigit(ch) ?: 0
            val bits = v.toString(2).padStart(groupSize, '0')
            step1Lines.add("Integer digit $ch${if (source == NumberSystem.HEXADECIMAL && v >= 10) " ($v)" else ""}  →  $bits")
            bits
        }
        val fracGroups = valid.fractionPart.map { ch ->
            val v = source.charToDigit(ch) ?: 0
            val bits = v.toString(2).padStart(groupSize, '0')
            step1Lines.add("Fraction digit $ch${if (source == NumberSystem.HEXADECIMAL && v >= 10) " ($v)" else ""} →  $bits")
            bits
        }

        val step1 = ExplanationStep(
            stepNumber = 1,
            title = "Expand Each ${source.displayName} Digit to $groupSize Binary Bits",
            formula = "1 ${source.displayName} digit = $groupSize Binary bits",
            description = "Replace every ${source.displayName} digit with its exact $groupSize-bit binary representation.",
            calculationLines = step1Lines
        )

        val joinedInt = intGroups.joinToString("")
        val joinedFrac = fracGroups.joinToString("")
        val trimmedInt = joinedInt.trimStart('0').ifEmpty { "0" }
        val trimmedFrac = joinedFrac.trimEnd('0')

        val step2Lines = mutableListOf<String>()
        val rawCombined = if (joinedFrac.isNotEmpty()) "$joinedInt.$joinedFrac" else joinedInt
        step2Lines.add("Joined bits: $rawCombined")
        val cleanUnsigned = if (trimmedFrac.isNotEmpty()) "$trimmedInt.$trimmedFrac" else trimmedInt
        step2Lines.add("Trim extra leading/trailing zeros: ($cleanUnsigned)₂")
        if (valid.isNegative && convertedValue != "0") {
            step2Lines.add("Apply negative sign: ($convertedValue)₂")
        }

        val step2 = ExplanationStep(
            stepNumber = 2,
            title = "Join and Simplify Binary Bits",
            description = "Concatenate the $groupSize-bit blocks and remove unnecessary outer zeros.",
            calculationLines = step2Lines,
            resultHighlight = MathUnicode.formatWithBase(convertedValue, NumberSystem.BINARY)
        )

        return ConversionExplanation(
            problemExpression = problemExpr,
            solvedExpression = solvedExpr,
            sourceSystem = source,
            targetSystem = NumberSystem.BINARY,
            methodTitle = "Direct $groupSize-Bit Expansion Method (Base ${source.base} → Base 2)",
            introduction = "Since ${source.base} = 2${MathUnicode.toSuperscript(groupSize)}, each ${source.displayName} digit expands cleanly into $groupSize binary bits.",
            steps = listOf(step1, step2),
            finalAnswer = solvedExpr,
            verificationNote = "Decimal equivalence check: both ${MathUnicode.formatWithBase(valid.normalizedInput, source)} and ${MathUnicode.formatWithBase(convertedValue, NumberSystem.BINARY)} equal ${MathUnicode.formatWithBase(exactDecimalSigned, NumberSystem.DECIMAL)}.",
            precisionNotice = precisionNotice
        )
    }

    private fun explainOctalHexViaBinary(
        valid: ValidationResult.Valid,
        source: NumberSystem,
        target: NumberSystem,
        convertedValue: String,
        exactDecimalSigned: String,
        problemExpr: String,
        solvedExpr: String,
        precisionNotice: String?
    ): ConversionExplanation {
        val srcGroupSize = if (source == NumberSystem.OCTAL) 3 else 4
        val tgtGroupSize = if (target == NumberSystem.OCTAL) 3 else 4

        val step1Lines = mutableListOf<String>()
        val binIntRaw = valid.integerPart.map { ch ->
            val v = source.charToDigit(ch) ?: 0
            val bits = v.toString(2).padStart(srcGroupSize, '0')
            step1Lines.add("$ch → $bits")
            bits
        }.joinToString("")

        val binFracRaw = valid.fractionPart.map { ch ->
            val v = source.charToDigit(ch) ?: 0
            val bits = v.toString(2).padStart(srcGroupSize, '0')
            step1Lines.add(".$ch → $bits")
            bits
        }.joinToString("")

        val binIntClean = binIntRaw.trimStart('0').ifEmpty { "0" }
        val binFracClean = binFracRaw.trimEnd('0')
        val binaryIntermediate = if (binFracClean.isNotEmpty()) "$binIntClean.$binFracClean" else binIntClean
        step1Lines.add("Binary equivalent: ($binaryIntermediate)₂")

        val step1 = ExplanationStep(
            stepNumber = 1,
            title = "Convert ${source.displayName} to Binary ($srcGroupSize Bits per Digit)",
            formula = "${source.displayName} → Binary (intermediate)",
            description = "First convert each ${source.displayName} digit into $srcGroupSize binary bits.",
            calculationLines = step1Lines,
            resultHighlight = "($binaryIntermediate)₂"
        )

        val intPadNeeded = (tgtGroupSize - (binIntClean.length % tgtGroupSize)) % tgtGroupSize
        val paddedInt = "0".repeat(intPadNeeded) + binIntClean
        val intGroups = paddedInt.chunked(tgtGroupSize)

        val hasFrac = binFracClean.isNotEmpty()
        val fracPadNeeded = if (hasFrac) (tgtGroupSize - (binFracClean.length % tgtGroupSize)) % tgtGroupSize else 0
        val paddedFrac = if (hasFrac) binFracClean + "0".repeat(fracPadNeeded) else ""
        val fracGroups = if (hasFrac) paddedFrac.chunked(tgtGroupSize) else emptyList()

        val step2Lines = mutableListOf<String>()
        step2Lines.add("Integer groups ($tgtGroupSize-bit): ${intGroups.joinToString(" ")}")
        if (hasFrac) {
            step2Lines.add("Fraction groups ($tgtGroupSize-bit): .${fracGroups.joinToString(" ")}")
        }

        val step2 = ExplanationStep(
            stepNumber = 2,
            title = "Regroup Binary Bits into Groups of $tgtGroupSize",
            description = "Pad integer bits on the left and fractional bits on the right with zeros to form $tgtGroupSize-bit groups.",
            calculationLines = step2Lines
        )

        val step3Lines = mutableListOf<String>()
        val intOut = intGroups.map { grp ->
            val v = grp.toInt(2)
            val d = target.digitToChar(v)
            step3Lines.add("$grp → $d${if (target == NumberSystem.HEXADECIMAL && v >= 10) " ($v)" else ""}")
            d
        }.joinToString("").trimStart('0').ifEmpty { "0" }

        val fracOut = if (hasFrac) {
            fracGroups.map { grp ->
                val v = grp.toInt(2)
                val d = target.digitToChar(v)
                step3Lines.add(".$grp → $d${if (target == NumberSystem.HEXADECIMAL && v >= 10) " ($v)" else ""}")
                d
            }.joinToString("").trimEnd('0')
        } else ""

        val unsignedOut = if (fracOut.isNotEmpty()) "$intOut.$fracOut" else intOut
        if (valid.isNegative && convertedValue != "0") {
            step3Lines.add("Apply negative sign: ($convertedValue)${target.subscript}")
        }

        val step3 = ExplanationStep(
            stepNumber = 3,
            title = "Convert Groups to ${target.displayName} Digits",
            description = "Map each $tgtGroupSize-bit block to its ${target.displayName} digit and combine.",
            calculationLines = step3Lines,
            resultHighlight = MathUnicode.formatWithBase(convertedValue, target)
        )

        return ConversionExplanation(
            problemExpression = problemExpr,
            solvedExpression = solvedExpr,
            sourceSystem = source,
            targetSystem = target,
            methodTitle = "Two-Step Conversion via Binary (Base ${source.base} → Base 2 → Base ${target.base})",
            introduction = "Convert ${source.displayName} to ${target.displayName} quickly using Binary as an exact bridge.",
            steps = listOf(step1, step2, step3),
            finalAnswer = solvedExpr,
            verificationNote = "Decimal equivalence check: both ${MathUnicode.formatWithBase(valid.normalizedInput, source)} and ${MathUnicode.formatWithBase(convertedValue, target)} equal ${MathUnicode.formatWithBase(exactDecimalSigned, NumberSystem.DECIMAL)}.",
            precisionNotice = precisionNotice
        )
    }
}
