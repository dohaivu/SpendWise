package com.spendwise.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.spendwise.domain.TagParser
import com.spendwise.ui.ExpenseUiState
import com.spendwise.ui.components.CategoryLabel
import com.spendwise.ui.components.CurrencyMenu
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.components.formatDate
import com.spendwise.ui.today
import com.spendwise.ui.toLocalDate
import com.spendwise.ui.toUtcLocalDate
import com.spendwise.ui.toUtcStartMillis
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@Composable
internal fun ExpenseScreen(
    state: ExpenseUiState,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    var noteField by remember { mutableStateOf(TextFieldValue(state.draft.note, TextRange(state.draft.note.length))) }
    LaunchedEffect(state.draft.note) {
        if (noteField.text != state.draft.note) {
            noteField = TextFieldValue(state.draft.note, TextRange(state.draft.note.length))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(
                        if (state.draft.editingExpenseId == null) "Expense" else "Edit expense",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                val currencyFormat = currencyDisplayFormat(state.draft.currencyCode)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.draft.amountText,
                        onValueChange = viewModel::updateAmount,
                        label = { Text("Amount") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (currencyFormat.fractionDigits > 0) {
                                KeyboardType.Decimal
                            } else {
                                KeyboardType.Number
                            }
                        )
                    )
                    CurrencyMenu(
                        selected = state.draft.currencyCode,
                        onSelected = viewModel::updateCurrency,
                        modifier = Modifier.width(126.dp)
                    )
                }
            }
            if (state.draft.currencyCode != state.baseCurrencyCode.code) {
                item {
                    OutlinedTextField(
                        value = state.draft.exchangeRateText,
                        onValueChange = viewModel::updateExchangeRate,
                        label = { Text("Rate to ${state.baseCurrencyCode.code}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            item {
                var showDatePicker by remember { mutableStateOf(false) }
                val selectedDate = state.draft.spentAtMillis.toLocalDate()
                val today = today()
                val yesterday = today.minus(1, DateTimeUnit.DAY)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateAssistChip(
                        selected = selectedDate == today,
                        onClick = viewModel::selectTodayForDraft,
                        label = "Today"
                    )
                    DateAssistChip(
                        selected = selectedDate == yesterday,
                        onClick = viewModel::selectYesterdayForDraft,
                        label = "Yesterday"
                    )
                    DateAssistChip(
                        selected = selectedDate != today && selectedDate != yesterday,
                        onClick = { showDatePicker = true },
                        label = formatDate(state.draft.spentAtMillis)
                    )
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
                val tagChips = state.tagSuggestions.ifEmpty {
                    state.tagUsage
                        .sortedByDescending { it.lastUsedAtMillis }
                        .map { it.name }
                }
                OutlinedTextField(
                    value = noteField,
                    onValueChange = { value ->
                        noteField = value
                        viewModel.updateNote(value.text, value.selection.end)
                    },
                    label = { Text("Note with #tags") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3
                )
                FlowRow(
                    modifier = Modifier.heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tagChips.forEach { tag ->
                        AssistChip(
                            onClick = {
                                val note = state.activeTagToken?.let { token ->
                                    TagParser.replaceActiveToken(noteField.text, token, tag)
                                } ?: appendTagToNote(noteField.text, tag)
                                noteField = TextFieldValue(note, TextRange(note.length))
                                if (state.activeTagToken != null) {
                                    viewModel.selectTagSuggestion(tag)
                                } else {
                                    viewModel.updateNote(note)
                                }
                            },
                            label = { Text("#$tag") },
                            contentPadding = PaddingValues(0.dp),
                        )
                    }
                }
            }

            item {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.draft.categoryId == category.id,
                            onClick = { viewModel.updateCategory(category.id) },
                            label = { CategoryLabel(category) },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::saveExpense, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (state.draft.editingExpenseId == null) "Save expense" else "Update expense",
                        style = MaterialTheme.typography.titleMedium
                    )
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
}

private fun appendTagToNote(note: String, tag: String): String {
    val separator = if (note.isBlank() || note.endsWith(" ") || note.endsWith("\n")) "" else " "
    return "$note$separator#$tag "
}

@Composable
private fun DateAssistChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            labelColor = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    )
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
