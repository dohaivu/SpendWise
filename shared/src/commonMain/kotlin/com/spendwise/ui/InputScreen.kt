package com.spendwise.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.spendwise.domain.TagParser
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun InputScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    var noteField by remember { mutableStateOf(TextFieldValue(state.draft.note, TextRange(state.draft.note.length))) }
    LaunchedEffect(state.draft.note) {
        if (noteField.text != state.draft.note) {
            noteField = TextFieldValue(state.draft.note, TextRange(state.draft.note.length))
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                if (state.draft.editingExpenseId == null) "Input" else "Edit expense",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
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
            var showDatePicker by remember { mutableStateOf(false) }
            Text("Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::selectTodayForDraft, label = { Text("Today") })
                AssistChip(onClick = viewModel::selectYesterdayForDraft, label = { Text("Yesterday") })
                AssistChip(onClick = { showDatePicker = true }, label = { Text(formatDate(state.draft.spentAtMillis)) })
            }
            if (showDatePicker) {
                DraftDatePickerDialog(
                    selectedDate = state.draft.spentAtMillis.toLocalDate(),
                    onDismiss = { showDatePicker = false },
                    onDateSelected = { date ->
                        viewModel.selectDateForDraft(date)
                        showDatePicker = false
                    }
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
                value = noteField,
                onValueChange = { value ->
                    noteField = value
                    viewModel.updateNote(value.text, value.selection.end)
                },
                label = { Text("Note with #tags") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            if (state.tagSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.tagSuggestions.forEach { tag ->
                        AssistChip(
                            onClick = {
                                state.activeTagToken?.let { token ->
                                    val note = TagParser.replaceActiveToken(noteField.text, token, tag)
                                    noteField = TextFieldValue(note, TextRange(note.length))
                                }
                                viewModel.selectTagSuggestion(tag)
                            },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }
        item {
            Button(onClick = viewModel::saveExpense, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.draft.editingExpenseId == null) "Save expense" else "Update expense")
            }
            if (state.draft.editingExpenseId != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = viewModel::cancelExpenseEdit, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    OutlinedButton(onClick = viewModel::deleteEditingExpense, modifier = Modifier.weight(1f)) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftDatePickerDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toUtcStartMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.toUtcLocalDate()
                        ?.let(onDateSelected)
                        ?: onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = null,
            headline = null,
            showModeToggle = false
        )
    }
}

private fun LocalDate.toUtcStartMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toUtcLocalDate(): LocalDate =
    kotlin.time.Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date
