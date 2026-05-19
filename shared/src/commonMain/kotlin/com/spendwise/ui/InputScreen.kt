package com.spendwise.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun InputScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                if (state.draft.editingExpenseId == null) "Input" else "Edit expense",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.draft.amountText,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                CurrencyMenu(
                    selected = state.draft.currencyCode,
                    onSelected = viewModel::updateCurrency,
                    modifier = Modifier.width(126.dp)
                )
            }
        }
        if (state.draft.currencyCode != state.baseCurrencyCode) {
            item {
                OutlinedTextField(
                    value = state.draft.exchangeRateText,
                    onValueChange = viewModel::updateExchangeRate,
                    label = { Text("Rate to ${state.baseCurrencyCode}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        item {
            Text("Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::selectTodayForDraft, label = { Text("Today") })
                AssistChip(onClick = viewModel::selectYesterdayForDraft, label = { Text("Yesterday") })
                AssistChip(onClick = {}, label = { Text(formatDate(state.draft.spentAtMillis)) })
            }
        }
        item {
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.snapshot.categories.filterNot { it.archived }.forEach { category ->
                    FilterChip(
                        selected = state.draft.categoryId == category.id,
                        onClick = { viewModel.updateCategory(category.id) },
                        label = { Text("${category.icon} ${category.name}") }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.draft.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Note with #tags") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            if (state.tagSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.tagSuggestions.forEach { tag ->
                        AssistChip(onClick = { viewModel.selectTagSuggestion(tag) }, label = { Text("#$tag") })
                    }
                }
            }
        }
        item {
            Button(onClick = viewModel::saveExpense, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.draft.editingExpenseId == null) "Save expense" else "Update expense")
            }
            if (state.draft.editingExpenseId != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = viewModel::cancelExpenseEdit, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    OutlinedButton(onClick = viewModel::deleteEditingExpense, modifier = Modifier.weight(1f)) {
                        Text("Delete")
                    }
                }
            }
        }
        item {
            RecentTransactions(
                expenses = state.snapshot.expenses,
                categories = state.snapshot.categories,
                currencyCode = state.baseCurrencyCode,
                onExpenseClick = viewModel::editExpense
            )
        }
    }
}

