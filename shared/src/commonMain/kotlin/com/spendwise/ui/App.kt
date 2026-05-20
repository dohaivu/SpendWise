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
import com.spendwise.ui.calendar.CalendarScreen
import com.spendwise.ui.calendar.CalendarViewModel
import com.spendwise.ui.expense.ExpenseViewModel
import com.spendwise.ui.expense.ExpenseScreen
import com.spendwise.ui.reports.CategoryReportScreen
import com.spendwise.ui.reports.ReportScreen
import com.spendwise.ui.reports.ReportViewModel
import com.spendwise.ui.settings.SettingsScreen
import com.spendwise.ui.settings.SettingsViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

private data object Routes {
    @Serializable
    data object Expense : NavKey

    @Serializable
    data object Calendar : NavKey

    @Serializable
    data object Report : NavKey

    @Serializable
    data object Settings : NavKey

    @Serializable
    data class CategoryReport(val categoryId: Long) : NavKey
}

@Composable
fun SpendWiseApp(
    expenseViewModel: ExpenseViewModel = koinViewModel(),
    calendarViewModel: CalendarViewModel = koinViewModel(),
    reportViewModel: ReportViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val backStack = remember { mutableStateListOf<NavKey>(Routes.Expense) }
    val expenseState by expenseViewModel.uiState.collectAsState()
    val calendarState by calendarViewModel.uiState.collectAsState()
    val reportState by reportViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    val currentRoute = backStack.lastOrNull() ?: Routes.Expense
    val selectedTab = currentRoute.asTab()

    LaunchedEffect(expenseState.message, settingsState.message) {
        val message = expenseState.message ?: settingsState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        if (expenseState.message != null) {
            expenseViewModel.consumeMessage()
        }
        if (settingsState.message != null) {
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
                    if (currentRoute !is Routes.CategoryReport) {
                        NavigationBar {
                            SpendWiseTab.entries.forEach { tab ->
                                val route = tab.route()
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { resetTo(route) },
                                    icon = { Icon(tab.icon(), contentDescription = tab.name) },
                                    label = { Text(tab.label(settingsState.language)) }
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
                                Routes.Expense -> ExpenseScreen(expenseState, expenseViewModel)
                                Routes.Calendar -> CalendarScreen(
                                    state = calendarState,
                                    calendarViewModel = calendarViewModel,
                                    onExpenseClick = { expense ->
                                        expenseViewModel.editExpense(expense)
                                        resetTo(Routes.Expense)
                                    }
                                )
                                Routes.Report -> ReportScreen(
                                    state = reportState,
                                    reportViewModel = reportViewModel,
                                    onCategoryClick = { categoryId ->
                                        push(Routes.CategoryReport(categoryId))
                                    }
                                )
                                Routes.Settings -> SettingsScreen(
                                    state = settingsState,
                                    reportState = reportState,
                                    settingsViewModel = settingsViewModel,
                                    reportViewModel = reportViewModel
                                )
                                is Routes.CategoryReport -> {
                                    val category = reportState.categories.firstOrNull { it.id == key.categoryId }
                                    if (category != null) {
                                        CategoryReportScreen(
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
    Routes.Calendar -> SpendWiseTab.Calendar
    Routes.Report,
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
