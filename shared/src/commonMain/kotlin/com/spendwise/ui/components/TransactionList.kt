package com.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.calendar.dailyTotalColor
import com.spendwise.ui.localizedCompactDateWithDayName
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.category_fallback
import spendwise.shared.generated.resources.no_transactions

@Composable
internal fun TransactionsByDateList(
    transactionItems: List<DateTransactionListItem>,
    categoryById: Map<Long, Category>,
    currencyFormat: CurrencyDisplayFormat,
    onExpenseClick: (Expense) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (transactionItems.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.no_transactions),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        items(
            items = transactionItems,
            key = { item -> "date-${item.date}" }
        ) { item ->
            DateTransactionListSection(
                item = item,
                categoryById = categoryById,
                currencyFormat = currencyFormat,
                onExpenseClick = onExpenseClick
            )
        }
    }
}

@Composable
private fun DateTransactionListSection(
    item: DateTransactionListItem,
    categoryById: Map<Long, Category>,
    currencyFormat: CurrencyDisplayFormat,
    onExpenseClick: (Expense) -> Unit
) {
    val shape = MaterialTheme.shapes.small

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = shape
            )
    ) {
        TransactionDateHeader(
            date = item.date,
            total = item.total,
            currencyFormat = currencyFormat
        )
        item.expenses.forEach { expense ->
            TransactionRow(
                expense = expense,
                category = categoryById[expense.categoryId],
                currencyFormat = currencyFormat,
                onExpenseClick = onExpenseClick
            )
        }
    }
}

@Composable
private fun TransactionDateHeader(
    date: LocalDate,
    total: Long,
    currencyFormat: CurrencyDisplayFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.localizedCompactDateWithDayName(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatMoney(-total, currencyFormat),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = dailyTotalColor(total, currencyFormat)
        )
    }
}

@Composable
private fun TransactionRow(
    expense: Expense,
    category: Category?,
    currencyFormat: CurrencyDisplayFormat,
    onExpenseClick: (Expense) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseClick(expense) }
            .padding(horizontal = 14.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconKey = category?.icon.orEmpty(),
            tint = category?.let { Color(it.color.toInt()) } ?: colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 14.dp).size(32.dp)
        )
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = category?.name ?: stringResource(Res.string.category_fallback),
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
            currencyFormat = currencyFormat,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
