package com.spendwise.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.TinyTopAppBar

@Composable
internal fun ReportScreen(
    state: ReportUiState,
    reportViewModel: ReportViewModel,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReport by rememberSaveable { mutableStateOf(CategoryReportPeriod.Month) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TinyTopAppBar(
                title = {
                    Text("Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CategoryReportSwitcher(
                selectedReport = selectedReport,
                onReportSelected = { selectedReport = it }
            )
            when (selectedReport) {
                CategoryReportPeriod.Month -> MonthCategoryReport(
                    state = state,
                    reportViewModel = reportViewModel,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.weight(1f)
                )
                CategoryReportPeriod.Annual -> AnnualCategoryReport(
                    state = state,
                    reportViewModel = reportViewModel,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryReportSwitcher(
    selectedReport: CategoryReportPeriod,
    onReportSelected: (CategoryReportPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryReportPeriod.entries.forEach { report ->
            FilterChip(
                selected = selectedReport == report,
                onClick = { onReportSelected(report) },
                label = { Text(report.label) }
            )
        }
    }
}

private enum class CategoryReportPeriod(val label: String) {
    Month("Monthly"),
    Annual("Annual")
}
