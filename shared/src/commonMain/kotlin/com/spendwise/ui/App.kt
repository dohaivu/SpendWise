package com.spendwise.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spendwise.ui.calendar.AllTransactions
import com.spendwise.ui.calendar.AllTransactionsViewModel
import com.spendwise.ui.calendar.CalendarScreen
import com.spendwise.ui.calendar.CalendarViewModel
import com.spendwise.ui.components.ReportPeriod
import com.spendwise.ui.expense.ExpenseViewModel
import com.spendwise.ui.expense.ExpenseScreen
import com.spendwise.ui.localization.AppLocaleProvider
import com.spendwise.ui.reports.AnnualReport
import com.spendwise.ui.reports.CategoryReport
import com.spendwise.ui.reports.ReportScreen
import com.spendwise.ui.reports.ReportViewModel
import com.spendwise.ui.settings.SettingsScreen
import com.spendwise.ui.settings.SettingsViewModel
import com.spendwise.platform.BackupScheduler
import com.spendwise.ui.theme.SpendWiseTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.tab_calendar
import spendwise.shared.generated.resources.tab_expense
import spendwise.shared.generated.resources.tab_report
import spendwise.shared.generated.resources.tab_settings

private data object Routes {
    @Serializable
    data object Expense : NavKey

    @Serializable
    data object Calendar : NavKey

    @Serializable
    data object AllTransactions : NavKey

    @Serializable
    data object Report : NavKey

    @Serializable
    data object Settings : NavKey

    @Serializable
    data object AnnualReport : NavKey

    @Serializable
    data class CategoryReport(val categoryId: Long) : NavKey
}

