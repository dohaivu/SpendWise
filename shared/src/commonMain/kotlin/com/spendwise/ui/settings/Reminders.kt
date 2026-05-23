package com.spendwise.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.domain.ExpenseReminder
import com.spendwise.ui.SettingsUiState

@Composable
internal fun Reminders(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = "Reminders",
        modifier = modifier,
        navigationIcon = { SettingsBackButton(onBack) },
        actions = {
            IconButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        }
    ) { contentModifier ->
        if (state.reminders.isEmpty()) {
            EmptyReminders(contentModifier)
        } else {
            LazyColumn(modifier = contentModifier.fillMaxSize()) {
                items(state.reminders.sortedBy { it.minutesSinceMidnight }, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onEnabledChange = { enabled -> viewModel.setReminderEnabled(reminder.id, enabled) },
                        onDelete = { viewModel.deleteReminder(reminder.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showTimePicker) {
        AddReminderDialog(
            onDismiss = { showTimePicker = false },
            onAdd = { hour, minute ->
                viewModel.addReminder(hour, minute)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun EmptyReminders(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("No reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Add a time to be reminded to enter expenses.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: ExpenseReminder,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formatReminderTime(reminder), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                if (reminder.enabled) "Notification enabled" else "Notification paused",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange)
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete reminder")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(is24Hour = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reminder") },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(onClick = { onAdd(timePickerState.hour, timePickerState.minute) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatReminderTime(reminder: ExpenseReminder): String {
    val hour12 = when (val hour = reminder.hour % 12) {
        0 -> 12
        else -> hour
    }
    val suffix = if (reminder.hour < 12) "AM" else "PM"
    return "$hour12:${reminder.minute.toString().padStart(2, '0')} $suffix"
}
