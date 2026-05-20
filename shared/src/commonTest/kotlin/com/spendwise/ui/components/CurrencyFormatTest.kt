package com.spendwise.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {
    @Test
    fun formatsUsdWithSymbolGroupingAndFraction() {
        assertEquals("\$123,456,789.00", formatMoney(12_345_678_900, "USD"))
    }

    @Test
    fun formatsVndWithSuffixSymbolAndNoFraction() {
        assertEquals("123,456,789₫", formatMoney(12_345_678_900, "VND"))
    }

    @Test
    fun formatsEuroWithEuropeanSeparatorsAndSuffixSymbol() {
        assertEquals("123.456.789,00 €", formatMoney(12_345_678_900, "EUR"))
    }

    @Test
    fun keepsSignOutsideSymbol() {
        assertEquals("-\$12.34", formatMoney(-1_234, "USD"))
        assertEquals("-12₫", formatMoney(-1_234, "VND"))
    }

    @Test
    fun formatsCurrencyValueWithoutSymbol() {
        assertEquals("123,456,789.00", formatMoneyValue(12_345_678_900, "USD"))
        assertEquals("123,456,789", formatMoneyValue(12_345_678_900, "VND"))
        assertEquals("-123.456.789,00", formatMoneyValue(-12_345_678_900, "EUR"))
    }
}
