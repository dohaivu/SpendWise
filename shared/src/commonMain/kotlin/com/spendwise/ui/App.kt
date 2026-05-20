package com.spendwise.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpendWiseApp(
    viewModel: AppViewModel = koinViewModel(),
    expenseViewModel: ExpenseViewModel = koinViewModel(),
    calendarViewModel: CalendarViewModel = koinViewModel(),
    reportViewModel: ReportViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val appState by viewModel.uiState.collectAsState()
    val expenseState by expenseViewModel.uiState.collectAsState()
    val calendarState by calendarViewModel.uiState.collectAsState()
    val reportState by reportViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    val reportCategory = reportState.selectedReportCategoryId?.let { categoryId ->
        reportState.categories.firstOrNull { it.id == categoryId }
    }
    val isReportCategoryDetail = appState.selectedTab == SpendWiseTab.Report && reportCategory != null

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

    NavigationBackHandler(
        state = backState,
        isBackEnabled = appState.selectedTab != SpendWiseTab.Expense || isReportCategoryDetail,
        onBackCompleted = {
            if (isReportCategoryDetail) {
                reportViewModel.closeReportCategory()
            } else {
                viewModel.handleBackNavigation()
            }
        }
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isReportCategoryDetail) {
                        NavigationBar {
                            SpendWiseTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = appState.selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(tab.icon(), contentDescription = tab.name) },
                                    label = { Text(tab.label(appState.language)) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                when (appState.selectedTab) {
                    SpendWiseTab.Expense -> ExpenseScreen(expenseState, expenseViewModel, Modifier.padding(padding))
                    SpendWiseTab.Calendar -> CalendarScreen(
                        state = calendarState,
                        calendarViewModel = calendarViewModel,
                        onExpenseClick = { expense ->
                            expenseViewModel.editExpense(expense)
                            viewModel.selectTab(SpendWiseTab.Expense)
                        },
                        modifier = Modifier.padding(padding)
                    )
                    SpendWiseTab.Report -> {
                        val category = reportCategory
                        if (category != null) {
                            CategoryReportScreen(
                                state = reportState,
                                category = category,
                                onBack = reportViewModel::closeReportCategory,
                                onExpenseClick = { expense ->
                                    expenseViewModel.editExpense(expense)
                                    viewModel.selectTab(SpendWiseTab.Expense)
                                },
                                modifier = Modifier.padding(padding)
                            )
                        } else {
                            ReportScreen(
                                state = reportState,
                                reportViewModel = reportViewModel,
                                modifier = Modifier.padding(padding)
                            )
                        }
                    }
                    SpendWiseTab.Settings -> SettingsScreen(
                        state = settingsState,
                        reportState = reportState,
                        settingsViewModel = settingsViewModel,
                        reportViewModel = reportViewModel,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
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
