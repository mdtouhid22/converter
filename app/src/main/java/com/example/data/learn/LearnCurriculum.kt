package com.example.data.learn

import com.example.domain.model.NumberSystem

data class LearnExample(
    val title: String,
    val steps: List<String>,
    val answer: String
)

data class PracticeProblem(
    val question: String,
    val sampleInput: String,
    val sourceSystem: NumberSystem,
    val targetSystem: NumberSystem,
    val hint: String,
    val answer: String
)

data class LearnTopic(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val definition: String,
    val rules: List<String>,
    val examples: List<LearnExample>,
    val practiceProblems: List<PracticeProblem>
)

object LearnCurriculum {
    val topics: List<LearnTopic> = listOf(
        LearnTopic(
            id = "what_is_number_system",
            title = "What is a Number System?",
            subtitle = "Understand positional notation, symbols, and place value.",
            badge = "FOUNDATION",
            definition = "A number system is a mathematical way of representing numeric values using a consistent set of digits or symbols. In positional number systems, the value of each digit depends on both the digit itself and its position relative to the radix (decimal) point.",
            rules = listOf(
                "Every positional number system has a fixed Base (or Radix), written as a subscript like (101)₂.",
                "If a system has base B, it uses exactly B unique symbols from 0 to (B − 1).",
                "Positions to the left of the point carry non-negative powers: B⁰, B¹, B², B³...",
                "Positions to the right of the point carry negative powers: B⁻¹, B⁻², B⁻³..."
            ),
            examples = listOf(
                LearnExample(
                    title = "Place value in Decimal: (345.2)₁₀",
                    steps = listOf(
                        "3 is in the 10² (hundreds) place → 3 × 100 = 300",
                        "4 is in the 10¹ (tens) place → 4 × 10 = 40",
                        "5 is in the 10⁰ (ones) place → 5 × 1 = 5",
                        "2 is in the 10⁻¹ (tenths) place → 2 × 0.1 = 0.2"
                    ),
                    answer = "300 + 40 + 5 + 0.2 = 345.2"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (101.1)₂ to Decimal using place values.",
                    sampleInput = "101.1",
                    sourceSystem = NumberSystem.BINARY,
                    targetSystem = NumberSystem.DECIMAL,
                    hint = "Multiply each bit by 2², 2¹, 2⁰, and 2⁻¹.",
                    answer = "(5.5)₁₀"
                )
            )
        ),
        LearnTopic(
            id = "binary_system",
            title = "Binary Number System",
            subtitle = "Base 2 — the fundamental language of digital computers.",
            badge = "BASE 2",
            definition = "The Binary number system uses only two digits: 0 and 1. Each binary digit is called a 'bit'. Modern computers and digital circuits use binary because electronic switches naturally represent two states: OFF (0) and ON (1).",
            rules = listOf(
                "Allowed digits: only 0 and 1 (entering 2–9 or letters is invalid).",
                "Radix (Base) is 2; place values double from right to left (1, 2, 4, 8, 16, 32...).",
                "Fractional places halve from left to right (0.5, 0.25, 0.125, 0.0625...).",
                "4 bits form a Nibble; 8 bits form a Byte."
            ),
            examples = listOf(
                LearnExample(
                    title = "Convert (1100.11)₂ to Decimal",
                    steps = listOf(
                        "(1100.11)₂ = (1×2³) + (1×2²) + (0×2¹) + (0×2⁰) + (1×2⁻¹) + (1×2⁻²)",
                        "= 8 + 4 + 0 + 0 + 0.5 + 0.25"
                    ),
                    answer = "(12.75)₁₀"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Find the Decimal value of (10110)₂.",
                    sampleInput = "10110",
                    sourceSystem = NumberSystem.BINARY,
                    targetSystem = NumberSystem.DECIMAL,
                    hint = "Add weights for positions with 1: 16 + 4 + 2.",
                    answer = "(22)₁₀"
                )
            )
        ),
        LearnTopic(
            id = "decimal_system",
            title = "Decimal Number System",
            subtitle = "Base 10 — everyday human arithmetic with digits 0–9.",
            badge = "BASE 10",
            definition = "The Decimal system is a base-10 positional number system using ten digits: 0, 1, 2, 3, 4, 5, 6, 7, 8, and 9. Each shift to the left multiplies a digit's value by 10, while shifting right divides by 10.",
            rules = listOf(
                "Allowed digits: 0 through 9.",
                "Base is 10; subscript is written as (N)₁₀.",
                "To convert Decimal to another base B, repeatedly divide the integer part by B and multiply the fractional part by B.",
                "Note: Some terminating decimal fractions (like 0.1₁₀) become repeating fractions in Binary (0.000110011...₂)."
            ),
            examples = listOf(
                LearnExample(
                    title = "Convert (25)₁₀ to Binary",
                    steps = listOf(
                        "25 ÷ 2 = 12 remainder 1",
                        "12 ÷ 2 = 6  remainder 0",
                        "6 ÷ 2  = 3  remainder 0",
                        "3 ÷ 2  = 1  remainder 1",
                        "1 ÷ 2  = 0  remainder 1 (read bottom-to-top)"
                    ),
                    answer = "(11001)₂"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (45.25)₁₀ to Binary.",
                    sampleInput = "45.25",
                    sourceSystem = NumberSystem.DECIMAL,
                    targetSystem = NumberSystem.BINARY,
                    hint = "Divide 45 by 2 repeatedly; multiply 0.25 by 2.",
                    answer = "(101101.01)₂"
                )
            )
        ),
        LearnTopic(
            id = "octal_system",
            title = "Octal Number System",
            subtitle = "Base 8 — compact 3-bit shorthand using digits 0–7.",
            badge = "BASE 8",
            definition = "The Octal number system has a base of 8 and uses eight digits: 0, 1, 2, 3, 4, 5, 6, and 7. Because 8 = 2³, every single octal digit represents exactly three binary bits.",
            rules = listOf(
                "Allowed digits: 0 through 7 only (8 and 9 do not exist in Octal!).",
                "Base is 8; place values are powers of 8 (1, 8, 64, 512...).",
                "Each Octal digit maps directly to 3 binary bits (0 → 000 up to 7 → 111).",
                "Commonly used in Unix/Linux file permissions (e.g., chmod 755)."
            ),
            examples = listOf(
                LearnExample(
                    title = "Convert (75.4)₈ to Decimal",
                    steps = listOf(
                        "(75.4)₈ = (7 × 8¹) + (5 × 8⁰) + (4 × 8⁻¹)",
                        "= 56 + 5 + 0.5"
                    ),
                    answer = "(61.5)₁₀"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (37.2)₈ to Binary.",
                    sampleInput = "37.2",
                    sourceSystem = NumberSystem.OCTAL,
                    targetSystem = NumberSystem.BINARY,
                    hint = "Expand 3 → 011, 7 → 111, and .2 → .010.",
                    answer = "(11111.01)₂"
                )
            )
        ),
        LearnTopic(
            id = "hex_system",
            title = "Hexadecimal Number System",
            subtitle = "Base 16 — compact 4-bit notation with 0–9 and A–F.",
            badge = "BASE 16",
            definition = "The Hexadecimal system has a base of 16 and uses sixteen symbols: digits 0–9 for values zero to nine, and letters A–F (or a–f) for values ten to fifteen. Because 16 = 2⁴, one hex digit equals four binary bits (one nibble).",
            rules = listOf(
                "Allowed symbols: 0–9 and A=10, B=11, C=12, D=13, E=14, F=15.",
                "Case-insensitive input: both 'a–f' and 'A–F' represent 10–15.",
                "Each Hex digit expands directly into 4 binary bits (0 → 0000 up to F → 1111).",
                "Widely used for memory addresses, MAC addresses, and RGB color codes (like #008F83)."
            ),
            examples = listOf(
                LearnExample(
                    title = "Convert (2F.8)₁₆ to Decimal",
                    steps = listOf(
                        "F = 15",
                        "(2F.8)₁₆ = (2 × 16¹) + (15 × 16⁰) + (8 × 16⁻¹)",
                        "= 32 + 15 + 0.5"
                    ),
                    answer = "(47.5)₁₀"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (A9.C)₁₆ to Binary.",
                    sampleInput = "A9.C",
                    sourceSystem = NumberSystem.HEXADECIMAL,
                    targetSystem = NumberSystem.BINARY,
                    hint = "A = 1010, 9 = 1001, C = 1100.",
                    answer = "(10101001.11)₂"
                )
            )
        ),
        LearnTopic(
            id = "base_and_radix",
            title = "Base and Radix",
            subtitle = "How radix governs digit range, place weights, and subscripts.",
            badge = "CONCEPT",
            definition = "The terms 'Base' and 'Radix' mean the same thing: the total count of unique digits (including zero) available in a positional number system. For example, Binary has radix 2, Octal has radix 8, Decimal has radix 10, and Hexadecimal has radix 16.",
            rules = listOf(
                "For any radix r, the largest valid single digit is (r − 1).",
                "Always write the radix as a subscript when mixing systems, e.g., (10)₂ = (2)₁₀.",
                "Moving a digit 1 step left multiplies its contribution by r; moving 1 step right divides by r.",
                "General value formula: Σ dᵢ × rⁱ where i ≥ 0 for integer digits and i < 0 for fractional digits."
            ),
            examples = listOf(
                LearnExample(
                    title = "Same digits '100' in four different bases",
                    steps = listOf(
                        "(100)₂  = 1 × 2²  = 4 in Decimal",
                        "(100)₈  = 1 × 8²  = 64 in Decimal",
                        "(100)₁₀ = 1 × 10² = 100 in Decimal",
                        "(100)₁₆ = 1 × 16² = 256 in Decimal"
                    ),
                    answer = "Same notation, completely different values!"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "What is (77)₈ in Hexadecimal?",
                    sampleInput = "77",
                    sourceSystem = NumberSystem.OCTAL,
                    targetSystem = NumberSystem.HEXADECIMAL,
                    hint = "Convert 77₈ → 111 111₂ → regroup in 4s: 0011 1111₂.",
                    answer = "(3F)₁₆"
                )
            )
        ),
        LearnTopic(
            id = "conversion_rules",
            title = "Conversion Rules Overview",
            subtitle = "Pick the fastest, cleanest method for any of the 12 base pairs.",
            badge = "STRATEGY",
            definition = "While every conversion can pass through Decimal as a universal bridge, power-of-2 bases (Binary, Octal, Hexadecimal) allow instant grouping shortcuts without division or multiplication.",
            rules = listOf(
                "Any Base → Decimal: Multiply each digit by Base^position and sum.",
                "Decimal → Any Base: Divide integer repeatedly by Target Base; multiply fraction repeatedly by Target Base.",
                "Binary ↔ Octal: Direct 3-bit grouping / expansion (2³ = 8).",
                "Binary ↔ Hexadecimal: Direct 4-bit grouping / expansion (2⁴ = 16).",
                "Octal ↔ Hexadecimal: Bridge through Binary (3-bit ↔ 4-bit) — no Decimal arithmetic needed!"
            ),
            examples = listOf(
                LearnExample(
                    title = "Bridge Octal (35)₈ to Hexadecimal",
                    steps = listOf(
                        "Step 1 (Octal → Binary): 3 → 011, 5 → 101  ⇒  (011101)₂ = (11101)₂",
                        "Step 2 (Regroup into 4s): 0001 1101",
                        "Step 3 (Binary → Hex): 0001 → 1, 1101 → D"
                    ),
                    answer = "(1D)₁₆"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (CAFE)₁₆ to Octal using Binary as a bridge.",
                    sampleInput = "CAFE",
                    sourceSystem = NumberSystem.HEXADECIMAL,
                    targetSystem = NumberSystem.OCTAL,
                    hint = "Expand each hex digit to 4 bits, then regroup into 3-bit blocks.",
                    answer = "(145376)₈"
                )
            )
        ),
        LearnTopic(
            id = "binary_to_octal",
            title = "Binary to Octal Rules",
            subtitle = "Master the 3-bit grouping rule for integers and fractions.",
            badge = "3-BIT RULE",
            definition = "Because 8 = 2³, each block of 3 binary bits maps uniquely to one Octal digit (0–7). Grouping starts outward from the binary point.",
            rules = listOf(
                "Integer part: Group bits in 3s from RIGHT to LEFT. Pad missing left bits with 0.",
                "Fractional part: Group bits in 3s from LEFT to RIGHT. Pad missing right bits with 0.",
                "Use weights 4-2-1 inside each 3-bit block (e.g., 101 = 4 + 0 + 1 = 5).",
                "Never pad the fractional part on the left or the integer part on the right!"
            ),
            examples = listOf(
                LearnExample(
                    title = "Convert (1100.11)₂ to Octal",
                    steps = listOf(
                        "Integer 1100 → pad left → 001 100",
                        "Fraction .11 → pad right → .110",
                        "001 = 1, 100 = 4, .110 = .6"
                    ),
                    answer = "(14.6)₈"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (10111.01)₂ to Octal.",
                    sampleInput = "10111.01",
                    sourceSystem = NumberSystem.BINARY,
                    targetSystem = NumberSystem.OCTAL,
                    hint = "010 111 . 010 → 2, 7, and .2.",
                    answer = "(27.2)₈"
                )
            )
        ),
        LearnTopic(
            id = "binary_to_hex",
            title = "Binary to Hexadecimal Rules",
            subtitle = "Master the 4-bit nibble grouping rule with 8-4-2-1 weights.",
            badge = "4-BIT RULE",
            definition = "Because 16 = 2⁴, every 4-bit block (nibble) represents one Hexadecimal symbol (0–9, A–F). Just like Octal grouping, grouping always radiates outward from the binary point.",
            rules = listOf(
                "Integer part: Group in 4s from RIGHT to LEFT; pad left with 0s if needed.",
                "Fractional part: Group in 4s from LEFT to RIGHT; pad right with 0s if needed.",
                "Use weights 8-4-2-1 inside each 4-bit group.",
                "Convert values 10–15 to letters: 10=A, 11=B, 12=C, 13=D, 14=E, 15=F."
            ),
            examples = listOf(
                LearnExample(
                    title = "Convert (11011110.1)₂ to Hexadecimal",
                    steps = listOf(
                        "Integer 11011110 → 1101 1110",
                        "Fraction .1 → pad right → .1000",
                        "1101 = 13 (D), 1110 = 14 (E), 1000 = 8"
                    ),
                    answer = "(DE.8)₁₆"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (10101111.11)₂ to Hexadecimal.",
                    sampleInput = "10101111.11",
                    sourceSystem = NumberSystem.BINARY,
                    targetSystem = NumberSystem.HEXADECIMAL,
                    hint = "1010=A, 1111=F, .1100=.C.",
                    answer = "(AF.C)₁₆"
                )
            )
        ),
        LearnTopic(
            id = "decimal_fraction_conversion",
            title = "Decimal Fraction Conversion",
            subtitle = "Repeated multiplication method & handling non-terminating digits.",
            badge = "FRACTIONS",
            definition = "To convert a Decimal fraction (the digits after the decimal point) into Binary, Octal, or Hexadecimal, repeatedly multiply only the fractional part by the target base and extract the integer part at each step.",
            rules = listOf(
                "Multiply the fractional part by target base B (2, 8, or 16).",
                "Record the integer part (before the dot) as the next output digit.",
                "Keep only the remaining fractional part for the next multiplication step.",
                "Read collected digits from TOP to BOTTOM (opposite of integer division!).",
                "Stop when the fractional remainder hits 0, or stop at your chosen precision limit if it repeats forever."
            ),
            examples = listOf(
                LearnExample(
                    title = "Convert (0.625)₁₀ to Binary",
                    steps = listOf(
                        "0.625 × 2 = 1.25  → integer digit = 1, keep 0.25",
                        "0.25  × 2 = 0.50  → integer digit = 0, keep 0.50",
                        "0.50  × 2 = 1.00  → integer digit = 1, remainder = 0 (stop!)"
                    ),
                    answer = "(0.101)₂"
                )
            ),
            practiceProblems = listOf(
                PracticeProblem(
                    question = "Convert (0.375)₁₀ to Hexadecimal.",
                    sampleInput = "0.375",
                    sourceSystem = NumberSystem.DECIMAL,
                    targetSystem = NumberSystem.HEXADECIMAL,
                    hint = "Multiply 0.375 × 16 = 6.0.",
                    answer = "(0.6)₁₆"
                )
            )
        )
    )

    fun findTopic(id: String): LearnTopic? = topics.firstOrNull { it.id == id }
}
