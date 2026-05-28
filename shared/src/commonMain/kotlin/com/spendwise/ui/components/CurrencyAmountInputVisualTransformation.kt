package com.spendwise.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal class CurrencyAmountInputVisualTransformation(
    private val format: CurrencyDisplayFormat
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatCurrencyAmountInputText(text.text, format)
        return TransformedText(
            text = AnnotatedString(formatted.text),
            offsetMapping = formatted.offsetMapping
        )
    }
}

internal fun formatCurrencyAmountInputText(
    amountText: String,
    format: CurrencyDisplayFormat
): FormattedCurrencyAmountInput {
    if (amountText.isEmpty()) {
        return FormattedCurrencyAmountInput("", OffsetMapping.Identity)
    }

    val decimalIndex = amountText.indexOf('.').takeIf { it >= 0 }
    val wholeEnd = decimalIndex ?: amountText.length
    val tokens = buildList {
        val wholeLength = wholeEnd
        for (index in 0 until wholeEnd) {
            val remainingDigits = wholeLength - index
            if (index > 0 && remainingDigits % 3 == 0) {
                add(AmountInputToken(format.groupSeparator, anchorIndex = index))
            }
            add(AmountInputToken(amountText[index], sourceIndex = index))
        }
        if (decimalIndex != null) {
            add(AmountInputToken(format.decimalSeparator, sourceIndex = decimalIndex))
            for (index in decimalIndex + 1 until amountText.length) {
                add(AmountInputToken(amountText[index], sourceIndex = index))
            }
        }
    }

    val transformedText = tokens.joinToString(separator = "") { it.char.toString() }
    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val coerced = offset.coerceIn(0, amountText.length)
            return tokens.count { token -> token.effectiveIndex < coerced }
        }

        override fun transformedToOriginal(offset: Int): Int {
            val coerced = offset.coerceIn(0, transformedText.length)
            return tokens.take(coerced).count { token -> token.sourceIndex != null }
        }
    }
    return FormattedCurrencyAmountInput(transformedText, offsetMapping)
}

internal data class FormattedCurrencyAmountInput(
    val text: String,
    val offsetMapping: OffsetMapping
)

private data class AmountInputToken(
    val char: Char,
    val sourceIndex: Int? = null,
    val anchorIndex: Int = sourceIndex ?: 0
) {
    val effectiveIndex: Int = sourceIndex ?: anchorIndex
}
