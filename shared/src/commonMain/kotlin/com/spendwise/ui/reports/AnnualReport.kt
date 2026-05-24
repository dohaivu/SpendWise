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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendwise.domain.MonthlyExpenseTotal
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.MoneyText
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.YearHeader
import com.spendwise.ui.components.currencyDisplayFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TinyTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        "Annual Report",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
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
                    onNextYear = reportViewModel::nextYear
                )
                AnnualColumnChart(
                    rows = monthlyTotals,
                    currencyCode = state.baseCurrencyCode,
                    modifier = Modifier.fillMaxWidth().height(230.dp)
                )
            }
            AnnualTotalRow(total = total, currencyCode = state.baseCurrencyCode)
            SectionDivider()
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(monthlyTotals, key = { it.monthNumber }) { row ->
                    AnnualMonthRow(
                        row = row,
                        currencyCode = state.baseCurrencyCode,
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
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val maxAmount = rows.maxOfOrNull { it.totalBaseAmountCents } ?: 0L
    val axisMax = maxAmount.coerceAtLeast(1L)
    val gridValues = listOf(axisMax, axisMax * 3 / 4, axisMax / 2, axisMax / 4, 0L)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    val barColor = Color(0xFF50A8E5)
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
private fun AnnualTotalRow(total: Long, currencyCode: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Total",
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
            monthFullNames[row.monthNumber - 1],
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

private val monthShortNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val monthFullNames = listOf(
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December"
)
