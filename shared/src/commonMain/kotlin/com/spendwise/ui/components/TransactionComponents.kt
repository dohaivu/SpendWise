package com.spendwise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.usecase.filterByTransactionFilters
import com.spendwise.ui.calendar.CalendarViewModel
import com.spendwise.ui.CalendarUiState

@Composable
internal fun TransactionFiltersPanel(
    state: CalendarUiState,
    calendarViewModel: CalendarViewModel,
    singleLineCategories: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.tagUsage.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.tagUsage.forEach { usage ->
                    FilterChip(
                        selected = usage.name in state.selectedTags,
                        onClick = { calendarViewModel.toggleTagFilter(usage.name) },
                        label = { Text("#${usage.name}") }
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.transactionFilters.query,
            onValueChange = calendarViewModel::updateTransactionQuery,
            label = { Text("Search note") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (singleLineCategories) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = state.transactionFilters.categoryId == null,
                    onClick = { calendarViewModel.updateTransactionCategory(null) },
                    label = { Text("All categories") }
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.transactionFilters.categoryId == category.id,
                        onClick = { calendarViewModel.updateTransactionCategory(category.id) },
                        label = { Text("${category.icon} ${category.name}") }
                    )
                }
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = state.transactionFilters.categoryId == null,
                    onClick = { calendarViewModel.updateTransactionCategory(null) },
                    label = { Text("All categories") }
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.transactionFilters.categoryId == category.id,
                        onClick = { calendarViewModel.updateTransactionCategory(category.id) },
                        label = { Text("${category.icon} ${category.name}") }
                    )
                }
            }
        }
    }
}

internal fun List<Expense>.applyTransactionFilters(
    filters: TransactionFilters,
    selectedTags: Set<String>
): List<Expense> {
    return filterByTransactionFilters(filters, selectedTags)
}

@Composable
internal fun RecentTransactions(
    expenses: List<Expense>,
    categories: List<Category>,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit
) {
    Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        expenses.take(5).forEach { expense ->
            TransactionRow(expense, categories, currencyCode, onExpenseClick)
        }
    }
}

@Composable
internal fun TransactionRow(
    expense: Expense,
    categories: List<Category>,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit
) {
    val category = categories.firstOrNull { it.id == expense.categoryId }
    Card(onClick = { onExpenseClick(expense) }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(category?.icon ?: "•", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: "Category", fontWeight = FontWeight.SemiBold)
                if (expense.note.isNotBlank()) {
                    Text(expense.note, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (expense.tags.isNotEmpty()) {
                    Text(expense.tags.joinToString(" ") { "#$it" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (expense.originalCurrencyCode != currencyCode) {
                    Text(
                        "Original ${formatMoney(expense.originalAmountCents, expense.originalCurrencyCode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            MoneyText(
                amountCents = expense.baseAmountCents,
                currencyCode = currencyCode,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
