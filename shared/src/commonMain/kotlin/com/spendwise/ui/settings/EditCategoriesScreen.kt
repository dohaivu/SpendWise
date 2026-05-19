package com.spendwise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Category
import com.spendwise.domain.CategoryDraft
import com.spendwise.ui.SpendWiseUiState
import com.spendwise.ui.SpendWiseViewModel

private val categoryIcons = listOf(
    "🍜", "🥤", "👕", "💄",
    "🥂", "💊", "📝", "🚰",
    "🚆", "📱", "🏠", "👛",
    "🐷", "🎁", "💰", "🪙",
    "👥", "🎲", "🛒", "🚕"
)

private val categoryColors = listOf(
    0xFF0000D8, 0xFF5E16B5, 0xFFB0B0B0, 0xFF505050, 0xFF2B2B2B,
    0xFFFF8200, 0xFF08B84F, 0xFF243FA8, 0xFFF24BA4, 0xFFFFD20A,
    0xFFFF4751, 0xFF5ED889, 0xFF20C5E5, 0xFFAC6A3C, 0xFF666666,
    0xFFFFA000, 0xFF4FB36C, 0xFFFFB17E, 0xFFFF2A0A, 0xFF16BCE5,
    0xFF49C9BD, 0xFFE97BAE, 0xFFFF7F00, 0xFF263FA8, 0xFFFFD800
)

@Composable
internal fun EditCategoriesScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Category) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        CategoryTopBar(title = "Edit categories", onBack = onBack, action = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        })
        CategoryTypeTabs()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.snapshot.categories.filterNot { it.archived }, key = { it.id }) { category ->
                CategorySortRow(
                    category = category,
                    onClick = { onEdit(category) },
                    onMoveUp = { viewModel.moveCategoryUp(category.id) },
                    onMoveDown = { viewModel.moveCategoryDown(category.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
internal fun CategoryEditorScreen(
    state: SpendWiseUiState,
    viewModel: SpendWiseViewModel,
    modifier: Modifier,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val title = state.categoryDraft.name.ifBlank { "New category" }

    Column(modifier = modifier.fillMaxSize()) {
        CategoryTopBar(title = title, onBack = onBack, action = {
            if (state.categoryDraft.editingCategoryId != null) {
                IconButton(
                    onClick = {
                        viewModel.archiveCategory(state.categoryDraft.editingCategoryId)
                        viewModel.cancelCategoryEdit()
                        onBack()
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Archive category")
                }
            }
        })
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Name", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.categoryDraft.name,
                        onValueChange = viewModel::updateCategoryName,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                HorizontalDivider()
            }
            item {
                Text("Icon", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                IconGrid(state.categoryDraft, viewModel)
            }
            item {
                HorizontalDivider()
                Text("Color", modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), style = MaterialTheme.typography.titleMedium)
                ColorGrid(state.categoryDraft, viewModel)
            }
        }
        TextButton(
            onClick = {
                viewModel.saveCategory()
                onSaved()
            },
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Text("Save", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CategoryTopBar(
    title: String,
    onBack: () -> Unit,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        action()
    }
}

@Composable
private fun CategoryTypeTabs() {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Expense",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Income",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            }
        }
        Row {
            Box(Modifier.weight(1f).height(3.dp).background(MaterialTheme.colorScheme.primary))
            Box(Modifier.weight(1f).height(3.dp).background(Color.Transparent))
        }
        HorizontalDivider()
    }
}

@Composable
private fun CategorySortRow(
    category: Category,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(88.dp).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category.icon,
            modifier = Modifier.width(52.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = Color(category.color.toInt())
        )
        Text(category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = onMoveUp, modifier = Modifier.height(30.dp)) { Text("↑") }
            Text("=", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onMoveDown, modifier = Modifier.height(30.dp)) { Text("↓") }
        }
    }
}

@Composable
private fun IconGrid(categoryDraft: CategoryDraft, viewModel: SpendWiseViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().height(288.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(categoryIcons) { icon ->
            val selected = categoryDraft.icon == icon
            Box(
                modifier = Modifier.height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) Color(categoryDraft.color.toInt()) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { viewModel.updateCategoryIcon(icon) },
                contentAlignment = Alignment.Center
            ) {
                Text(icon, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun ColorGrid(categoryDraft: CategoryDraft, viewModel: SpendWiseViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxWidth().height(270.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(categoryColors) { color ->
            val selected = categoryDraft.color == color
            Box(
                modifier = Modifier.height(54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(color.toInt()))
                    .border(
                        width = if (selected) 4.dp else 0.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { viewModel.updateCategoryColor(color) }
            )
        }
    }
}
