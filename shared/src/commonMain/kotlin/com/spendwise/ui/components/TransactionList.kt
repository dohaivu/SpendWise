package com.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.ui.DateTransactionListItem
import com.spendwise.ui.calendar.compactDateWithDayName
import kotlinx.datetime.LocalDate

@Composable
internal fun TransactionsByDateList(
    transactionItems: List<DateTransactionListItem>,
    categoryById: Map<Long, Category>,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        if (transactionItems.isEmpty()) {
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
        items(
            items = transactionItems,
            key = { item ->
                when (item) {
                    is DateTransactionListItem.Header -> "header-${item.date}"
                    is DateTransactionListItem.Transaction -> item.expense.id
                }
            }
        ) { item ->
            when (item) {
                is DateTransactionListItem.Header -> TransactionDateHeader(
                    date = item.date,
                    total = item.total,
                    currencyCode = currencyCode
                )
                is DateTransactionListItem.Transaction -> TransactionRow(
                    expense = item.expense,
                    category = categoryById[item.expense.categoryId],
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
private fun TransactionRow(
    expense: Expense,
    category: Category?,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit
) {
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