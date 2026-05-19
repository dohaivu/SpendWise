package com.spendwise.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.plusMonths
import com.spendwise.domain.DailyExpenseTotal
import com.spendwise.ui.components.TagFilterBar
import com.spendwise.ui.components.TransactionFiltersPanel
import com.spendwise.ui.components.TransactionRow
import com.spendwise.ui.components.formatAmount
import com.spendwise.ui.components.formatMoney
import com.spendwise.ui.components.monthTitle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.yearMonth

@Composable
internal fun CalendarScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    val dailyTotals = viewModel.getDailyExpenseTotals(timeZone)
    val totalsByDate = dailyTotals.associateBy { it.date }
    val monthTotal = dailyTotals
        .filter { it.date.year == state.selectedMonth.year && it.date.month == state.selectedMonth.month }
        .sumOf { it.totalBaseAmountCents }
    val selectedDayExpenses = viewModel.getTransactionsForSelectedDate(timeZone, ignoreCurrencyFilter = true)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::previousMonth) { Text("Prev") }
                Text(monthTitle(state.selectedMonth), fontWeight = FontWeight.Medium)
                TextButton(onClick = viewModel::nextMonth) { Text("Next") }
            }
            Text(
                "Month total ${formatMoney(monthTotal, state.baseCurrencyCode)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            MonthCalendar(
                month = state.selectedMonth,
                selectedDate = state.selectedDate,
                totalsByDate = totalsByDate,
                onDateSelected = viewModel::selectDate
            )
        }
        item { TagFilterBar(state, viewModel) }
        item {
            TransactionFiltersPanel(
                state = state,
                viewModel = viewModel,
                showCurrencyFilter = false,
                singleLineCategories = true
            )
        }
        item {
            Text("${state.selectedDate}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
        items(selectedDayExpenses) { expense ->
            TransactionRow(expense, state.snapshot.categories, state.baseCurrencyCode, viewModel::editExpense)
        }
    }
}

@Composable
private fun MonthCalendar(
    month: LocalDate,
    selectedDate: LocalDate,
    totalsByDate: Map<LocalDate, DailyExpenseTotal>,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = month.yearMonth
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek().first()
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                daysOfWeek().forEach {
                    Text(
                        it.name.take(3),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalCalendar(
                state = calendarState,
                userScrollEnabled = true,
                dayContent = { day ->
                    val date = day.date
                    val isMonthDate = day.position == DayPosition.MonthDate
                    val total = totalsByDate[date]
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.8f)
                            .background(
                                color = when {
                                    !isMonthDate -> Color.Transparent
                                    date == selectedDate -> MaterialTheme.colorScheme.primaryContainer
                                    total != null -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = isMonthDate) { onDateSelected(date) }
                            .padding(2.dp)
                    ) {
                        if (isMonthDate) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${date.day}", fontWeight = FontWeight.Medium)
                                if (total != null) {
                                    Text(
                                        text = formatAmount(total.totalBaseAmountCents),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        autoSize = TextAutoSize.StepBased(
                                            minFontSize = 6.sp,
                                            maxFontSize = MaterialTheme.typography.labelSmall.fontSize
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
