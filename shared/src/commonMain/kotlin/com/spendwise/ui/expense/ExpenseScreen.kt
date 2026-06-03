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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spendwise.domain.TagParser
import com.spendwise.ui.ExpenseUiState
import com.spendwise.ui.components.CategoryLabel
import com.spendwise.ui.components.CurrencyAmountInputVisualTransformation
import com.spendwise.ui.components.CurrencyMenu
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.currencyDisplayFormat
import com.spendwise.ui.components.formatDate
import com.spendwise.ui.toLocalDate
import com.spendwise.ui.toUtcLocalDate
import com.spendwise.ui.toUtcStartMillis
import com.spendwise.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.stringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.amount
import spendwise.shared.generated.resources.cancel
import spendwise.shared.generated.resources.category
import spendwise.shared.generated.resources.delete
import spendwise.shared.generated.resources.edit_expense_title
import spendwise.shared.generated.resources.expense_title
import spendwise.shared.generated.resources.note_with_tags
import spendwise.shared.generated.resources.ok
import spendwise.shared.generated.resources.rate_to
import spendwise.shared.generated.resources.save_expense
import spendwise.shared.generated.resources.today
import spendwise.shared.generated.resources.update_expense
import spendwise.shared.generated.resources.yesterday

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
    val backState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = backState,
        isBackEnabled = state.draft.editingExpenseId != null,
        onBackCompleted = viewModel::cancelExpenseEdit
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(
                        if (state.draft.editingExpenseId == null) {
                            stringResource(Res.string.expense_title)
                        } else {
                            stringResource(Res.string.edit_expense_title)
                        },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val currencyFormat = currencyDisplayFormat(state.draft.currencyCode)
                val amountVisualTransformation = remember(currencyFormat) {
                    CurrencyAmountInputVisualTransformation(currencyFormat)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.draft.amountText,
                        onValueChange = viewModel::updateAmount,
                        label = { Text(stringResource(Res.string.amount)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = spendWiseTextFieldColors(),
                        visualTransformation = amountVisualTransformation,
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
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
            if (state.draft.currencyCode != state.baseCurrency.code) {
                item {
                    OutlinedTextField(
                        value = state.draft.exchangeRateText,
                        onValueChange = viewModel::updateExchangeRate,
                        label = { Text(stringResource(Res.string.rate_to, state.baseCurrency.code)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = spendWiseTextFieldColors(),
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
                        label = stringResource(Res.string.today)
                    )
                    DateAssistChip(
                        selected = selectedDate == yesterday,
                        onClick = viewModel::selectYesterdayForDraft,
                        label = stringResource(Res.string.yesterday)
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
                    label = { Text(stringResource(Res.string.note_with_tags)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = spendWiseTextFieldColors(),
                    minLines = 1,
                    maxLines = 3
                )
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp).heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            item {
                Text(stringResource(Res.string.category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.draft.categoryId == category.id,
                            onClick = { viewModel.updateCategory(category.id) },
                            label = { CategoryLabel(category) },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.draft.categoryId == category.id,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::saveExpense,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (state.draft.editingExpenseId == null) {
                            stringResource(Res.string.save_expense)
                        } else {
                            stringResource(Res.string.update_expense)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (state.draft.editingExpenseId != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = viewModel::cancelExpenseEdit, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.cancel))
                        }
                        OutlinedButton(onClick = viewModel::deleteEditingExpense, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.delete))
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
private fun spendWiseTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
private fun DateAssistChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary
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
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
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
