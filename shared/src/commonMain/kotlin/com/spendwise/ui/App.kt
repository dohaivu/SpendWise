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
import com.spendwise.ui.reports.CategoryReportScreen
import com.spendwise.ui.reports.ReportScreen
import com.spendwise.ui.settings.OthersScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpendWiseApp(
    viewModel: SpendWiseViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    val reportCategory = state.selectedReportCategoryId?.let { categoryId ->
        state.snapshot.categories.firstOrNull { it.id == categoryId }
    }
    val isReportCategoryDetail = state.selectedTab == SpendWiseTab.Report && reportCategory != null

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    NavigationBackHandler(
        state = backState,
        isBackEnabled = state.selectedTab != SpendWiseTab.Input || isReportCategoryDetail,
        onBackCompleted = viewModel::handleBackNavigation
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
                                    selected = state.selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(tab.icon(), contentDescription = tab.name) },
                                    label = { Text(tab.label(state.language)) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                when (state.selectedTab) {
                    SpendWiseTab.Input -> InputScreen(state, viewModel, Modifier.padding(padding))
                    SpendWiseTab.Calendar -> CalendarScreen(state, viewModel, Modifier.padding(padding))
                    SpendWiseTab.Report -> {
                        val category = reportCategory
                        if (category != null) {
                            CategoryReportScreen(
                                state = state,
                                category = category,
                                onBack = viewModel::closeReportCategory,
                                onExpenseClick = viewModel::editExpense,
                                modifier = Modifier.padding(padding)
                            )
                        } else {
                            ReportScreen(state, viewModel, Modifier.padding(padding))
                        }
                    }
                    SpendWiseTab.Others -> OthersScreen(state, viewModel, Modifier.padding(padding))
                }
            }
        }
    }
}

private fun SpendWiseTab.icon() = when (this) {
    SpendWiseTab.Input -> Icons.Default.Add
    SpendWiseTab.Calendar -> Icons.Default.CalendarMonth
    SpendWiseTab.Report -> Icons.Default.BarChart
    SpendWiseTab.Others -> Icons.Default.MoreHoriz
}

private fun SpendWiseTab.label(language: AppLanguage): String = when (language) {
    AppLanguage.English -> name
    AppLanguage.Vietnamese -> when (this) {
        SpendWiseTab.Input -> "Nhập"
        SpendWiseTab.Calendar -> "Lịch"
        SpendWiseTab.Report -> "Báo cáo"
        SpendWiseTab.Others -> "Khác"
    }
    AppLanguage.Chinese -> when (this) {
        SpendWiseTab.Input -> "输入"
        SpendWiseTab.Calendar -> "日历"
        SpendWiseTab.Report -> "报表"
        SpendWiseTab.Others -> "其他"
    }
}
