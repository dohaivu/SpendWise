package com.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

enum class ReportPeriod(val label: String) {
    Month("Monthly"),
    Annual("Annual")
}

@Composable
internal fun ReportPeriodSwitcher(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPeriod.entries.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label) }
            )
        }
    }
}

@Composable
internal fun MonthHeader(
    month: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    PeriodHeader(
        title = monthTitle(month),
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
        previousContentDescription = "Previous month",
        nextContentDescription = "Next month"
    )
}

@Composable
internal fun YearHeader(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit
) {
    PeriodHeader(
        title = "$year",
        subtitle = "Jan 01 - Dec 31",
        onPrevious = onPreviousYear,
        onNext = onNextYear,
        previousContentDescription = "Previous year",
        nextContentDescription = "Next year"
    )
}

@Composable
private fun PeriodHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousContentDescription: String,
    nextContentDescription: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = previousContentDescription)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(
                    text = "($subtitle)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.padding(start = 14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = nextContentDescription)
        }
    }
}
