package com.spendwise.ui

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
    }

    @Test
    fun editAmountTextRespectsCurrencyFractionDigits() {
        assertEquals("123", centsToAmountText(12_300, "VND"))
        assertEquals("123.45", centsToAmountText(12_345, "USD"))
    }
}
