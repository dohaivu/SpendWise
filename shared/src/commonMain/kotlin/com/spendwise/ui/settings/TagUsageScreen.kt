package com.spendwise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.ui.SpendWiseUiState
import com.spendwise.ui.SpendWiseViewModel
import com.spendwise.ui.TagUsageSort
import com.spendwise.ui.formatMoney

@Composable
internal fun TagUsageScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Text(
                    "Tag usage",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TagUsageSort.entries.forEach { sort ->
                    FilterChip(
                        selected = state.tagUsageSort == sort,
                        onClick = { viewModel.setTagUsageSort(sort) },
                        label = { Text(sort.label()) }
                    )
                }
            }
        }
        items(viewModel.getSortedTagUsage()) { usage ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${usage.name}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${usage.expenseCount} uses")
                        Spacer(Modifier.width(14.dp))
                        Text(formatMoney(usage.totalBaseAmountCents, state.baseCurrencyCode))
                    }
                    Text(
                        "This month ${formatMoney(usage.currentMonthAmountCents, state.baseCurrencyCode)} • Previous ${formatMoney(usage.previousMonthAmountCents, state.baseCurrencyCode)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

private fun TagUsageSort.label(): String = when (this) {
    TagUsageSort.MostUsed -> "Most used"
    TagUsageSort.HighestSpending -> "Highest spending"
    TagUsageSort.RecentlyUsed -> "Recently used"
    TagUsageSort.Alphabetical -> "A-Z"
}
