package com.transport.tickets.presentation.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Отображает 8 сырых цифр как дд.мм.гггг.
 * Состояние TextField хранит только цифры (e.g. "01012000").
 */
object DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 2 || i == 4) append('.')
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val extra = (if (offset >= 2) 1 else 0) + (if (offset >= 4) 1 else 0)
                return (offset + extra).coerceAtMost(formatted.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                val dots = (if (offset > 2) 1 else 0) + (if (offset > 5) 1 else 0)
                return (offset - dots).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/**
 * Отображает 10 сырых цифр как +7 (XXX) XXX-XX-XX.
 * Состояние TextField хранит только цифры без кода страны (e.g. "9123456789").
 */
object PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text
        val formatted = buildString {
            append("+7")
            if (d.isNotEmpty()) {
                append(" (")
                append(d.take(3))
                if (d.length >= 3) {
                    append(") ")
                    append(d.substring(3, minOf(6, d.length)))
                    if (d.length > 6) {
                        append("-")
                        append(d.substring(6, minOf(8, d.length)))
                        if (d.length > 8) {
                            append("-")
                            append(d.substring(8))
                        }
                    }
                }
            }
        }
        val mapping = object : OffsetMapping {
            // Prefix "+7 (" = 4 chars; after digit 3: ") " = +2; after digit 6: "-" = +1; after digit 8: "-" = +1
            override fun originalToTransformed(offset: Int): Int {
                val base = if (d.isEmpty()) 2 else 4
                val extra = (if (offset >= 3) 2 else 0) +
                    (if (offset >= 6) 1 else 0) +
                    (if (offset >= 8) 1 else 0)
                return (offset + base + extra - (if (d.isEmpty()) 0 else 0))
                    .coerceAtMost(formatted.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (d.isEmpty()) return 0
                // Prefix is 4 chars: offsets 0-3 map to raw 0
                if (offset <= 4) return 0
                val adj = offset - 4
                val raw = when {
                    adj <= 3 -> adj              // digits 1-3
                    adj <= 5 -> 3               // ") " separator
                    adj <= 8 -> adj - 2         // digits 4-6 (subtract ") ")
                    adj == 9 -> 6               // "-" separator
                    adj <= 11 -> adj - 3        // digits 7-8
                    adj == 12 -> 8              // second "-" separator
                    else -> adj - 4             // digits 9-10
                }
                return raw.coerceIn(0, d.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/** Извлекает 10 сырых цифр из телефонной строки любого формата. */
fun extractPhoneDigits(input: String): String =
    input.filter(Char::isDigit)
        .let { if (it.startsWith("7") || it.startsWith("8")) it.drop(1) else it }
        .take(10)

/** Извлекает 8 сырых цифр из строки даты любого формата. */
fun extractDateDigits(input: String): String =
    input.filter(Char::isDigit).take(8)
