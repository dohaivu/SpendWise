package com.spendwise.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

internal fun spendingHeatmapBackgroundColor(
    totalBaseAmountCents: Long?,
    currencyFormat: CurrencyDisplayFormat,
    defaultBackground: Color,
    highBackground: Color,
    overlayBackground: Color? = null,
    overlayAlpha: Float = 0f
): Color {
    val baseBackground = if (totalBaseAmountCents != null) {
        val progress = spendingHeatmapProgress(totalBaseAmountCents, currencyFormat)
        lerp(defaultBackground, highBackground, progress)
    } else {
        defaultBackground
    }
    return if (overlayBackground != null && overlayAlpha > 0f) {
        lerp(baseBackground, overlayBackground, overlayAlpha.coerceIn(0f, 1f))
    } else {
        baseBackground
    }
}

private fun spendingHeatmapProgress(
    totalBaseAmountCents: Long,
    currencyFormat: CurrencyDisplayFormat
): Float {
    val wholeAmount = (totalBaseAmountCents / 100).coerceAtLeast(0L)
    val maxAmount = if (currencyFormat.fractionDigits > 0) {
        1_000L
    } else {
        1_000_000L
    }
    return (wholeAmount.toDouble() / maxAmount).coerceIn(0.0, 1.0).toFloat()
}
