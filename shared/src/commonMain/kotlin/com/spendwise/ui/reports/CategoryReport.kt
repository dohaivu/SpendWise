package com.spendwise.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.domain.usecase.filterByTransactionFilters
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.components.MoneyText
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.TransactionFiltersMenu
import com.spendwise.ui.components.TransactionsByDateList
import com.spendwise.ui.components.formatCompactAmount
import com.spendwise.ui.components.formatMoney
import com.spendwise.ui.isSameMonth
import com.spendwise.ui.localizedShortMonthName
import com.spendwise.ui.spentDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlin.math.ceil
import org.jetbrains.compose.resources.stringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.average
import spendwise.shared.generated.resources.back
import spendwise.shared.generated.resources.total

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryReport(
    state: ReportUiState,
    category: Category,
    reportViewModel: ReportViewModel,
    onBack: () -> Unit,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    var selectedMonth by remember(state.selectedMonth) { mutableStateOf(state.selectedMonth) }
    val categoryExpenses = state.expenses
        .filter { it.categoryId == category.id }
        .filterByTransactionFilters(state.transactionFilters.copy(categoryId = null))
    val monthExpenses = categoryExpenses
        .filter { it.spentDate(timeZone).isSameMonth(selectedMonth) }
        .sortedByDescending { it.spentAtMillis }
    val monthTotal = monthExpenses.sumOf { it.baseAmountCents }
    val transactionListState = rememberLazyListState()
    val categoryById = mapOf(category.id to category)
    val monthTotals = recentMonths(state.selectedMonth, count = 12).map { month ->
        CategoryMonthTotal(
            month = month,
            total = categoryExpenses
                .filter { it.spentDate(timeZone).isSameMonth(month) }
                .sumOf { it.baseAmountCents }
        )
    }
    val categoryPeriodTotal = monthTotals.sumOf { it.total }
    val categoryPeriodAverage = activeCategoryMonthlyAverage(monthTotals)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                title = {
                    Text(
                        text = "${category.name} (${selectedMonth.localizedShortMonthName()}) ${formatMoney(monthTotal, state.baseCurrency)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            CategoryMonthlyColumnChart(
                monthTotals = monthTotals,
                averageAmount = categoryPeriodAverage,
                selectedMonth = selectedMonth,
                color = Color(category.color.toInt()),
                onMonthSelected = { selectedMonth = it }
            )
            CategoryTotalRow(
                total = categoryPeriodTotal,
                averageAmount = categoryPeriodAverage,
                currencyCode = state.baseCurrency.code
            )
            Spacer(Modifier.height(4.dp))
            TransactionsByDateList(
                transactionItems = transactionItems,
                categoryById = categoryById,
                currencyCode = state.baseCurrency.code,
                onExpenseClick = onExpenseClick,
                listState = transactionListState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryMonthlyColumnChart(
    monthTotals: List<CategoryMonthTotal>,
    averageAmount: Long,
    selectedMonth: LocalDate,
    color: Color,
    onMonthSelected: (LocalDate) -> Unit
) {
    val chartHeight = 230.dp
    val maxValue = niceChartMax(monthTotals.maxOfOrNull { it.total } ?: 0L)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val averageLineColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    val valueLabelStyle = MaterialTheme.typography.labelMedium
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .pointerInput(monthTotals) {
                        detectTapGestures { offset ->
                            if (monthTotals.isNotEmpty()) {
                                val selectedIndex = (offset.x / (size.width / monthTotals.size))
                                    .toInt()
                                    .coerceIn(0, monthTotals.lastIndex)
                                onMonthSelected(monthTotals[selectedIndex].month)
                            }
                        }
                    }
            ) {
                val valueLabelHeight = 30.dp.toPx()
                val chartAreaHeight = size.height - valueLabelHeight
                val slotWidth = size.width / monthTotals.size.coerceAtLeast(1)
                val columnWidth = minOf(42.dp.toPx(), slotWidth * 0.72f)
                val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

                repeat(6) { index ->
                    val y = size.height * index / 5f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                monthTotals.forEachIndexed { index, monthTotal ->
                    if (monthTotal.total > 0L) {
                        val selected = monthTotal.month.isSameMonth(selectedMonth)
                        val columnColor = if (selected) color else color.copy(alpha = 0.55f)
                        val fraction = (monthTotal.total.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                        val columnHeight = (chartAreaHeight * fraction).coerceAtLeast(4.dp.toPx())
                        val left = slotWidth * index + (slotWidth - columnWidth) / 2f
                        val top = size.height - columnHeight
                        drawRoundRect(
                            color = columnColor,
                            topLeft = Offset(left, top),
                            size = Size(columnWidth, columnHeight),
                            cornerRadius = cornerRadius
                        )

                        val label = formatCompactAmount(monthTotal.total, displayMillions = false)
                        val textLayout = textMeasurer.measure(
                            text = label,
                            style = valueLabelStyle.copy(color = columnColor),
                            maxLines = 1
                        )
                        val labelX = (slotWidth * index + (slotWidth - textLayout.size.width) / 2f)
                            .coerceIn(0f, size.width - textLayout.size.width)
                        val labelY = (top - 4.dp.toPx() - textLayout.size.height)
                            .coerceAtLeast(0f)
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(labelX, labelY)
                        )
                    }
                }

                if (averageAmount > 0L) {
                    val averageY = size.height -
                        (chartAreaHeight * (averageAmount.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))
                    drawLine(
                        color = averageLineColor,
                        start = Offset(0f, averageY),
                        end = Offset(size.width, averageY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
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
                    text = "${monthTotal.month.month.number}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CategoryTotalRow(total: Long, averageAmount: Long, currencyCode: String) {
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

private data class CategoryMonthTotal(
    val month: LocalDate,
    val total: Long
)

private fun recentMonths(selectedMonth: LocalDate, count: Int): List<LocalDate> {
    return List(count) { index -> selectedMonth.minus(count - 1 - index, DateTimeUnit.MONTH) }
}

private fun activeCategoryMonthlyAverage(monthTotals: List<CategoryMonthTotal>): Long {
    val activeMonths = monthTotals.filter { it.total > 0L }
    return activeMonths.sumOf { it.total } / activeMonths.size.coerceAtLeast(1)
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
