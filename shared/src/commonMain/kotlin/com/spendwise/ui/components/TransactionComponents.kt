package com.spendwise.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
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

@Composable
internal fun TransactionFiltersPanel(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.transactionFilters.query,
            onValueChange = viewModel::updateTransactionQuery,
            label = { Text("Search note") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.transactionFilters.categoryId == null,
                onClick = { viewModel.updateTransactionCategory(null) },
                label = { Text("All categories") }
            )
            state.snapshot.categories.forEach { category ->
                FilterChip(
                    selected = state.transactionFilters.categoryId == category.id,
                    onClick = { viewModel.updateTransactionCategory(category.id) },
                    label = { Text("${category.icon} ${category.name}") }
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.transactionFilters.currencyCode == null,
                onClick = { viewModel.updateTransactionCurrency(null) },
                label = { Text("All currencies") }
            )
            supportedCurrencies.forEach { currency ->
                FilterChip(
                    selected = state.transactionFilters.currencyCode == currency,
                    onClick = { viewModel.updateTransactionCurrency(currency) },
                    label = { Text(currency) }
                )
            }
            AssistChip(onClick = viewModel::clearTransactionFilters, label = { Text("Reset") })
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
                Text(expense.note.ifBlank { "No note" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(formatMoney(expense.baseAmountCents, currencyCode), fontWeight = FontWeight.SemiBold)
        }
    }
}
