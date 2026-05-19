package com.spendwise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.spendwise.ui.CurrencyMenu
import com.spendwise.ui.SpendWiseUiState
import com.spendwise.ui.SpendWiseViewModel

private enum class OthersPane {
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
    Column {
        Text("Base currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        CurrencyMenu(
            selected = state.baseCurrencyCode,
            onSelected = viewModel::setBaseCurrency,
            modifier = Modifier.fillMaxWidth(),
            label = "Base currency"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSettings(state: SpendWiseUiState, viewModel: SpendWiseViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.language.label,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label) },
                        onClick = {
                            expanded = false
                            viewModel.setLanguage(language)
                        }
                    )
                }
            }
        }
    }
}
