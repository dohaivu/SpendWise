package com.spendwise.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.YearHeader
import kotlinx.datetime.TimeZone

@Composable
internal fun AnnualCategoryReport(
    state: ReportUiState,
    reportViewModel: ReportViewModel,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val year = state.selectedMonth.year
    val rows = reportViewModel.getYearlyCategoryReport(year, TimeZone.currentSystemDefault())
    val total = rows.sumOf { it.totalBaseAmountCents }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                YearHeader(
                    year = year,
                    onPreviousYear = reportViewModel::previousYear,
                    onNextYear = reportViewModel::nextYear,
                    onCurrentYear = reportViewModel::resetToCurrentPeriod
                )
            }
        }
        item {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                CategoryPie(rows)
            }
            CategoryReportTotalRow(total = total, currencyCode = state.baseCurrency.code)
        }
        items(rows, key = { it.category.id }) { row ->
            CategoryReportRowView(
                row = row,
                currencyCode = state.baseCurrency.code,
                onClick = { onCategoryClick(row.category.id) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}
