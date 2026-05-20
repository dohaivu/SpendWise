package com.spendwise.data

import kotlin.test.Test
import kotlin.test.assertEquals

class FrankfurterExchangeRateClientTest {
    @Test
    fun derivesCrossRateFromDefaultEurQuotes() {
        val response = """
            [
              {"date":"2026-05-19","base":"EUR","quote":"USD","rate":1.25},
              {"date":"2026-05-19","base":"EUR","quote":"VND","rate":31250.0}
            ]
        """.trimIndent()

        val quote = parseFrankfurterRate(
            response = response,
            fromCurrencyCode = "USD",
            toCurrencyCode = "VND"
        )

        assertEquals(25_000.0, quote?.rate)
        assertEquals("USD", quote?.fromCurrencyCode)
        assertEquals("VND", quote?.toCurrencyCode)
    }

    @Test
    fun treatsEurAsImplicitDefaultBaseRate() {
        val response = """
            [
              {"date":"2026-05-19","base":"EUR","quote":"USD","rate":1.25}
            ]
        """.trimIndent()

        val quote = parseFrankfurterRate(
            response = response,
            fromCurrencyCode = "USD",
            toCurrencyCode = "EUR"
        )

        assertEquals(0.8, quote?.rate)
    }
}
