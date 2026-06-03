package com.spendwise.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendwise.domain.MonthlyExpenseTotal
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.MoneyText
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.TransactionFiltersMenu
import com.spendwise.ui.components.YearHeader
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.localizedMonthName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnnualReport(
    state: ReportUiState,
    reportViewModel: ReportViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onMonthClick: (LocalDate) -> Unit
) {
    val year = state.selectedMonth.year
    val monthlyTotals = reportViewModel.getAnnualMonthlyReport(year, TimeZone.currentSystemDefault())
    val total = monthlyTotals.sumOf { it.totalBaseAmountCents }
    val averageAmount = activeMonthlyAverage(monthlyTotals)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                title = {
                    Text(
                        stringResource(Res.string.annual_report),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    TransactionFiltersMenu(
                        categories = state.categories,
                        tagUsage = state.tagUsage,
                        filters = state.transactionFilters,
                        onTagClick = reportViewModel::toggleTagFilter,
                        onQueryChange = reportViewModel::updateTransactionQuery,
                        onCategoryChange = reportViewModel::updateTransactionCategory
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                YearHeader(
                    year = year,
                    onPreviousYear = reportViewModel::previousYear,
                    onNextYear = reportViewModel::nextYear,
                    onCurrentYear = reportViewModel::resetToCurrentPeriod
                )
                AnnualColumnChart(
                    rows = monthlyTotals,
                    averageAmount = averageAmount,
                    currencyCode = state.baseCurrency.code,
                    modifier = Modifier.fillMaxWidth().height(230.dp)
                )
            }
            AnnualTotalRow(total = total, averageAmount = averageAmount, currencyCode = state.baseCurrency.code)
            SectionDivider()
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(monthlyTotals, key = { it.monthNumber }) { row ->
                    AnnualMonthRow(
                        row = row,
                        currencyCode = state.baseCurrency.code,
                        onClick = { onMonthClick(LocalDate(year, row.monthNumber, 1)) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AnnualColumnChart(
    rows: List<MonthlyExpenseTotal>,
    averageAmount: Long,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val maxAmount = rows.maxOfOrNull { it.totalBaseAmountCents } ?: 0L
    val axisMax = maxAmount.coerceAtLeast(1L)
    val gridValues = listOf(axisMax, axisMax * 3 / 4, axisMax / 2, axisMax / 4, 0L)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)
    val averageLineColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val format = currencyDisplayFormat(currencyCode)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.width(36.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                gridValues.forEach { value ->
                    Text(
                        text = format.formatCompact(value),
                        color = labelColor,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Canvas(modifier = Modifier.weight(1f).fillMaxSize()) {
                val gridCount = 4
                val chartHeight = size.height
                val chartWidth = size.width
                for (index in 0..gridCount) {
                    val y = chartHeight * index / gridCount
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f
                    )
                }
                val slotWidth = chartWidth / rows.size.coerceAtLeast(1)
                val barWidth = slotWidth * 0.58f
                rows.forEachIndexed { index, row ->
                    if (row.totalBaseAmountCents > 0L) {
                        val fraction = row.totalBaseAmountCents.toFloat() / axisMax
                        val barHeight = chartHeight * fraction
                        val left = slotWidth * index + (slotWidth - barWidth) / 2f
                        drawRect(
                            color = barColor,
                            topLeft = Offset(left, chartHeight - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
                if (averageAmount > 0f) {
                    val averageY = chartHeight -
                        (chartHeight * (averageAmount.toFloat() / axisMax.toFloat()).coerceIn(0f, 1f))
                    drawLine(
                        color = averageLineColor,
                        start = Offset(0f, averageY),
                        end = Offset(chartWidth, averageY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(start = 36.dp)) {
            (1..12).forEach { monthNumber ->
                Text(
                    text = monthNumber.toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = labelColor,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AnnualTotalRow(total: Long, averageAmount: Long, currencyCode: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.total),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            MoneyText(
                amountCents = total,
                currencyCode = currencyCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.average),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoneyText(
                amountCents = averageAmount,
                currencyCode = currencyCode,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun activeMonthlyAverage(rows: List<MonthlyExpenseTotal>): Long {
    val activeRows = rows.filter { it.totalBaseAmountCents > 0L }
    return activeRows.sumOf { it.totalBaseAmountCents } / activeRows.size.coerceAtLeast(1)
}

@Composable
private fun AnnualMonthRow(
    row: MonthlyExpenseTotal,
    currencyCode: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            localizedMonthName(row.monthNumber),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        MoneyText(
            amountCents = row.totalBaseAmountCents,
            currencyCode = currencyCode,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionDivider() {
    Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant))
//    HorizontalDivider()
}
