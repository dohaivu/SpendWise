package com.spendwise.ui.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.spendwise.ui.SettingsUiState
import com.spendwise.ui.TagUsageSort
import com.spendwise.ui.components.MoneyText
import kotlin.math.roundToInt

@Composable
internal fun TagUsage(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val tagUsage = viewModel.getSortedTagUsage()
    var optionsTag by remember { mutableStateOf<String?>(null) }
    var renamingTag by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deletingTag by remember { mutableStateOf<String?>(null) }
    var menuContainerOffset by remember { mutableStateOf(Offset.Zero) }
    var menuAnchorOffset by remember { mutableStateOf(IntOffset.Zero) }

    SettingsScaffold(
        title = "Tags",
        modifier = modifier,
        navigationIcon = { SettingsBackButton(onBack) }
    ) { contentModifier ->
        Box(
            modifier = contentModifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    menuContainerOffset = coordinates.positionInRoot()
                }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
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
                    var rowOffset by remember { mutableStateOf(Offset.Zero) }
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    rowOffset = coordinates.positionInRoot() - menuContainerOffset
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { offset ->
                                            optionsTag = usage.name
                                            menuAnchorOffset = IntOffset(
                                                x = (rowOffset.x + offset.x).roundToInt(),
                                                y = (rowOffset.y + offset.y).roundToInt()
                                            )
                                        }
                                    )
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${usage.name}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${usage.expenseCount} uses", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(14.dp))
                            MoneyText(usage.totalBaseAmountCents, state.baseCurrency.code)
                        }
                        if (index < tagUsage.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(1.dp)
                    .offset { menuAnchorOffset }
            ) {
                DropdownMenu(
                    expanded = optionsTag != null,
                    onDismissRequest = { optionsTag = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            optionsTag?.let { tag ->
                                renamingTag = tag
                                renameText = tag
                            }
                            optionsTag = null
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            optionsTag?.let { tag -> deletingTag = tag }
                            optionsTag = null
                        }
                    )
                }
            }
        }
        renamingTag?.let { tag ->
            RenameTagDialog(
                tag = tag,
                value = renameText,
                onValueChange = { renameText = it },
                onDismiss = { renamingTag = null },
                onRename = {
                    viewModel.renameTag(tag, renameText)
                    renamingTag = null
                }
            )
        }
        deletingTag?.let { tag ->
            DeleteTagDialog(
                tag = tag,
                onDismiss = { deletingTag = null },
                onDelete = {
                    viewModel.deleteTag(tag)
                    deletingTag = null
                }
            )
        }
    }
}

@Composable
private fun RenameTagDialog(
    tag: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename #$tag") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Tag") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onRename,
                enabled = value.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteTagDialog(
    tag: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete #$tag") },
        text = { Text("Remove this tag from matching transactions?") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun TagUsageSort.label(): String = when (this) {
    TagUsageSort.MostUsed -> "Most used"
    TagUsageSort.HighestSpending -> "Highest spending"
    TagUsageSort.RecentlyUsed -> "Recently used"
    TagUsageSort.Alphabetical -> "A-Z"
}
