package com.spendwise.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendwise.ui.ReportUiState
import com.spendwise.ui.reports.ReportViewModel

@Composable
internal fun TagFilterBar(state: ReportUiState, viewModel: ReportViewModel) {
    if (state.tagUsage.isEmpty()) {
        Text("No tags yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.tagUsage.forEach { usage ->
            FilterChip(
                selected = usage.name in state.selectedTags,
                onClick = { viewModel.toggleTagFilter(usage.name) },
                label = { Text("#${usage.name}") },
                contentPadding = PaddingValues(0.dp)
            )
        }
        if (state.selectedTags.isNotEmpty()) {
            AssistChip(onClick = viewModel::clearTagFilters, label = { Text("Clear") })
        }
    }
}
