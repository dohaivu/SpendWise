package com.spendwise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spendwise.ui.AppLanguage
import com.spendwise.ui.SpendWiseUiState
import com.spendwise.ui.SpendWiseViewModel
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.components.formatMoney
import com.spendwise.ui.supportedCurrencies

internal enum class OthersPane {
    Home,
    CategoryList,
    CategoryEditor,
    TagUsage
}

@Composable
internal fun OthersScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    var pane by remember { mutableStateOf(OthersPane.Home) }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = backState,
        isBackEnabled = pane.backDestination() != null,
        onBackCompleted = {
            pane.backDestination()?.let { pane = it }
        }
    )

    when (pane) {
        OthersPane.Home -> OthersHomeScreen(
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            onEditCategories = {
                viewModel.cancelCategoryEdit()
                pane = OthersPane.CategoryList
            },
            onTagUsage = {
                pane = OthersPane.TagUsage
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

        OthersPane.TagUsage -> TagUsageScreen(
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            onBack = { pane = OthersPane.Home }
        )
    }
}

internal fun OthersPane.backDestination(): OthersPane? = when (this) {
    OthersPane.Home -> null
    OthersPane.CategoryList,
    OthersPane.TagUsage -> OthersPane.Home
    OthersPane.CategoryEditor -> OthersPane.CategoryList
}

@Composable
private fun OthersHomeScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier,
    onEditCategories: () -> Unit,
    onTagUsage: () -> Unit
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
            SettingsRow(
                title = "Tag usage",
                subtitle = "${state.snapshot.tagUsage.size} tracked tags",
                onClick = onTagUsage
            )
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
    var showDialog by remember { mutableStateOf(false) }
    val format = currencyDisplayFormat(state.baseCurrencyCode)

    SettingValueRow(
        title = "Base currency",
        value = "${format.symbol} ${format.code}",
        onClick = { showDialog = true }
    )
    if (showDialog) {
        CurrencySelectionDialog(
            selected = state.baseCurrencyCode,
            onDismiss = { showDialog = false },
            onSelected = { currency ->
                viewModel.setBaseCurrency(currency)
                showDialog = false
            }
        )
    }
}

@Composable
private fun LanguageSettings(state: SpendWiseUiState, viewModel: SpendWiseViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    SettingValueRow(
        title = "Language",
        value = state.language.label,
        onClick = { showDialog = true }
    )
    if (showDialog) {
        LanguageSelectionDialog(
            selected = state.language,
            onDismiss = { showDialog = false },
            onSelected = { language ->
                viewModel.setLanguage(language)
                showDialog = false
            }
        )
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun CurrencySelectionDialog(
    selected: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Base currency") },
        text = {
            Column {
                supportedCurrencies.forEach { currency ->
                    CurrencySelectionRow(
                        currencyCode = currency,
                        selected = selected == currency,
                        onClick = { onSelected(currency) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LanguageSelectionDialog(
    selected: AppLanguage,
    onDismiss: () -> Unit,
    onSelected: (AppLanguage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Language") },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    SelectionRow(
                        text = language.label,
                        selected = selected == language,
                        onClick = { onSelected(language) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CurrencySelectionRow(
    currencyCode: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val format = currencyDisplayFormat(currencyCode)
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                format.symbol,
                modifier = Modifier.weight(0.4f),
                style = MaterialTheme.typography.titleMedium
            )
            Column(modifier = Modifier.weight(1.8f)) {
                Text(formatMoney(CURRENCY_FORMAT_SAMPLE_CENTS, currencyCode))
                Text(
                    "${format.code} • ${format.name}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun SelectionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Text(text, modifier = Modifier.weight(1f))
        }
    }
}

private const val CURRENCY_FORMAT_SAMPLE_CENTS = 12_345_678_900L
