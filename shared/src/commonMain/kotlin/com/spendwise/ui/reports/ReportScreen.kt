package com.spendwise.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.spendwise.domain.CategoryReportRow
import com.spendwise.domain.Expense
import com.spendwise.ui.MonthHeader
import com.spendwise.ui.SpendWiseUiState
import com.spendwise.ui.SpendWiseViewModel
import com.spendwise.ui.components.MoneyText
import com.spendwise.ui.components.TagFilterBar
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    val reportExpenses = state.snapshot.expenses.filter { expense ->
        expense.spentDate(timeZone).isSameMonth(state.selectedMonth)
    }
    val rows = viewModel.getCategoryReport(reportExpenses)
    val total = rows.sumOf { it.totalBaseAmountCents }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MonthHeader(
                        month = state.selectedMonth,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth
                    )
                    TagFilterBar(state, viewModel)
                }
            }
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    CategoryPie(rows)
                }
                ReportTotalRow(total = total, currencyCode = state.baseCurrencyCode)
            }
            items(rows, key = { it.category.id }) { row ->
                CategoryReportRowView(
                    row = row,
                    currencyCode = state.baseCurrencyCode,
                    onClick = { viewModel.openReportCategory(row.category.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReportTotalRow(total: Long, currencyCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Total",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        MoneyText(
            amountCents = total,
            currencyCode = currencyCode,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CategoryPie(rows: List<CategoryReportRow>) {
    val modelProducer = remember { PieChartModelProducer() }
    LaunchedEffect(rows) {
        modelProducer.runTransaction {
            pieSeries {
                series(rows.map { (it.totalBaseAmountCents / 100.0).coerceAtLeast(0.01) })
            }
        }
    }
    val pieChart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(
            rows.map { row ->
                PieChart.Slice(fill = Fill(Color(row.category.color.toInt())))
            }.ifEmpty {
                listOf(PieChart.Slice(fill = Fill(Color.Transparent)))
            }
        ),
        innerSize = PieSize.Inner.fixed(74.dp),
        spacing = 2.dp
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
                if (rows.isEmpty()) {
                    Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    PieChartHost(
                        chart = pieChart,
                        modelProducer = modelProducer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                rows.take(4).forEach { row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Color(row.category.color.toInt()), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(row.category.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(row.percentage * 100).toInt()}%")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryReportRowView(
    row: CategoryReportRow,
    currencyCode: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            row.category.icon,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(40.dp)
        )
        Text(
            row.category.name,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            amountCents = row.totalBaseAmountCents,
            currencyCode = currencyCode,
            fontWeight = FontWeight.Medium
        )
        Text(
            percentLabel(row.percentage),
            modifier = Modifier.width(52.dp).padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun percentLabel(percentage: Double): String {
    val tenths = (percentage * 1000).roundToInt()
    return if (tenths % 10 == 0) {
        "${tenths / 10}%"
    } else {
        "${tenths / 10}.${tenths % 10}%"
    }
}

private fun Expense.spentDate(timeZone: TimeZone): LocalDate =
    kotlin.time.Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

private fun LocalDate.isSameMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month
