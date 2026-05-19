package com.spendwise.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun TagFilterBar(state: SpendWiseUiState, viewModel: SpendWiseViewModel) {
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
