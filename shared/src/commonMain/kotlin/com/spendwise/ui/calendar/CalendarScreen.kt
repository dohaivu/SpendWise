package com.spendwise.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.CalendarUiState
import com.spendwise.ui.components.CategoryIcon
import com.spendwise.ui.components.MoneyText
import com.spendwise.ui.components.MonthHeader
import com.spendwise.ui.components.ReportPeriod
import com.spendwise.ui.components.ReportPeriodSwitcher
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.TransactionFiltersMenu
import com.spendwise.ui.components.TransactionsByDateList
import com.spendwise.ui.components.YearHeader
import com.spendwise.ui.components.formatMoney
import com.spendwise.ui.components.formatMoneyValue
import com.spendwise.ui.firstDayOfMonth
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(
    state: CalendarUiState,
    calendarViewModel: CalendarViewModel,
    onExpenseClick: (Expense) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    onAllTransactionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryById = remember(state.categories) {
        state.categories.associateBy { it.id }
    }
    val transactionListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            var showOverflowMenu by remember { mutableStateOf(false) }
            TinyTopAppBar(
                title = {
                    Text("Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    TransactionFiltersMenu(
                        categories = state.categories,
                        tagUsage = state.tagUsage,
                        filters = state.transactionFilters,
                        onTagClick = calendarViewModel::toggleTagFilter,
                        onQueryChange = calendarViewModel::updateTransactionQuery,
                        onCategoryChange = calendarViewModel::updateTransactionCategory
                    )
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Transactions") },
                                onClick = {
                                    showOverflowMenu = false
                                    onAllTransactionsClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Dataset, contentDescription = "All Transactions")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 0.dp)
                .padding(top = padding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReportPeriodSwitcher(
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = calendarViewModel::selectPeriod
            )
            when (state.selectedPeriod) {
                ReportPeriod.Month -> {
                    MonthHeader(
                        month = state.selectedMonth,
                        onPreviousMonth = calendarViewModel::previousMonth,
                        onNextMonth = calendarViewModel::nextMonth,
                        onCurrentMonth = calendarViewModel::resetToToday
                    )
                    MonthCalendar(
                        month = state.selectedMonth,
                        selectedDate = state.selectedDate,
                        totalsByDate = state.calendarData.totalsByDate,
                        currencyCode = state.baseCurrencyCode,
                        onDateSelected = { date ->
                            calendarViewModel.selectDate(date)
                            state.calendarData.headerIndexes[date]?.let { index ->
                                coroutineScope.launch {
                                    transactionListState.scrollToItem(index)
                                }
                            }
                        },
                        onDateDoubleClick = onDateDoubleClick
                    )
                }
                ReportPeriod.Annual -> {
                    YearHeader(
                        year = state.selectedMonth.year,
                        onPreviousYear = calendarViewModel::previousYear,
                        onNextYear = calendarViewModel::nextYear,
                        onCurrentYear = calendarViewModel::resetToToday
                    )
                    YearCalendar(
                        year = state.selectedMonth.year,
                        selectedMonth = state.selectedMonth,
                        totalsByDate = state.calendarData.totalsByDate,
                        currencyCode = state.baseCurrencyCode,
                        onMonthSelected = { month ->
                            calendarViewModel.selectMonth(month)
                            state.calendarData.headerIndexes.firstHeaderIndexForMonth(month)?.let { index ->
                                coroutineScope.launch {
                                    transactionListState.scrollToItem(index)
                                }
                            }
                        }
                    )
                }
            }
            TotalRow(
                total = state.calendarData.filteredMonthTotal,
                transactionCount = state.calendarData.monthTransactionCount,
                currencyCode = state.baseCurrencyCode
            )
            TransactionsByDateList(
                transactionItems = state.calendarData.transactionItems,
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
private fun MonthCalendar(
    month: LocalDate,
    selectedDate: LocalDate,
    totalsByDate: Map<LocalDate, DailyExpenseTotal>,
    currencyCode: String,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) {
        CalendarCellColors(
            outline = colorScheme.outline.copy(alpha = 0.22f),
            selectedBackground = colorScheme.primaryContainer.copy(alpha = 0.45f),
            defaultBackground = colorScheme.surface,
            headerBackground = colorScheme.surfaceVariant,
            disabledDay = colorScheme.onSurface.copy(alpha = 0.32f),
            saturday = Color(0xFF249AC8),
            sunday = colorScheme.error,
            day = colorScheme.onSurface,
            headerDay = colorScheme.onSurfaceVariant
        )
    }
    val dates = remember(month) { calendarGridDates(month) }
    val weeks = remember(dates) { dates.chunked(7) }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            calendarWeekDays.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.shortName(),
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.headerBackground)
                        .border(0.5.dp, colors.outline)
                        .padding(vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = dayOfWeek.headerColor(colors),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        weeks.forEach { week ->
            CalendarWeekRow(
                week = week,
                month = month,
                selectedDate = selectedDate,
                totalsByDate = totalsByDate,
                currencyCode = currencyCode,
                colors = colors,
                onDateSelected = onDateSelected,
                onDateDoubleClick = onDateDoubleClick
            )
        }
    }
}

@Composable
private fun YearCalendar(
    year: Int,
    selectedMonth: LocalDate,
    totalsByDate: Map<LocalDate, DailyExpenseTotal>,
    currencyCode: String,
    onMonthSelected: (LocalDate) -> Unit
) {
    val monthTotals = remember(totalsByDate, year) {
        totalsByDate.values
            .filter { it.date.year == year }
            .groupBy { it.date.month.number }
            .mapValues { (_, totals) ->
                YearMonthTotal(
                    totalBaseAmountCents = totals.sumOf { it.totalBaseAmountCents },
                    expenseCount = totals.sumOf { it.expenseCount }
                )
            }
    }
    val months = remember(year) { (1..12).map { month -> LocalDate(year, month, 1) } }

    Column(Modifier.fillMaxWidth()) {
        months.chunked(6).forEach { rowMonths ->
            Row(Modifier.fillMaxWidth()) {
                rowMonths.forEach { month ->
                    YearMonthCell(
                        month = month,
                        total = monthTotals[month.month.number],
                        selected = month.isSameMonth(selectedMonth),
                        currencyCode = currencyCode,
                        onClick = { onMonthSelected(month) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun YearMonthCell(
    month: LocalDate,
    total: YearMonthTotal?,
    selected: Boolean,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .height(74.dp)
            .border(0.5.dp, colorScheme.outline.copy(alpha = 0.22f))
            .background(if (selected) colorScheme.primaryContainer.copy(alpha = 0.45f) else colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = month.month.shortName(),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (total != null) {
            Text(
                text = formatMoneyValue(total.totalBaseAmountCents, currencyCode),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(color = colorScheme.error),
                maxLines = 1,
                textAlign = TextAlign.Center,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 6.sp,
                    maxFontSize = 11.sp
                )
            )
            Text(
                text = total.expenseCount.toString(),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CalendarWeekRow(
    week: List<LocalDate>,
    month: LocalDate,
    selectedDate: LocalDate,
    totalsByDate: Map<LocalDate, DailyExpenseTotal>,
    currencyCode: String,
    colors: CalendarCellColors,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        week.forEach { date ->
            CalendarDayCell(
                date = date,
                month = month,
                isSelected = date == selectedDate,
                total = totalsByDate[date],
                currencyCode = currencyCode,
                colors = colors,
                onDateSelected = onDateSelected,
                onDateDoubleClick = onDateDoubleClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    month: LocalDate,
    isSelected: Boolean,
    total: DailyExpenseTotal?,
    currencyCode: String,
    colors: CalendarCellColors,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMonthDate = date.isSameMonth(month)
    val dayColor = when {
        !isMonthDate -> colors.disabledDay
        date.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        date.dayOfWeek == DayOfWeek.SUNDAY -> colors.sunday
        else -> colors.day
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .border(0.5.dp, colors.outline)
            .background(if (isSelected) colors.selectedBackground else colors.defaultBackground)
            .combinedClickable(
                enabled = isMonthDate,
                onClick = { onDateSelected(date) },
                onDoubleClick = { onDateDoubleClick(date) }
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = date.day.toString(),
                modifier = Modifier.padding(top = 4.dp, bottom = 0.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = dayColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
internal fun TotalRow(
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

data class CalendarCellColors(
    val outline: Color,
    val selectedBackground: Color,
    val defaultBackground: Color,
    val headerBackground: Color,
    val disabledDay: Color,
    val saturday: Color,
    val sunday: Color,
    val day: Color,
    val headerDay: Color
)

private val calendarWeekDays: List<DayOfWeek> = listOf(
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

private fun LocalDate.isSameMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month

internal fun LocalDate.compactDateWithDayName(): String =
    "${month.number}.${day} (${dayOfWeek.shortName()})"

internal fun Map<LocalDate, Int>.firstHeaderIndexForMonth(month: LocalDate): Int? =
    entries
        .filter { (date, _) -> date.isSameMonth(month) }
        .maxByOrNull { (date, _) -> date }
        ?.value

internal data class YearMonthTotal(
    val totalBaseAmountCents: Long,
    val expenseCount: Int
)

internal fun kotlinx.datetime.Month.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

internal fun DayOfWeek.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

internal fun DayOfWeek.headerColor(colors: CalendarCellColors): Color = when (this) {
    DayOfWeek.SATURDAY -> colors.saturday
    DayOfWeek.SUNDAY -> colors.sunday
    else -> colors.headerDay
}
