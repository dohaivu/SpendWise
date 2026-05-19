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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private enum class OthersPane {
    Home,
    CategoryList,
    CategoryEditor
}

@Composable
internal fun OthersScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    var pane by remember { mutableStateOf(OthersPane.Home) }

    when (pane) {
        OthersPane.Home -> OthersHomeScreen(
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            onEditCategories = {
                viewModel.cancelCategoryEdit()
                pane = OthersPane.CategoryList
            }
        )

        OthersPane.CategoryList -> EditCategoriesScreen(
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            onBack = { pane = OthersPane.Home },
            onAdd = {
                viewModel.cancelCategoryEdit()
                pane = OthersPane.CategoryEditor
            },
            onEdit = { category ->
                viewModel.editCategory(category)
                pane = OthersPane.CategoryEditor
            }
        )

        OthersPane.CategoryEditor -> CategoryEditorScreen(
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            onBack = { pane = OthersPane.CategoryList },
            onSaved = { pane = OthersPane.CategoryList }
        )
    }
}

@Composable
private fun OthersHomeScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier,
    onEditCategories: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Others", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            SettingsRow(
                title = "Edit Categories",
                subtitle = "${state.snapshot.categories.count { !it.archived }} active categories",
                onClick = onEditCategories
            )
        }
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
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
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

private fun TagUsageSort.label(): String = when (this) {
    TagUsageSort.MostUsed -> "Most used"
    TagUsageSort.HighestSpending -> "Highest spending"
    TagUsageSort.RecentlyUsed -> "Recently used"
    TagUsageSort.Alphabetical -> "A-Z"
}
