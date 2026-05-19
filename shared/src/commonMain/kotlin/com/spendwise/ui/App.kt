package com.spendwise.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.plusMonths
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryReportRow
import com.spendwise.domain.Expense
import com.spendwise.domain.ReportCalculator
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

@Composable
fun SpendWiseApp(
    viewModel: SpendWiseViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar {
                        SpendWiseTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = state.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Icon(tab.icon(), contentDescription = tab.name) },
                                label = { Text(tab.label(state.language)) }
                            )
                        }
                    }
                }
            ) { padding ->
                when (state.selectedTab) {
                    SpendWiseTab.Input -> InputScreen(state, viewModel, Modifier.padding(padding))
                    SpendWiseTab.Calendar -> CalendarScreen(state, viewModel, Modifier.padding(padding))
                    SpendWiseTab.Report -> ReportScreen(state, viewModel, Modifier.padding(padding))
                    SpendWiseTab.Others -> OthersScreen(state, viewModel, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun InputScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Input", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.draft.amountText,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                CurrencyMenu(
                    selected = state.draft.currencyCode,
                    onSelected = viewModel::updateCurrency,
                    modifier = Modifier.width(126.dp)
                )
            }
        }
        if (state.draft.currencyCode != state.baseCurrencyCode) {
            item {
                OutlinedTextField(
                    value = state.draft.exchangeRateText,
                    onValueChange = viewModel::updateExchangeRate,
                    label = { Text("Rate to ${state.baseCurrencyCode}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        item {
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.snapshot.categories.filterNot { it.archived }.forEach { category ->
                    FilterChip(
                        selected = state.draft.categoryId == category.id,
                        onClick = { viewModel.updateCategory(category.id) },
                        label = { Text("${category.icon} ${category.name}") }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.draft.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Note with #tags") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            if (state.tagSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.tagSuggestions.forEach { tag ->
                        AssistChip(onClick = { viewModel.selectTagSuggestion(tag) }, label = { Text("#$tag") })
                    }
                }
            }
        }
        item {
            Button(onClick = viewModel::saveExpense, modifier = Modifier.fillMaxWidth()) {
                Text("Save expense")
            }
        }
        item {
            RecentTransactions(state.snapshot.expenses, state.snapshot.categories, state.baseCurrencyCode)
        }
    }
}

@Composable
private fun CalendarScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    val timeZone = TimeZone.currentSystemDefault()
    val dailyTotals = ReportCalculator.dailyTotals(state.snapshot.expenses, timeZone)
    val totalsByDate = dailyTotals.associateBy { it.date }
    val selectedDayExpenses = state.snapshot.expenses.filter { expense ->
        Instant.fromEpochMilliseconds(expense.spentAtMillis).toLocalDateTime(timeZone).date == state.selectedDate
    }.let { expenses -> with(ReportCalculator) { expenses.filterByTags(state.selectedTags) } }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::previousMonth) { Text("Prev") }
                Text(monthTitle(state.selectedMonth), fontWeight = FontWeight.Medium)
                TextButton(onClick = viewModel::nextMonth) { Text("Next") }
            }
        }
        item {
            MonthCalendar(
                month = state.selectedMonth,
                selectedDate = state.selectedDate,
                totalsByDate = totalsByDate,
                currencyCode = state.baseCurrencyCode,
                onDateSelected = viewModel::selectDate
            )
        }
        item {
            TagFilterBar(state, viewModel)
        }
        item {
            Text("Transactions on ${state.selectedDate}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
        items(selectedDayExpenses) { expense ->
            TransactionRow(expense, state.snapshot.categories, state.baseCurrencyCode)
        }
    }
}

@Composable
private fun ReportScreen(
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
    val rows = ReportCalculator.categoryReport(reportExpenses, state.snapshot.categories, state.selectedTags)
    val comparisonRows = ReportCalculator.monthOverMonth(
        expenses = state.snapshot.expenses,
        categories = state.snapshot.categories,
        selectedMonth = state.selectedMonth,
        selectedTags = state.selectedTags,
        timeZone = timeZone
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::toggleReportPeriod) {
                    Text(if (state.selectedReportPeriod == ReportPeriod.Month) "Month" else "Year")
                }
            }
        }
        item {
            TagFilterBar(state, viewModel)
        }
        item {
            CategoryPie(rows)
        }
        items(rows) { row ->
            CategoryReportRowView(row, state.baseCurrencyCode)
        }
        item {
            Text("Month-over-month", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        items(comparisonRows) { row ->
            MonthComparisonRowView(row.category, row.currentMonthAmountCents, row.previousMonthAmountCents, state.baseCurrencyCode, row.status)
        }
    }
}

