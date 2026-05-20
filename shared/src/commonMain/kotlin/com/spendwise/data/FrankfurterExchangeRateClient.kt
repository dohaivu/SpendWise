package com.spendwise.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

data class ExchangeRateQuote(
    val fromCurrencyCode: String,
    val toCurrencyCode: String,
    val effectiveDateEpochDay: Int,
    val rate: Double
)

class FrankfurterExchangeRateClient(
    private val httpClient: HttpClient
) {
    suspend fun getLatestRate(fromCurrencyCode: String, toCurrencyCode: String): ExchangeRateQuote? {
        if (fromCurrencyCode == toCurrencyCode) {
            return ExchangeRateQuote(
                fromCurrencyCode = fromCurrencyCode,
                toCurrencyCode = toCurrencyCode,
                effectiveDateEpochDay = todayEpochDay(),
                rate = 1.0
            )
        }
        val response = httpClient.get(
            "https://api.frankfurter.dev/v2/rates?quotes=$fromCurrencyCode,$toCurrencyCode"
        ).bodyAsText()
        return parseFrankfurterRate(
            response = response,
            fromCurrencyCode = fromCurrencyCode,
            toCurrencyCode = toCurrencyCode
        )
    }
}

internal fun parseFrankfurterRate(
    response: String,
    fromCurrencyCode: String,
    toCurrencyCode: String
): ExchangeRateQuote? {
    val rows = Json.parseToJsonElement(response) as? JsonArray ?: return null
    val ratesByQuote = mutableMapOf(FRANKFURTER_DEFAULT_BASE to 1.0)
    var effectiveDateEpochDay: Int? = null

    rows.forEach { element ->
        val item = element.jsonObject
        val quote = item["quote"]?.jsonPrimitive?.content ?: return@forEach
        val rate = item["rate"]?.jsonPrimitive?.double ?: return@forEach
        val date = item["date"]?.jsonPrimitive?.content
        ratesByQuote[quote] = rate
        if (effectiveDateEpochDay == null && date != null) {
            effectiveDateEpochDay = LocalDate.parse(date).toEpochDays().toInt()
        }
    }

    val fromRate = ratesByQuote[fromCurrencyCode] ?: return null
    val toRate = ratesByQuote[toCurrencyCode] ?: return null
    return ExchangeRateQuote(
        fromCurrencyCode = fromCurrencyCode,
        toCurrencyCode = toCurrencyCode,
        effectiveDateEpochDay = effectiveDateEpochDay ?: todayEpochDay(),
        rate = toRate / fromRate
    )
}

private const val FRANKFURTER_DEFAULT_BASE = "EUR"

private fun todayEpochDay(): Int =
    Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toEpochDays().toInt()
