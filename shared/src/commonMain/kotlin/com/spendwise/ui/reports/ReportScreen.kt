package com.spendwise.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryReportRow
import com.spendwise.ui.ReportPeriod
import com.spendwise.ui.SpendWiseUiState
import com.spendwise.ui.SpendWiseViewModel
import com.spendwise.ui.TagFilterBar
import com.spendwise.ui.formatCompactMoney
import com.spendwise.ui.formatMoney
import com.spendwise.ui.monthTitle
import com.spendwise.ui.signedMoney
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun ReportScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    val reportExpenses = state.snapshot.expenses.filter { expense ->
        val date = Instant.fromEpochMilliseconds(expense.spentAtMillis).toLocalDateTime(timeZone).date
        if (state.selectedReportPeriod == ReportPeriod.Month) {
            date.year == state.selectedMonth.year && date.month == state.selectedMonth.month
        } else {
            date.year == state.selectedMonth.year
        }
    }
    val rows = if (state.selectedReportPeriod == ReportPeriod.Month) {
        viewModel.getCategoryReport(reportExpenses)
    } else {
        viewModel.getYearlyCategoryReport(state.selectedMonth.year, timeZone)
    }
    val comparisonRows = viewModel.getMonthOverMonthReport(timeZone)
    val reportTotal = rows.sumOf { it.totalBaseAmountCents }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::previousMonth) {
                    Text("Prev")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = viewModel::nextMonth) {
                    Text("Next")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = viewModel::toggleReportPeriod) {
                    Text(if (state.selectedReportPeriod == ReportPeriod.Month) "Month" else "Year")
                }
            }
            Text(
                "${if (state.selectedReportPeriod == ReportPeriod.Month) monthTitle(state.selectedMonth) else state.selectedMonth.year.toString()} total ${
                    formatMoney(
                        reportTotal,
                        state.baseCurrencyCode
                    )
                }",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { TagFilterBar(state, viewModel) }
        item { CategoryPie(rows) }
        items(rows) { row -> CategoryReportRowView(row, state.baseCurrencyCode) }
        item {
            Text("Month-over-month", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        items(comparisonRows) { row ->
            MonthComparisonRowView(row.category, row.currentMonthAmountCents, row.previousMonthAmountCents, state.baseCurrencyCode, row.status)
        }
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
private fun CategoryReportRowView(row: CategoryReportRow, currencyCode: String) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(row.category.icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(row.category.name, fontWeight = FontWeight.SemiBold)
                Text("${(row.percentage * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatMoney(row.totalBaseAmountCents, currencyCode), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthComparisonRowView(
    category: Category,
    current: Long,
    previous: Long,
    currencyCode: String,
    status: String?
) {
    val max = maxOf(current, previous, 1L)
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${category.icon} ${category.name}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(status ?: "${signedMoney(current - previous, currencyCode)} ${changePercentLabel(current, previous)}")
            }
            ComparisonBar("This", current, max, currencyCode, MaterialTheme.colorScheme.primary)
            ComparisonBar("Prev", previous, max, currencyCode, MaterialTheme.colorScheme.tertiary)
        }
    }
}

private fun changePercentLabel(current: Long, previous: Long): String {
    if (previous == 0L) return ""
    val percent = ((current - previous).toDouble() / previous * 100).toInt()
    val sign = if (percent >= 0) "+" else ""
    return "($sign$percent%)"
}

@Composable
private fun ComparisonBar(label: String, amount: Long, max: Long, currencyCode: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(34.dp), style = MaterialTheme.typography.labelMedium)
        Box(Modifier.weight(1f).height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))) {
            Box(
                Modifier
                    .fillMaxWidth((amount.toFloat() / max.toFloat()).coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )
        }
        Text(formatCompactMoney(amount, currencyCode), modifier = Modifier.width(74.dp), style = MaterialTheme.typography.labelMedium)
    }
}