@Composable
private fun OthersScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Others", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Text("Base currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                supportedCurrencies.forEach { currency ->
                    FilterChip(
                        selected = state.baseCurrencyCode == currency,
                        onClick = { viewModel.setBaseCurrency(currency) },
                        label = { Text(currency) }
                    )
                }
            }
        }
        item {
            Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = state.language == language,
                        onClick = { viewModel.setLanguage(language) },
                        label = { Text(language.label) }
                    )
                }
            }
        }
        item {
            Text("Tag usage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        items(state.snapshot.tagUsage) { usage ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${usage.name}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${usage.expenseCount} uses")
                    Spacer(Modifier.width(14.dp))
                    Text(formatMoney(usage.totalBaseAmountCents, state.baseCurrencyCode))
                }
            }
        }
    }
}

@Composable
private fun TagFilterBar(state: SpendWiseUiState, viewModel: SpendWiseViewModel) {
    if (state.snapshot.tagUsage.isEmpty()) {
        Text("No tags yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.snapshot.tagUsage.forEach { usage ->
            FilterChip(
                selected = usage.name in state.selectedTags,
                onClick = { viewModel.toggleTagFilter(usage.name) },
                label = { Text("#${usage.name}") }
            )
        }
        if (state.selectedTags.isNotEmpty()) {
            AssistChip(onClick = viewModel::clearTagFilters, label = { Text("Clear") })
        }
    }
}

@Composable
private fun MonthCalendar(
    month: LocalDate,
    selectedDate: LocalDate,
    totalsByDate: Map<LocalDate, com.spendwise.domain.DailyExpenseTotal>,
    currencyCode: String,
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
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                daysOfWeek().forEach {
                    Text(
                        it.name.take(1),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalCalendar(
                state = calendarState,
                userScrollEnabled = false,
                dayContent = { day ->
                    val date = day.date
                    val isMonthDate = day.position == DayPosition.MonthDate
                    val total = totalsByDate[date]
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.82f)
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
                            .padding(6.dp)
                    ) {
                        if (isMonthDate) {
                            Column {
                                Text("${date.day}", fontWeight = FontWeight.Medium)
                                if (total != null) {
                                    Text(
                                        formatCompactMoney(total.totalBaseAmountCents, currencyCode),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
            rows.ifEmpty { emptyList() }.map { row ->
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
                Text(status ?: signedMoney(current - previous, currencyCode))
            }
            ComparisonBar("This", current, max, currencyCode, MaterialTheme.colorScheme.primary)
            ComparisonBar("Prev", previous, max, currencyCode, MaterialTheme.colorScheme.tertiary)
        }
    }
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

@Composable
private fun RecentTransactions(expenses: List<Expense>, categories: List<Category>, currencyCode: String) {
    Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        expenses.take(5).forEach { expense ->
            TransactionRow(expense, categories, currencyCode)
        }
    }
}

@Composable
private fun TransactionRow(expense: Expense, categories: List<Category>, currencyCode: String) {
    val category = categories.firstOrNull { it.id == expense.categoryId }
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(category?.icon ?: "•", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: "Category", fontWeight = FontWeight.SemiBold)
                Text(expense.note.ifBlank { "No note" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expense.tags.isNotEmpty()) {
                    Text(expense.tags.joinToString(" ") { "#$it" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(formatMoney(expense.baseAmountCents, currencyCode), fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyMenu(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Currency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        expanded = false
                        onSelected(currency)
                    }
                )
            }
        }
    }
}

private fun SpendWiseTab.icon() = when (this) {
    SpendWiseTab.Input -> Icons.Default.Add
    SpendWiseTab.Calendar -> Icons.Default.CalendarMonth
    SpendWiseTab.Report -> Icons.Default.BarChart
    SpendWiseTab.Others -> Icons.Default.MoreHoriz
}

private fun SpendWiseTab.label(language: AppLanguage): String = when (language) {
    AppLanguage.English -> name
    AppLanguage.Vietnamese -> when (this) {
        SpendWiseTab.Input -> "Nhập"
        SpendWiseTab.Calendar -> "Lịch"
        SpendWiseTab.Report -> "Báo cáo"
        SpendWiseTab.Others -> "Khác"
    }
    AppLanguage.Chinese -> when (this) {
        SpendWiseTab.Input -> "输入"
        SpendWiseTab.Calendar -> "日历"
        SpendWiseTab.Report -> "报表"
        SpendWiseTab.Others -> "其他"
    }
}

private fun monthTitle(date: LocalDate): String = "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"

private fun formatMoney(cents: Long, currencyCode: String): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = cents.absoluteValue
    return "$sign$currencyCode ${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

private fun formatCompactMoney(cents: Long, currencyCode: String): String {
    val amount = cents / 100
    return when {
        amount >= 1_000_000 -> "$currencyCode ${amount / 1_000_000}m"
        amount >= 1_000 -> "$currencyCode ${amount / 1_000}k"
        else -> "$currencyCode $amount"
    }
}

private fun signedMoney(cents: Long, currencyCode: String): String {
    val prefix = if (cents >= 0) "+" else "-"
    return prefix + formatMoney(cents.absoluteValue, currencyCode)
}
