package com.spendwise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.ui.AppLanguage
import com.spendwise.ui.SpendWiseUiState
import com.spendwise.ui.SpendWiseViewModel
import com.spendwise.ui.TagUsageSort
import com.spendwise.ui.formatMoney
import com.spendwise.ui.supportedCurrencies

private val categoryColors = listOf(
    0xFFE76F51,
    0xFF2A9D8F,
    0xFF457B9D,
    0xFFE9C46A,
    0xFF6D597A,
    0xFF43AA8B
)

@Composable
internal fun OthersScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Others", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item { CategoryManagement(state, viewModel) }
        item { CurrencySettings(state, viewModel) }
        item { LanguageSettings(state, viewModel) }
        item {
            Text("Tag usage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TagUsageSort.entries.forEach { sort ->
                    FilterChip(
                        selected = state.tagUsageSort == sort,
                        onClick = { viewModel.setTagUsageSort(sort) },
                        label = { Text(sort.label()) }
                    )
                }
            }
        }
        items(viewModel.getSortedTagUsage()) { usage ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${usage.name}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${usage.expenseCount} uses")
                        Spacer(Modifier.width(14.dp))
                        Text(formatMoney(usage.totalBaseAmountCents, state.baseCurrencyCode))
                    }
                    Text(
                        "This month ${formatMoney(usage.currentMonthAmountCents, state.baseCurrencyCode)} • Previous ${formatMoney(usage.previousMonthAmountCents, state.baseCurrencyCode)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        item {
            Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${state.snapshot.expenses.size} expenses • ${state.snapshot.categories.size} categories • ${state.snapshot.tagUsage.size} tags",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryManagement(state: SpendWiseUiState, viewModel: SpendWiseViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.categoryDraft.icon,
                onValueChange = viewModel::updateCategoryIcon,
                label = { Text("Icon") },
                singleLine = true,
                modifier = Modifier.width(88.dp)
            )
            OutlinedTextField(
                value = state.categoryDraft.name,
                onValueChange = viewModel::updateCategoryName,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categoryColors.forEach { color ->
                FilterChip(
                    selected = state.categoryDraft.color == color,
                    onClick = { viewModel.updateCategoryColor(color) },
                    label = { Text(colorName(color)) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = viewModel::saveCategory, modifier = Modifier.weight(1f)) {
                Text(if (state.categoryDraft.editingCategoryId == null) "Add category" else "Update category")
            }
            if (state.categoryDraft.editingCategoryId != null) {
                OutlinedButton(onClick = viewModel::cancelCategoryEdit, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
            }
        }
        state.snapshot.categories.forEach { category ->
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${category.icon} ${category.name}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    AssistChip(onClick = { viewModel.moveCategoryUp(category.id) }, label = { Text("Up") })
                    Spacer(Modifier.width(6.dp))
                    AssistChip(onClick = { viewModel.moveCategoryDown(category.id) }, label = { Text("Down") })
                    Spacer(Modifier.width(6.dp))
                    AssistChip(onClick = { viewModel.editCategory(category) }, label = { Text("Edit") })
                    if (!category.archived) {
                        Spacer(Modifier.width(6.dp))
                        AssistChip(onClick = { viewModel.archiveCategory(category.id) }, label = { Text("Archive") })
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySettings(state: SpendWiseUiState, viewModel: SpendWiseViewModel) {
    Column {
        Text("Base currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            supportedCurrencies.forEach { currency ->
                FilterChip(
                    selected = state.baseCurrencyCode == currency,
                    onClick = { viewModel.setBaseCurrency(currency) },
                    label = { Text(currency) }
                )
            }
        }
    }
}

@Composable
private fun LanguageSettings(state: SpendWiseUiState, viewModel: SpendWiseViewModel) {
    Column {
        Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = state.language == language,
                    onClick = { viewModel.setLanguage(language) },
                    label = { Text(language.label) }
                )
            }
        }
    }
}

private fun colorName(color: Long): String = when (color) {
    0xFFE76F51 -> "Coral"
    0xFF2A9D8F -> "Teal"
    0xFF457B9D -> "Blue"
    0xFFE9C46A -> "Gold"
    0xFF6D597A -> "Plum"
    else -> "Green"
}

private fun TagUsageSort.label(): String = when (this) {
    TagUsageSort.MostUsed -> "Most used"
    TagUsageSort.HighestSpending -> "Highest spending"
    TagUsageSort.RecentlyUsed -> "Recently used"
    TagUsageSort.Alphabetical -> "A-Z"
}
