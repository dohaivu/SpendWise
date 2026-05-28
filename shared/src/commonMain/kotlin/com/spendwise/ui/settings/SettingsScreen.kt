package com.spendwise.ui.settings

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spendwise.ui.AppLanguage
import com.spendwise.ui.SettingsUiState
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.components.formatMoney
import com.spendwise.ui.supportedCurrencies
import org.jetbrains.compose.resources.stringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.*

@Composable
internal fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit
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
                            onReminders = {
                                push(SettingsRoute.Reminders)
                            }
                        )
                    }

                    SettingsRoute.CategoryList -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        Categories(
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
                        TagUsage(
                            state = currentState,
                            viewModel = settingsViewModel,
                            onBack = { pop() },
                            onTagClick = onTagClick
                        )
                    }

                    SettingsRoute.Reminders -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        Reminders(
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
        content(Modifier.padding(top = padding.calculateTopPadding()))
    }
}

@Composable
private fun SettingsHomeScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onEditCategories: () -> Unit,
    onTagUsage: () -> Unit,
    onReminders: () -> Unit
) {
    SettingsScaffold(title = stringResource(Res.string.settings_title)) { contentModifier ->
        Column(
            modifier = contentModifier.fillMaxSize().padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                item {
                    SettingsRow(
                        title = stringResource(Res.string.categories),
                        subtitle = stringResource(Res.string.categories_count, state.categories.size),
                        onClick = onEditCategories
                    )
                }
                item { CurrencySettings(state, viewModel) }
                item { LanguageSettings(state, viewModel) }
                item {
                    SettingsRow(
                        title = stringResource(Res.string.reminders),
                        subtitle = if (state.reminders.isEmpty()) {
                            stringResource(Res.string.no_reminders)
                        } else {
                            stringResource(
                                Res.string.reminders_enabled_count,
                                state.reminders.count { it.enabled },
                                state.reminders.size
                            )
                        },
                        onClick = onReminders
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(Res.string.tags),
                        subtitle = stringResource(Res.string.tracked_tags_count, state.tagUsage.size),
                        onClick = onTagUsage
                    )
                }
                item {
                    Column(modifier = Modifier.padding(top = 18.dp)) {
                        Text(stringResource(Res.string.data), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(
                                Res.string.data_summary,
                                state.expenses.size,
                                state.categories.size,
                                state.tagUsage.size
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item { DataTransferSettings(state, viewModel) }
            }
            Text(
                text = stringResource(Res.string.version, viewModel.versionName),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        HorizontalDivider()
    }
}

@Composable
private fun CurrencySettings(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    val format = state.baseCurrency

    SettingValueRow(
        title = stringResource(Res.string.currency_title),
        value = stringResource(Res.string.currency_value, format.symbol, format.code),
        onClick = { showDialog = true }
    )
    if (showDialog) {
        CurrencySelectionDialog(
            selected = state.baseCurrency.code,
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
        title = stringResource(Res.string.language),
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        HorizontalDivider()
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
        title = { Text(stringResource(Res.string.base_currency)) },
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
                Text(stringResource(Res.string.cancel))
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
        title = { Text(stringResource(Res.string.language)) },
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
                Text(stringResource(Res.string.cancel))
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
                    stringResource(Res.string.currency_option, format.code, format.name),
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
