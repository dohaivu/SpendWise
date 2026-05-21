package com.spendwise.ui.settings

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spendwise.ui.AppLanguage
import com.spendwise.ui.reports.ReportViewModel
import com.spendwise.ui.SettingsUiState
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.components.formatMoney
import com.spendwise.ui.reports.AnnualReportScreen
import com.spendwise.ui.supportedCurrencies

@Composable
internal fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    reportViewModel: ReportViewModel,
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf<NavKey>(SettingsRoute.Home) }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)

    fun push(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    NavigationBackHandler(
        state = backState,
        isBackEnabled = backStack.size > 1,
        onBackCompleted = { pop() }
    )

    NavDisplay(
        modifier = modifier.fillMaxSize(),
        backStack = backStack,
        onBack = { pop() },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryProvider = { route ->
            NavEntry(route) {
                when (route) {
                    SettingsRoute.Home -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        SettingsHomeScreen(
                            state = currentState,
                            viewModel = settingsViewModel,
                            onEditCategories = {
                                settingsViewModel.cancelCategoryEdit()
                                push(SettingsRoute.CategoryList)
                            },
                            onTagUsage = {
                                push(SettingsRoute.TagUsage)
                            },
                            onAnnualReport = {
                                push(SettingsRoute.AnnualReport)
                            }
                        )
                    }

                    SettingsRoute.AnnualReport -> {
                        val reportState by reportViewModel.uiState.collectAsState()
                        AnnualReportScreen(
                            state = reportState,
                            reportViewModel = reportViewModel,
                            onBack = { pop() }
                        )
                    }

                    SettingsRoute.CategoryList -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        EditCategoriesScreen(
                            state = currentState,
                            viewModel = settingsViewModel,
                            onBack = { pop() },
                            onAdd = {
                                settingsViewModel.cancelCategoryEdit()
                                push(SettingsRoute.CategoryEditor)
                            },
                            onEdit = { category ->
                                settingsViewModel.editCategory(category)
                                push(SettingsRoute.CategoryEditor)
                            }
                        )
                    }

                    SettingsRoute.CategoryEditor -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        CategoryEditorScreen(
                            state = currentState,
                            viewModel = settingsViewModel,
                            onBack = { pop() },
                            onSaved = { pop() }
                        )
                    }

                    SettingsRoute.TagUsage -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        TagUsageScreen(
                            state = currentState,
                            viewModel = settingsViewModel,
                            onBack = { pop() }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = navigationIcon,
                actions = { actions() }
            )
        }
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

@Composable
private fun SettingsHomeScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onEditCategories: () -> Unit,
    onTagUsage: () -> Unit,
    onAnnualReport: () -> Unit
) {
    SettingsScaffold(title = "Settings") { contentModifier ->
        LazyColumn(
            modifier = contentModifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsRow(
                    title = "Edit Categories",
                    subtitle = "${state.categories.count { !it.archived }} active categories",
                    onClick = onEditCategories
                )
            }
            item { CurrencySettings(state, viewModel) }
            item { LanguageSettings(state, viewModel) }
            item {
                SettingsRow(
                    title = "Tag usage",
                    subtitle = "${state.tagUsage.size} tracked tags",
                    onClick = onTagUsage
                )
            }
            item {
                SettingsRow(
                    title = "Annual Report",
                    subtitle = "Monthly spending totals",
                    onClick = onAnnualReport
                )
            }
            item {
                Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${state.expenses.size} expenses • ${state.categories.size} categories • ${state.tagUsage.size} tags",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
private fun CurrencySettings(state: SettingsUiState, viewModel: SettingsViewModel) {
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
private fun LanguageSettings(state: SettingsUiState, viewModel: SettingsViewModel) {
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
