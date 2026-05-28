package com.spendwise.ui

import com.spendwise.ui.components.formatCurrencyAmountInputText
import com.spendwise.ui.components.currencyDisplayFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class AmountInputTest {
    @Test
    fun vndAmountInputDoesNotAllowFraction() {
        assertEquals("123456", sanitizeAmountTextForCurrency("123.456", "VND"))
        assertEquals("1200", sanitizeAmountTextForCurrency("12a.00", "VND"))
    }

    @Test
    fun fractionCurrencyAmountInputAllowsTwoDecimalPlaces() {
        assertEquals("123.45", sanitizeAmountTextForCurrency("123.4567", "USD"))
        assertEquals("12.34", sanitizeAmountTextForCurrency("12..34", "USD"))
        assertEquals("1234.56", sanitizeAmountTextForCurrency("1234,56", "EUR"))
        assertEquals("1234.56", sanitizeAmountTextForCurrency("1.234,56", "EUR"))
    }

    @Test
    fun editAmountTextRespectsCurrencyFractionDigits() {
        assertEquals("123", centsToAmountText(12_300, "VND"))
        assertEquals("123.45", centsToAmountText(12_345, "USD"))
    }

    @Test
    fun amountInputDisplayUsesCurrencySeparators() {
        assertEquals(
            "1,234,567.89",
            formatCurrencyAmountInputText("1234567.89", currencyDisplayFormat("USD")).text
        )
        assertEquals(
            "1.234.567",
            formatCurrencyAmountInputText("1234567", currencyDisplayFormat("VND")).text
        )
        assertEquals(
            "1.234,56",
            formatCurrencyAmountInputText("1234.56", currencyDisplayFormat("EUR")).text
        )
    }

    @Test
    fun amountInputOffsetMappingAccountsForGroupSeparators() {
        val usdAmount = formatCurrencyAmountInputText("1234567.89", currencyDisplayFormat("USD"))
        assertEquals("1,234,567.89", usdAmount.text)
        assertEquals(0, usdAmount.offsetMapping.originalToTransformed(0))
        assertEquals(1, usdAmount.offsetMapping.originalToTransformed(1))
        assertEquals(3, usdAmount.offsetMapping.originalToTransformed(2))
        assertEquals(5, usdAmount.offsetMapping.originalToTransformed(4))
        assertEquals(7, usdAmount.offsetMapping.originalToTransformed(5))
        assertEquals(9, usdAmount.offsetMapping.originalToTransformed(7))
        assertEquals(10, usdAmount.offsetMapping.originalToTransformed(8))

        assertEquals(1, usdAmount.offsetMapping.transformedToOriginal(1))
        assertEquals(1, usdAmount.offsetMapping.transformedToOriginal(2))
        assertEquals(4, usdAmount.offsetMapping.transformedToOriginal(5))
        assertEquals(4, usdAmount.offsetMapping.transformedToOriginal(6))
        assertEquals(7, usdAmount.offsetMapping.transformedToOriginal(9))
        assertEquals(8, usdAmount.offsetMapping.transformedToOriginal(10))

        val vndAmount = formatCurrencyAmountInputText("1234", currencyDisplayFormat("VND"))
        assertEquals("1.234", vndAmount.text)
        assertEquals(1, vndAmount.offsetMapping.originalToTransformed(1))
        assertEquals(3, vndAmount.offsetMapping.originalToTransformed(2))
        assertEquals(1, vndAmount.offsetMapping.transformedToOriginal(2))

        val eurAmount = formatCurrencyAmountInputText("1234.56", currencyDisplayFormat("EUR"))
        assertEquals("1.234,56", eurAmount.text)
        assertEquals(5, eurAmount.offsetMapping.originalToTransformed(4))
        assertEquals(6, eurAmount.offsetMapping.originalToTransformed(5))
        assertEquals(4, eurAmount.offsetMapping.transformedToOriginal(5))
        assertEquals(5, eurAmount.offsetMapping.transformedToOriginal(6))
    }
}
