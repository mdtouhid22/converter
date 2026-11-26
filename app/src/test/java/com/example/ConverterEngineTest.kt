package com.example

import com.example.data.learn.LearnCurriculum
import com.example.domain.math.ConverterEngine
import com.example.domain.model.NumberSystem
import com.example.domain.model.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConverterEngineTest {

    private val engine = ConverterEngine()

    @Test
    fun `binary to decimal and octal exact fractions`() {
        val toDec = engine.convert("1100.11", NumberSystem.BINARY, NumberSystem.DECIMAL).getOrThrow()
        assertEquals("12.75", toDec.convertedValue)
        assertTrue(toDec.isExact)
        assertTrue(toDec.explanation.steps.isNotEmpty())

        val toOct = engine.convert("1100.11", NumberSystem.BINARY, NumberSystem.OCTAL).getOrThrow()
        assertEquals("14.6", toOct.convertedValue)
        assertTrue(toOct.isExact)
        assertTrue(toOct.explanation.methodTitle.contains("3-Bit"))
        assertTrue(toOct.explanation.steps.any { it.calculationLines.any { line -> line.contains("001 100") } })
    }

    @Test
    fun `all 12 conversion pairs produce accurate outputs and explanations`() {
        data class Case(
            val input: String,
            val src: NumberSystem,
            val tgt: NumberSystem,
            val expected: String
        )

        val cases = listOf(
            Case("10111011.1101", NumberSystem.BINARY, NumberSystem.DECIMAL, "187.8125"),
            Case("10111011.1101", NumberSystem.BINARY, NumberSystem.OCTAL, "273.64"),
            Case("10111011.1101", NumberSystem.BINARY, NumberSystem.HEXADECIMAL, "BB.D"),
            Case("187.8125", NumberSystem.DECIMAL, NumberSystem.BINARY, "10111011.1101"),
            Case("187.8125", NumberSystem.DECIMAL, NumberSystem.OCTAL, "273.64"),
            Case("187.8125", NumberSystem.DECIMAL, NumberSystem.HEXADECIMAL, "BB.D"),
            Case("273.64", NumberSystem.OCTAL, NumberSystem.BINARY, "10111011.1101"),
            Case("273.64", NumberSystem.OCTAL, NumberSystem.DECIMAL, "187.8125"),
            Case("273.64", NumberSystem.OCTAL, NumberSystem.HEXADECIMAL, "BB.D"),
            Case("bb.d", NumberSystem.HEXADECIMAL, NumberSystem.BINARY, "10111011.1101"),
            Case("BB.D", NumberSystem.HEXADECIMAL, NumberSystem.DECIMAL, "187.8125"),
            Case("BB.d", NumberSystem.HEXADECIMAL, NumberSystem.OCTAL, "273.64")
        )

        for (c in cases) {
            val res = engine.convert(c.input, c.src, c.tgt).getOrThrow()
            assertEquals("Failed ${c.src} -> ${c.tgt}", c.expected, res.convertedValue)
            assertTrue("Missing steps for ${c.src} -> ${c.tgt}", res.explanation.steps.isNotEmpty())
            assertTrue("Missing final answer for ${c.src} -> ${c.tgt}", res.explanation.finalAnswer.contains(c.expected))
            assertTrue("Missing verification for ${c.src} -> ${c.tgt}", res.explanation.verificationNote.isNotBlank())
        }
    }

    @Test
    fun `negative numbers, leading dot, and negative zero handled accurately`() {
        val negHex = engine.convert("-FF.8", NumberSystem.HEXADECIMAL, NumberSystem.DECIMAL).getOrThrow()
        assertEquals("-255.5", negHex.convertedValue)
        assertTrue(negHex.isNegative)

        val leadDot = engine.convert(".5", NumberSystem.DECIMAL, NumberSystem.BINARY).getOrThrow()
        assertEquals("0.1", leadDot.convertedValue)

        val negZero = engine.convert("-0.0", NumberSystem.BINARY, NumberSystem.DECIMAL).getOrThrow()
        assertEquals("0", negZero.convertedValue)
        assertFalse(negZero.isNegative)
    }

    @Test
    fun `non terminating decimal fraction sets isExact false and provides precision notice`() {
        val res = engine.convert("0.1", NumberSystem.DECIMAL, NumberSystem.BINARY, decimalPrecision = 8).getOrThrow()
        assertEquals("0.00011001", res.convertedValue)
        assertFalse(res.isExact)
        assertNotNull(res.explanation.precisionNotice)
    }

    @Test
    fun `arbitrary precision large integer conversion has zero overflow`() {
        val bigHex = "FFFFFFFFFFFFFFFF"
        val res = engine.convert(bigHex, NumberSystem.HEXADECIMAL, NumberSystem.DECIMAL).getOrThrow()
        assertEquals("18446744073709551615", res.convertedValue)
    }

    @Test
    fun `input validation rejects invalid digits per base`() {
        assertTrue(engine.validateInput("102", NumberSystem.BINARY) is ValidationResult.Invalid)
        assertTrue(engine.validateInput("78", NumberSystem.OCTAL) is ValidationResult.Invalid)
        assertTrue(engine.validateInput("12A", NumberSystem.DECIMAL) is ValidationResult.Invalid)
        assertTrue(engine.validateInput("1G", NumberSystem.HEXADECIMAL) is ValidationResult.Invalid)
        assertTrue(engine.validateInput("12.3.4", NumberSystem.DECIMAL) is ValidationResult.Invalid)
        assertTrue(engine.validateInput("12.", NumberSystem.DECIMAL) is ValidationResult.Invalid)
    }

    @Test
    fun `learn curriculum covers all 10 educational topics with valid practice problems`() {
        assertEquals(10, LearnCurriculum.topics.size)
        for (topic in LearnCurriculum.topics) {
            assertTrue(topic.definition.isNotBlank())
            assertTrue(topic.rules.isNotEmpty())
            assertTrue(topic.examples.isNotEmpty())
            for (practice in topic.practiceProblems) {
                val res = engine.convert(practice.sampleInput, practice.sourceSystem, practice.targetSystem)
                assertTrue("Practice problem failed for ${topic.id}", res.isSuccess)
            }
        }
    }
}
