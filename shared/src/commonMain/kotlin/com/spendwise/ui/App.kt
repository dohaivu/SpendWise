package com.spendwise.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.spendwise.ui.expense.ExpenseViewModel
import com.spendwise.ui.expense.ExpenseScreen
import com.spendwise.ui.reports.AnnualReport
import com.spendwise.ui.reports.CategoryReport
import com.spendwise.ui.reports.ReportScreen
import com.spendwise.ui.reports.ReportViewModel
import com.spendwise.ui.settings.SettingsScreen
import com.spendwise.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

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
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
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

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (currentRoute !is Routes.CategoryReport &&
                        currentRoute != Routes.AnnualReport &&
                        currentRoute != Routes.AllTransactions
                    ) {
                        NavigationBar {
                            SpendWiseTab.entries.forEach { tab ->
                                val route = tab.route()
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        if (route == Routes.Calendar) {
                                            calendarViewModel.resetToToday()
                                        }
                                        resetTo(route)
                                    },
                                    icon = { Icon(tab.icon(), contentDescription = tab.name) },
                                    label = { Text(tab.label(appLanguage)) }
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
                                        onBack = { onBack() }
                                    )
                                }
                                is Routes.CategoryReport -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    val category = reportState.categories.firstOrNull { it.id == key.categoryId }
                                    if (category != null) {
                                        CategoryReport(
                                            state = reportState,
                                            category = category,
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
                                    settingsViewModel = settingsViewModel
                                )
                            }
                        }
                    }
                )
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

private fun SpendWiseTab.label(language: AppLanguage): String = when (language) {
    AppLanguage.English -> name
    AppLanguage.Vietnamese -> when (this) {
        SpendWiseTab.Expense -> "Nhập"
        SpendWiseTab.Calendar -> "Lịch"
        SpendWiseTab.Report -> "Báo cáo"
        SpendWiseTab.Settings -> "Khác"
    }
    AppLanguage.Chinese -> when (this) {
        SpendWiseTab.Expense -> "输入"
        SpendWiseTab.Calendar -> "日历"
        SpendWiseTab.Report -> "报表"
        SpendWiseTab.Settings -> "其他"
    }
}
