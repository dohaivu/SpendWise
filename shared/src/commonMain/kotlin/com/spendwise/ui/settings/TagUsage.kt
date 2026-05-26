package com.spendwise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.ui.SettingsUiState
import com.spendwise.ui.TagUsageSort
import com.spendwise.ui.components.MoneyText

@Composable
internal fun TagUsage(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val tagUsage = viewModel.getSortedTagUsage()

    SettingsScaffold(
        title = "Tag usage",
        modifier = modifier,
        navigationIcon = { SettingsBackButton(onBack) }
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier.fillMaxSize().padding(horizontal = 12.dp)
        ) {
            item {
                FlowRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TagUsageSort.entries.forEach { sort ->
                        FilterChip(
                            selected = state.tagUsageSort == sort,
                            onClick = { viewModel.setTagUsageSort(sort) },
                            label = { Text(sort.label()) },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
            }
            itemsIndexed(tagUsage) { index, usage ->
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${usage.name}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${usage.expenseCount} uses", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(14.dp))
                        MoneyText(usage.totalBaseAmountCents, state.baseCurrencyCode.code)
                    }
                    if (index < tagUsage.lastIndex) {
                        HorizontalDivider()
                    }
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
