package com.spendwise.domain.usecase

import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong

class ConvertToBaseCurrencyUseCase {
    operator fun invoke(amountCents: Long, sourceCurrencyCode: String, baseCurrencyCode: String, exchangeRate: Double): Long {
        return if (sourceCurrencyCode == baseCurrencyCode) amountCents else (amountCents * exchangeRate).roundToLong()
    }
}

class GetCurrencySettingsUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<UserSettings> =
        repository.observeSnapshot().map { it.settings }
}

class UpdateBaseCurrencyUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(baseCurrencyCode: String) {
        val current = repository.observeSnapshot().map { it.settings }.first()
        repository.saveSettings(current.copy(baseCurrencyCode = baseCurrencyCode))
    }
}

class GetExchangeRateUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(fromCurrencyCode: String, toCurrencyCode: String): Double? =
        repository.getLatestExchangeRate(fromCurrencyCode, toCurrencyCode)
}

