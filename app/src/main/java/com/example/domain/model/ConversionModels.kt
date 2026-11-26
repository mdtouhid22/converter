package com.example.domain.model

sealed class ValidationResult {
    data class Valid(
        val normalizedInput: String,
        val isNegative: Boolean,
        val integerPart: String,
        val fractionPart: String
    ) : ValidationResult()

    data class Invalid(
        val message: String
    ) : ValidationResult()
}

data class ExplanationStep(
    val stepNumber: Int,
    val title: String,
    val formula: String? = null,
    val description: String,
    val calculationLines: List<String> = emptyList(),
    val resultHighlight: String? = null
)

data class ConversionExplanation(
    val problemExpression: String,
    val solvedExpression: String,
    val sourceSystem: NumberSystem,
    val targetSystem: NumberSystem,
    val methodTitle: String,
    val introduction: String,
    val steps: List<ExplanationStep>,
    val finalAnswer: String,
    val verificationNote: String,
    val precisionNotice: String? = null
)

data class ConversionResult(
    val rawInput: String,
    val normalizedInput: String,
    val sourceSystem: NumberSystem,
    val targetSystem: NumberSystem,
    val convertedValue: String,
    val isNegative: Boolean,
    val isExact: Boolean,
    val precisionLimitUsed: Int,
    val explanation: ConversionExplanation
)
