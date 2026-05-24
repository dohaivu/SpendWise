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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.CategoryIcon
import com.spendwise.ui.components.MoneyText
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.formatCompactMoney
import com.spendwise.ui.components.formatMoney
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
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
    val categoryExpenses = state.expenses
        .filter { it.categoryId == category.id }
        .filter { it.matchesSelectedTags(state.selectedTags) }
    val monthExpenses = categoryExpenses
        .filter { it.spentDate(timeZone).isSameMonth(state.selectedMonth) }
        .sortedByDescending { it.spentAtMillis }
    val monthTotal = monthExpenses.sumOf { it.baseAmountCents }
    val monthTotals = recentMonths(state.selectedMonth, count = 6).map { month ->
        CategoryMonthTotal(
            month = month,
            total = categoryExpenses
                .filter { it.spentDate(timeZone).isSameMonth(month) }
                .sumOf { it.baseAmountCents }
        )
    }
    val groupedTransactions = monthExpenses.groupBy { it.spentDate(timeZone) }.toList()

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
                        text = "${category.name} (${state.selectedMonth.shortMonthName()}) ${formatMoney(monthTotal, state.baseCurrencyCode)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                CategoryMonthlyBarChart(
                    monthTotals = monthTotals,
                    selectedMonth = state.selectedMonth,
                    color = Color(category.color.toInt()),
                    currencyCode = state.baseCurrencyCode
                )
                Spacer(Modifier.height(8.dp))
            }
            if (groupedTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            groupedTransactions.forEach { (date, expenses) ->
                item(key = "category-header-$date") {
                    CategoryTransactionDateHeader(
                        date = date,
                        total = expenses.sumOf { it.baseAmountCents },
                        currencyCode = state.baseCurrencyCode
                    )
                }
                items(expenses, key = { it.id }) { expense ->
                    CategoryTransactionRow(
                        expense = expense,
                        category = category,
                        currencyCode = state.baseCurrencyCode,
                        onClick = { onExpenseClick(expense) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryMonthlyBarChart(
    monthTotals: List<CategoryMonthTotal>,
    selectedMonth: LocalDate,
    color: Color,
    currencyCode: String
) {
    val chartHeight = 230.dp
    val maxValue = niceChartMax(monthTotals.maxOfOrNull { it.total } ?: 0L)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier.width(88.dp).height(chartHeight),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yAxisValues(maxValue).asReversed().forEach { value ->
                    Text(
                        text = formatMoney(value, currencyCode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = labelColor,
                        maxLines = 1
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(chartHeight)
                    .padding(start = 8.dp)
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
                            currencyCode = currencyCode,
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
                .padding(start = 96.dp, top = 4.dp),
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
    currencyCode: String,
    chartHeight: Dp,
    modifier: Modifier = Modifier
) {
    val barHeight = ((chartHeight - 30.dp) * (total.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))

    Column(
        modifier = modifier.height(chartHeight),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (total > 0L) {
            Text(
                text = formatCompactMoney(total, currencyCode),
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

@Composable
private fun CategoryTransactionDateHeader(
    date: LocalDate,
    total: Long,
    currencyCode: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.detailDateWithDayName(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatMoney(-total, currencyCode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CategoryTransactionRow(
    expense: Expense,
    category: Category,
    currencyCode: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.padding(end = 14.dp), contentAlignment = Alignment.Center) {
            CategoryIcon(
                iconKey = category.icon,
                tint = Color(category.color.toInt()),
                modifier = Modifier.size(26.dp)
            )
        }
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (expense.note.isNotBlank()) {
                Text(
                    text = "  ${expense.note}",
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        MoneyText(
            amountCents = expense.baseAmountCents,
            currencyCode = currencyCode,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1
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

private fun yAxisValues(maxValue: Long): List<Long> =
    List(6) { index -> maxValue / 5 * index }

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

private fun LocalDate.detailDateWithDayName(): String =
    "${month.number}.$day $year (${dayOfWeek.shortName()})"

private fun LocalDate.shortMonthName(): String =
    month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

private fun LocalDate.axisLabel(): String {
    val monthLabel = shortMonthName()
    return if (month.number == 1) "$monthLabel $year" else monthLabel
}

private fun DayOfWeek.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
