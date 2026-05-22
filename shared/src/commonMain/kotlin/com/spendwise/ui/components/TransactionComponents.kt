package com.spendwise.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category
import com.spendwise.domain.Expense
import com.spendwise.domain.TagUsage
import com.spendwise.domain.TransactionFilters
import com.spendwise.domain.usecase.filterByTransactionFilters

@Composable
internal fun TransactionFiltersPanel(
    categories: List<Category>,
    tagUsage: List<TagUsage>,
    filters: TransactionFilters,
    selectedTags: Set<String>,
    isCollapsed: Boolean = true,
    onTagClick: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    singleLineCategories: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (isCollapsed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse filters" else "Expand filters",
                    modifier = Modifier.size(16.dp).clickable {
                        expanded = !expanded
                    }
                )
            }

            if (!expanded) return@Column
        }

        if (tagUsage.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .height(32.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tagUsage.forEach { usage ->
                    FilterChip(
                        selected = usage.name in selectedTags,
                        onClick = { onTagClick(usage.name) },
                        label = { Text("#${usage.name}") },
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }
        }
        AppOutlinedTextField(
            value = filters.query,
            onValueChange = onQueryChange,
            label = "Search note"
        )
        if (singleLineCategories) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filters.categoryId == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All categories") }
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = filters.categoryId == category.id,
                        onClick = { onCategoryChange(category.id) },
                        label = { CategoryLabel(category) }
                    )
                }
            }
        } else {
            FlowRow(
                modifier = Modifier.height(32.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = filters.categoryId == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All categories") }
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = filters.categoryId == category.id,
                        onClick = { onCategoryChange(category.id) },
                        label = { CategoryLabel(category) }
                    )
                }
            }
        }
    }
}

internal fun List<Expense>.applyTransactionFilters(
    filters: TransactionFilters,
    selectedTags: Set<String>
): List<Expense> {
    return filterByTransactionFilters(filters, selectedTags)
}

@Composable
internal fun RecentTransactions(
    expenses: List<Expense>,
    categories: List<Category>,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit
) {
    Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        expenses.take(5).forEach { expense ->
            TransactionRow(expense, categories, currencyCode, onExpenseClick)
        }
    }
}

@Composable
internal fun TransactionRow(
    expense: Expense,
    categories: List<Category>,
    currencyCode: String,
    onExpenseClick: (Expense) -> Unit
) {
    val category = categories.firstOrNull { it.id == expense.categoryId }
    Card(onClick = { onExpenseClick(expense) }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(
                iconKey = category?.icon.orEmpty(),
                tint = category?.let { Color(it.color.toInt()) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: "Category", fontWeight = FontWeight.SemiBold)
                if (expense.note.isNotBlank()) {
                    Text(expense.note, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (expense.tags.isNotEmpty()) {
                    Text(expense.tags.joinToString(" ") { "#$it" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (expense.originalCurrencyCode != currencyCode) {
                    Text(
                        "Original ${formatMoney(expense.originalAmountCents, expense.originalCurrencyCode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            MoneyText(
                amountCents = expense.baseAmountCents,
                currencyCode = currencyCode,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
