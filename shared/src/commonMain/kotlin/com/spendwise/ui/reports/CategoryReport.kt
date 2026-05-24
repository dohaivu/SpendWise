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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.domain.TagParser
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.TransactionsByDateList
import com.spendwise.ui.components.formatCompactAmount
import com.spendwise.ui.components.formatMoney
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryReport(
    state: ReportUiState,
    category: Category,
    onBack: () -> Unit,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    var selectedMonth by remember(state.selectedMonth) { mutableStateOf(state.selectedMonth) }
    val categoryExpenses = state.expenses
        .filter { it.categoryId == category.id }
        .filter { it.matchesSelectedTags(state.selectedTags) }
    val monthExpenses = categoryExpenses
        .filter { it.spentDate(timeZone).isSameMonth(selectedMonth) }
        .sortedByDescending { it.spentAtMillis }
    val monthTotal = monthExpenses.sumOf { it.baseAmountCents }
    val transactionListState = rememberLazyListState()
    val categoryById = mapOf(category.id to category)
    val monthTotals = recentMonths(state.selectedMonth, count = 6).map { month ->
        CategoryMonthTotal(
            month = month,
            total = categoryExpenses
                .filter { it.spentDate(timeZone).isSameMonth(month) }
                .sumOf { it.baseAmountCents }
        )
    }
    val transactionItems = monthExpenses
        .groupBy { it.spentDate(timeZone) }
        .toList()
        .flatMap { (date, expenses) ->
            listOf(DateTransactionListItem.Header(date, expenses.sumOf { it.baseAmountCents })) +
                expenses.map(DateTransactionListItem::Transaction)
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TinyTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = "${category.name} (${selectedMonth.shortMonthName()}) ${formatMoney(monthTotal, state.baseCurrencyCode)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            CategoryMonthlyBarChart(
                monthTotals = monthTotals,
                selectedMonth = selectedMonth,
                color = Color(category.color.toInt()),
                onMonthSelected = { selectedMonth = it }
            )
            Spacer(Modifier.height(4.dp))
            TransactionsByDateList(
                transactionItems = transactionItems,
                categoryById = categoryById,
                currencyCode = state.baseCurrencyCode,
                onExpenseClick = onExpenseClick,
                listState = transactionListState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryMonthlyBarChart(
    monthTotals: List<CategoryMonthTotal>,
    selectedMonth: LocalDate,
    color: Color,
    onMonthSelected: (LocalDate) -> Unit
) {
    val chartHeight = 230.dp
    val maxValue = niceChartMax(monthTotals.maxOfOrNull { it.total } ?: 0L)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(6) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(gridColor))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    monthTotals.forEach { monthTotal ->
                        val selected = monthTotal.month.isSameMonth(selectedMonth)
                        MonthlyBar(
                            total = monthTotal.total,
                            maxValue = maxValue,
                            color = if (selected) color else color.copy(alpha = 0.55f),
                            onClick = { onMonthSelected(monthTotal.month) },
                            chartHeight = chartHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            monthTotals.forEach { monthTotal ->
                Text(
                    text = monthTotal.month.axisLabel(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MonthlyBar(
    total: Long,
    maxValue: Long,
    color: Color,
    onClick: () -> Unit,
    chartHeight: Dp,
    modifier: Modifier = Modifier
) {
    val barHeight = ((chartHeight - 30.dp) * (total.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))

    Column(
        modifier = modifier
            .height(chartHeight)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (total > 0L) {
            Text(
                text = formatCompactAmount(total, displayMillions = false),
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        }
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(if (total > 0L) barHeight.coerceAtLeast(4.dp) else 0.dp)
                .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        )
    }
}

private data class CategoryMonthTotal(
    val month: LocalDate,
    val total: Long
)

private fun recentMonths(selectedMonth: LocalDate, count: Int): List<LocalDate> {
    return List(count) { index -> selectedMonth.minus(count - 1 - index, DateTimeUnit.MONTH) }
}

private fun niceChartMax(value: Long): Long {
    if (value <= 0L) return 100L
    val whole = (value + 99L) / 100L
    val step = when {
        whole <= 1_000L -> 1_000L
        whole <= 10_000L -> 5_000L
        whole <= 100_000L -> 10_000L
        whole <= 1_000_000L -> 100_000L
        whole <= 10_000_000L -> 1_000_000L
        else -> 10_000_000L
    }
    return (ceil(whole.toDouble() / step).toLong() * step).coerceAtLeast(step) * 100L
}

private fun Expense.matchesSelectedTags(selectedTags: Set<String>): Boolean {
    if (selectedTags.isEmpty()) return true
    val normalizedSelectedTags = selectedTags.map(TagParser::normalize).filter { it.isNotBlank() }.toSet()
    if (normalizedSelectedTags.isEmpty()) return true
    val normalizedTags = tags.map(TagParser::normalize).toSet()
    return normalizedTags.any { it in normalizedSelectedTags }
}

private fun Expense.spentDate(timeZone: TimeZone): LocalDate =
    kotlin.time.Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

private fun LocalDate.isSameMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month

private fun LocalDate.shortMonthName(): String =
    month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

private fun LocalDate.axisLabel(): String {
    val monthLabel = shortMonthName()
    return if (month.number == 1) "$monthLabel $year" else monthLabel
}
