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
        assertEquals("123.456.789₫", formatMoney(12_345_678_900, "VND"))
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
        assertEquals("123.456.789", formatMoneyValue(12_345_678_900, "VND"))
        assertEquals("-123.456.789,00", formatMoneyValue(-12_345_678_900, "EUR"))
    }

    @Test
    fun currencyFilterSliderConfigAdaptsToCurrencies() {
        val vndConfig = currencyFilterSliderConfig(currencyDisplayFormat("VND"))
        assertEquals(10_000_000_00L, vndConfig.maxCents)
        assertEquals(100_000_00L, vndConfig.stepCents)

        val usdConfig = currencyFilterSliderConfig(currencyDisplayFormat("USD"))
        assertEquals(100_000L, usdConfig.maxCents)
        assertEquals(1_000L, usdConfig.stepCents)

        val jpyConfig = currencyFilterSliderConfig(currencyDisplayFormat("JPY"))
        assertEquals(100_000_00L, jpyConfig.maxCents)
        assertEquals(1_000_00L, jpyConfig.stepCents)
    }
}