@Composable
fun SpendWiseApp(
    expenseViewModel: ExpenseViewModel = koinViewModel(),
    calendarViewModel: CalendarViewModel = koinViewModel(),
    allTransactionsViewModel: AllTransactionsViewModel = koinViewModel(),
    reportViewModel: ReportViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    backupScheduler: BackupScheduler = koinInject(),
    onSelectBackupFolder: (() -> Unit)? = null,
    onRestoreFromFolder: (() -> Unit)? = null
) {
    LaunchedEffect(Unit) {
        backupScheduler.backupNow()
    }

    val backStack = remember { mutableStateListOf<NavKey>(Routes.Expense) }
    val expenseMessage by remember(expenseViewModel) {
        expenseViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val settingsMessage by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val appLanguage by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.language }.distinctUntilChanged()
    }.collectAsState(AppLanguage.English)
    val appThemeMode by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.themeMode }.distinctUntilChanged()
    }.collectAsState(AppThemeMode.System)
    val appColorSchemeMode by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.colorSchemeMode }.distinctUntilChanged()
    }.collectAsState(AppColorSchemeMode.Sunset)
    val snackbarHostState = remember { SnackbarHostState() }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    val currentRoute = backStack.lastOrNull() ?: Routes.Expense
    val selectedTab = currentRoute.asTab()

    LaunchedEffect(expenseMessage, settingsMessage) {
        val message = expenseMessage ?: settingsMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        if (expenseMessage != null) {
            expenseViewModel.consumeMessage()
        }
        if (settingsMessage != null) {
            settingsViewModel.consumeMessage()
        }
    }

    fun resetTo(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    fun push(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun onBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else if (backStack.lastOrNull() != Routes.Expense) {
            resetTo(Routes.Expense)
        }
    }

    NavigationBackHandler(
        state = backState,
        isBackEnabled = backStack.size > 1 || currentRoute != Routes.Expense,
        onBackCompleted = { onBack() }
    )

    AppLocaleProvider(appLanguage.code) {
        SpendWiseTheme(themeMode = appThemeMode, colorSchemeMode = appColorSchemeMode) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (currentRoute !is Routes.CategoryReport &&
                        currentRoute != Routes.AnnualReport &&
                        currentRoute != Routes.AllTransactions
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = NavigationBarDefaults.Elevation
                        ) {
                            SpendWiseTab.entries.forEach { tab ->
                                val route = tab.route()
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        resetTo(route)
                                    },
                                    icon = { Icon(tab.icon(), contentDescription = tab.label()) },
                                    label = { Text(tab.label()) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavDisplay(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    backStack = backStack,
                    onBack = { onBack() },
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    entryProvider = { key ->
                        NavEntry(key) {
                            when (key) {
                                Routes.Expense -> {
                                    val expenseState by expenseViewModel.uiState.collectAsState()
                                    ExpenseScreen(expenseState, expenseViewModel)
                                }
                                Routes.Calendar -> {
                                    val calendarState by calendarViewModel.uiState.collectAsState()
                                    CalendarScreen(
                                        state = calendarState,
                                        calendarViewModel = calendarViewModel,
                                        onExpenseClick = { expense ->
                                            expenseViewModel.editExpense(expense)
                                            resetTo(Routes.Expense)
                                        },
                                        onDateDoubleClick = { date ->
                                            expenseViewModel.cancelExpenseEdit()
                                            expenseViewModel.selectDateForDraft(date)
                                            resetTo(Routes.Expense)
                                        },
                                        onAllTransactionsClick = {
                                            push(Routes.AllTransactions)
                                        }
                                    )
                                }
                                Routes.AllTransactions -> {
                                    val allTransactionsState by allTransactionsViewModel.uiState.collectAsState()
                                    AllTransactions(
                                        state = allTransactionsState,
                                        viewModel = allTransactionsViewModel,
                                        onBack = { onBack() },
                                        onExpenseClick = { expense ->
                                            expenseViewModel.editExpense(expense)
                                            resetTo(Routes.Expense)
                                        }
                                    )
                                }
                                Routes.Report -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    ReportScreen(
                                        state = reportState,
                                        reportViewModel = reportViewModel,
                                        onCategoryClick = { categoryId ->
                                            push(Routes.CategoryReport(categoryId))
                                        },
                                        onAnnualReportClick = {
                                            push(Routes.AnnualReport)
                                        }
                                    )
                                }
                                Routes.AnnualReport -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    AnnualReport(
                                        state = reportState,
                                        reportViewModel = reportViewModel,
                                        onBack = { onBack() },
                                        onMonthClick = { month ->
                                            calendarViewModel.selectPeriod(ReportPeriod.Month)
                                            calendarViewModel.selectMonth(month)
                                            calendarViewModel.selectDate(month)
                                            push(Routes.Calendar)
                                        }
                                    )
                                }
                                is Routes.CategoryReport -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    val category = reportState.categories.firstOrNull { it.id == key.categoryId }
                                    if (category != null) {
                                        CategoryReport(
                                            state = reportState,
                                            category = category,
                                            reportViewModel = reportViewModel,
                                            onBack = { onBack() },
                                            onExpenseClick = { expense ->
                                                expenseViewModel.editExpense(expense)
                                                resetTo(Routes.Expense)
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(key.categoryId) {
                                            onBack()
                                        }
                                    }
                                }
                                Routes.Settings -> SettingsScreen(
                                    settingsViewModel = settingsViewModel,
                                    onTagClick = { tag ->
                                        allTransactionsViewModel.showOnlyTag(tag)
                                        push(Routes.AllTransactions)
                                    },
                                    onSelectBackupFolder = onSelectBackupFolder,
                                    onRestoreFromFolder = onRestoreFromFolder
                                )
                            }
                        }
                    }
                )
            }
        }
    }
    }
}

private fun NavKey.asTab(): SpendWiseTab? = when (this) {
    Routes.Expense -> SpendWiseTab.Expense
    Routes.Calendar,
    Routes.AllTransactions -> SpendWiseTab.Calendar
    Routes.Report,
    Routes.AnnualReport,
    is Routes.CategoryReport -> SpendWiseTab.Report
    Routes.Settings -> SpendWiseTab.Settings
    else -> null
}

private fun SpendWiseTab.route(): NavKey = when (this) {
    SpendWiseTab.Expense -> Routes.Expense
    SpendWiseTab.Calendar -> Routes.Calendar
    SpendWiseTab.Report -> Routes.Report
    SpendWiseTab.Settings -> Routes.Settings
}

private fun SpendWiseTab.icon() = when (this) {
    SpendWiseTab.Expense -> Icons.Default.Add
    SpendWiseTab.Calendar -> Icons.Default.CalendarMonth
    SpendWiseTab.Report -> Icons.Default.BarChart
    SpendWiseTab.Settings -> Icons.Default.MoreHoriz
}

@Composable
private fun SpendWiseTab.label(): String = when (this) {
    SpendWiseTab.Expense -> stringResource(Res.string.tab_expense)
    SpendWiseTab.Calendar -> stringResource(Res.string.tab_calendar)
    SpendWiseTab.Report -> stringResource(Res.string.tab_report)
    SpendWiseTab.Settings -> stringResource(Res.string.tab_settings)
}
