package com.spendwise.ui.reports

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.ReportPeriod
import com.spendwise.ui.components.ReportPeriodSwitcher
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.TransactionFiltersMenu

@Composable
internal fun ReportScreen(
    state: ReportUiState,
    reportViewModel: ReportViewModel,
    onCategoryClick: (Long) -> Unit,
    onAnnualReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReport by rememberSaveable { mutableStateOf(ReportPeriod.Month) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            var showOverflowMenu by remember { mutableStateOf(false) }
            TinyTopAppBar(
                title = {
                    Text("Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    TransactionFiltersMenu(
                        categories = state.categories,
                        tagUsage = state.tagUsage,
                        filters = state.transactionFilters,
                        onTagClick = reportViewModel::toggleTagFilter,
                        onQueryChange = reportViewModel::updateTransactionQuery,
                        onCategoryChange = reportViewModel::updateTransactionCategory,
                        showCategories = false
                    )
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Annual Report") },
                                onClick = {
                                    showOverflowMenu = false
                                    onAnnualReportClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Analytics, contentDescription = "Annual Report")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            ReportPeriodSwitcher(
                selectedPeriod = selectedReport,
                onPeriodSelected = { selectedReport = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp)
            )
            when (selectedReport) {
                ReportPeriod.Month -> MonthCategoryReport(
                    state = state,
                    reportViewModel = reportViewModel,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.weight(1f)
                )
                ReportPeriod.Annual -> AnnualCategoryReport(
                    state = state,
                    reportViewModel = reportViewModel,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
