package com.spendwise.domain.usecase

import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.AddExpenseInput
import com.spendwise.domain.Expense
import com.spendwise.domain.ReportCalculator
import com.spendwise.domain.TransactionFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AddExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(input: AddExpenseInput): Long {
        require(input.id == null) { "AddExpenseUseCase requires a new expense input." }
        return repository.saveExpense(input)
    }
}

class UpdateExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(input: AddExpenseInput): Long {
        require(input.id != null) { "UpdateExpenseUseCase requires an existing expense id." }
        return repository.saveExpense(input)
    }
}

class DeleteExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteExpense(id)
    }
}

class RenameTagUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(oldTag: String, newTag: String) {
        repository.renameTag(oldTag, newTag)
    }
}

class DeleteTagUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(tag: String) {
        repository.deleteTag(tag)
    }
}

class GetExpensesUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<Expense>> =
        repository.observeSnapshot().map { it.expenses }
}

class GetTransactionsByDateUseCase {
    operator fun invoke(
        expenses: List<Expense>,
        date: LocalDate,
        timeZone: TimeZone,
        filters: TransactionFilters = TransactionFilters()
    ): List<Expense> {
        return expenses
            .filter { expense ->
                Instant.fromEpochMilliseconds(expense.spentAtMillis).toLocalDateTime(timeZone).date == date
            }
            .filterByTransactionFilters(filters)
    }
}

class GetTransactionsByFiltersUseCase {
    operator fun invoke(
        expenses: List<Expense>,
        filters: TransactionFilters
    ): List<Expense> = expenses.filterByTransactionFilters(filters)
}

fun List<Expense>.filterByTransactionFilters(filters: TransactionFilters): List<Expense> {
    return with(ReportCalculator) { filterByTags(filters.selectedTags) }
        .filter { filters.categoryId == null || it.categoryId == filters.categoryId }
        .filter { filters.query.isBlank() || it.note.contains(filters.query, ignoreCase = true) }
}
