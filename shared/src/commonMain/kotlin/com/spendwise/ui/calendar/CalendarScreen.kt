package com.spendwise.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.domain.Category
import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.domain.Expense
import com.spendwise.ui.CalendarUiState
import com.spendwise.ui.components.CategoryIcon
import com.spendwise.ui.components.MonthHeader
import com.spendwise.ui.components.MoneyText
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.TransactionFiltersPanel
import com.spendwise.ui.components.applyTransactionFilters
import com.spendwise.ui.components.formatMoney
import com.spendwise.ui.components.formatMoneyValue
import com.spendwise.ui.firstDayOfMonth
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(
    state: CalendarUiState,
    calendarViewModel: CalendarViewModel,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    val filterWithoutCurrency = state.transactionFilters.copy(currencyCode = null)
    val monthTransactions = state.expenses
        .filter { it.spentDate(timeZone).isSameMonth(state.selectedMonth) }
        .applyTransactionFilters(filterWithoutCurrency, state.selectedTags)
        .sortedByDescending { it.spentAtMillis }
    val totalsByDate = monthTransactions
        .groupBy { it.spentDate(timeZone) }
        .mapValues { (date, expenses) ->
            DailyExpenseTotal(
                date = date,
                totalBaseAmountCents = expenses.sumOf { it.baseAmountCents },
                expenseCount = expenses.size
            )
        }
    val filteredMonthTotal = monthTransactions.sumOf { it.baseAmountCents }
    val groupedTransactions = monthTransactions.groupBy { it.spentDate(timeZone) }.toList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TinyTopAppBar(
                title = {
                    Text("Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MonthHeader(
                    month = state.selectedMonth,
                    onPreviousMonth = calendarViewModel::previousMonth,
                    onNextMonth = calendarViewModel::nextMonth
                )
                MonthCalendar(
                    month = state.selectedMonth,
                    selectedDate = state.selectedDate,
                    totalsByDate = totalsByDate,
                    currencyCode = state.baseCurrencyCode,
                    onDateSelected = calendarViewModel::selectDate
                )
                TransactionFiltersPanel(
                    state = state,
                    calendarViewModel = calendarViewModel,
                    singleLineCategories = true
                )
                MonthTotalRow(
                    total = filteredMonthTotal,
                    transactionCount = monthTransactions.size,
                    currencyCode = state.baseCurrencyCode
                )
            }
            TransactionsByDateList(
                groupedTransactions = groupedTransactions,
                categories = state.categories,
                currencyCode = state.baseCurrencyCode,
                onExpenseClick = onExpenseClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    month: LocalDate,
    selectedDate: LocalDate,
    totalsByDate: Map<LocalDate, DailyExpenseTotal>,
    currencyCode: String,
    onDateSelected: (LocalDate) -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val weekDays = calendarWeekDays()
    val dates = calendarGridDates(month)

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.shortName(),
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(0.5.dp, outline)
                        .padding(vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = dayOfWeek.weekendColor(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        month = month,
                        selectedDate = selectedDate,
                        total = totalsByDate[date],
                        currencyCode = currencyCode,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    month: LocalDate,
    selectedDate: LocalDate,
    total: DailyExpenseTotal?,
    currencyCode: String,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMonthDate = date.isSameMonth(month)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val selectedColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val dayColor = when {
        !isMonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
        date.dayOfWeek == DayOfWeek.SATURDAY -> Color(0xFF249AC8)
        date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(46.dp)
            .border(0.5.dp, outline)
            .background(if (date == selectedDate) selectedColor else MaterialTheme.colorScheme.surface)
            .clickable(enabled = isMonthDate) { onDateSelected(date) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = date.day.toString(),
                modifier = Modifier.padding(top = 4.dp, bottom = 0.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = dayColor,
                fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal
            )
            if (isMonthDate && total != null) {
                Text(
                    text = formatMoneyValue(total.totalBaseAmountCents, currencyCode),
                    modifier = Modifier.padding(end = 2.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error),
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 6.sp,
                        maxFontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun MonthTotalRow(
    total: Long,
    transactionCount: Int,
    currencyCode: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$transactionCount transactions",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatMoney(-total, currencyCode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TransactionsByDateList(
    groupedTransactions: List<Pair<LocalDate, List<Expense>>>,
    categories: List<Category>,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
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
            item(key = "header-$date") {
                TransactionDateHeader(
                    date = date,
                    total = expenses.sumOf { it.baseAmountCents },
                    currencyCode = currencyCode
                )
            }
            items(expenses, key = { it.id }) { expense ->
                CalendarTransactionRow(
                    expense = expense,
                    categories = categories,
                    currencyCode = currencyCode,
                    onExpenseClick = onExpenseClick
                )
            }
        }
    }
}

@Composable
private fun TransactionDateHeader(
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
            text = date.compactDateWithDayName(),
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
private fun CalendarTransactionRow(
    expense: Expense,
    categories: List<Category>,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit
) {
    val category = categories.firstOrNull { it.id == expense.categoryId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseClick(expense) }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconKey = category?.icon.orEmpty(),
            tint = category?.let { Color(it.color.toInt()) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 14.dp).size(28.dp)
        )
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = category?.name ?: "Category",
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

private fun calendarGridDates(month: LocalDate): List<LocalDate> {
    val first = month.firstDayOfMonth()
    val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    val start = first.minus(daysFromMonday(first.dayOfWeek), DateTimeUnit.DAY)
    val end = last.plus(daysToSunday(last.dayOfWeek), DateTimeUnit.DAY)
    val dates = mutableListOf<LocalDate>()
    var current = start
    while (current <= end) {
        dates += current
        current = current.plus(1, DateTimeUnit.DAY)
    }
    return dates
}

private fun calendarWeekDays(): List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

private fun daysFromMonday(dayOfWeek: DayOfWeek): Int =
    (dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + 7) % 7

private fun daysToSunday(dayOfWeek: DayOfWeek): Int =
    (DayOfWeek.SUNDAY.ordinal - dayOfWeek.ordinal + 7) % 7

private fun Expense.spentDate(timeZone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(spentAtMillis).toLocalDateTime(timeZone).date

private fun LocalDate.isSameMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month

private fun LocalDate.compactDateWithDayName(): String =
    "${month.number}.${day} (${dayOfWeek.shortName()})"

private fun DayOfWeek.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

@Composable
private fun DayOfWeek.weekendColor(): Color = when (this) {
    DayOfWeek.SATURDAY -> Color(0xFF249AC8)
    DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
