package com.spendwise.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.unit.DpSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.spendwise.domain.Category
import com.spendwise.domain.TagUsage
import com.spendwise.domain.TransactionFilters
import org.jetbrains.compose.resources.stringResource
import spendwise.shared.generated.resources.Res
import spendwise.shared.generated.resources.*

internal data class SliderConfig(
    val maxCents: Long,
    val stepCents: Long
)

internal fun currencyFilterSliderConfig(currencyFormat: CurrencyDisplayFormat): SliderConfig {
    return when (currencyFormat.code.uppercase()) {
        "VND" -> SliderConfig(maxCents = 10_000_000_00L, stepCents = 100_000_00L)
        "JPY" -> SliderConfig(maxCents = 100_000_00L, stepCents = 1_000_00L)
        else -> {
            if (currencyFormat.fractionDigits == 0) {
                SliderConfig(maxCents = 1_000_000_00L, stepCents = 10_000_00L)
            } else {
                SliderConfig(maxCents = 100_000L, stepCents = 1_000L)
            }
        }
    }
}

@Composable
internal fun TransactionFiltersPanel(
    categories: List<Category>,
    tagUsage: List<TagUsage>,
    filters: TransactionFilters,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean = true,
    currencyFormat: CurrencyDisplayFormat = CurrencyDisplayFormat.default(),
    onTagClick: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onMinAmountChange: (Long) -> Unit = {},
    singleLineCategories: Boolean = false,
    showCategories: Boolean = true
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (isCollapsed) 2.dp else 8.dp)
    ) {
        if (isCollapsed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(Res.string.collapse_filters)
                    } else {
                        stringResource(Res.string.expand_filters)
                    },
                    modifier = Modifier.size(16.dp).clickable {
                        expanded = !expanded
                    }
                )
            }

            if (!expanded) return@Column
        }

        if (tagUsage.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .height(32.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tagUsage.forEach { usage ->
                    FilterChip(
                        selected = usage.name in filters.selectedTags,
                        onClick = { onTagClick(usage.name) },
                        label = { Text("#${usage.name}") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = usage.name in filters.selectedTags,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
        AppOutlinedTextField(
            value = filters.query,
            onValueChange = onQueryChange,
            label = stringResource(Res.string.search_note)
        )

        val sliderConfig = remember(currencyFormat) { currencyFilterSliderConfig(currencyFormat) }
        val currentMinCents = filters.minAmountCents.coerceIn(0L, sliderConfig.maxCents)
        val minAmountText = if (currentMinCents <= 0L) {
            stringResource(Res.string.min_amount_any)
        } else {
            "≥ ${formatMoney(currentMinCents, currencyFormat)}"
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(Res.string.min_amount, minAmountText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val interactionSource = remember { MutableInteractionSource() }
            Slider(
                value = currentMinCents.toFloat(),
                onValueChange = { newValue ->
                    val rawCents = newValue.toLong()
                    val step = sliderConfig.stepCents
                    val snappedCents = ((rawCents + step / 2) / step) * step
                    onMinAmountChange(snappedCents.coerceIn(0L, sliderConfig.maxCents))
                },
                valueRange = 0f..sliderConfig.maxCents.toFloat(),
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        thumbSize = DpSize(12.dp, 12.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(28.dp)
            )
        }

        if (!showCategories) {
            return@Column
        }
        val onCategoryClick = { categoryId: Long ->
            onCategoryChange(categoryId.takeUnless { filters.categoryId == categoryId })
        }
        if (singleLineCategories) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = filters.categoryId == category.id,
                        onClick = { onCategoryClick(category.id) },
                        label = { CategoryLabel(category) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filters.categoryId == category.id,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = filters.categoryId == category.id,
                        onClick = { onCategoryClick(category.id) },
                        label = { CategoryLabel(category) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filters.categoryId == category.id,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun TransactionFiltersMenu(
    categories: List<Category>,
    tagUsage: List<TagUsage>,
    filters: TransactionFilters,
    currencyFormat: CurrencyDisplayFormat = CurrencyDisplayFormat.default(),
    onTagClick: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onMinAmountChange: (Long) -> Unit = {},
    showCategories: Boolean = true
) {
    var isPopupOpen by remember { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(false) }
    val hasActiveFilters = filters.query.isNotBlank() ||
        (showCategories && filters.categoryId != null) ||
        filters.selectedTags.isNotEmpty() ||
        filters.minAmountCents > 0L
    val contentDescription = if (hasActiveFilters) {
        stringResource(Res.string.open_transaction_filters_active)
    } else {
        stringResource(Res.string.open_transaction_filters)
    }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = {
                isPopupOpen = true
                visibleState.targetState = true
            }
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = contentDescription,
                    tint = if (hasActiveFilters) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
                if (hasActiveFilters) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                    ) {}
                }
            }
        }

        if (isPopupOpen) {
            if (visibleState.isIdle && !visibleState.targetState) {
                isPopupOpen = false
            }

            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = 0, y = 130),
                onDismissRequest = { visibleState.targetState = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = scaleIn(
                        initialScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(200)
                    ) + fadeIn(),
                    exit = scaleOut(
                        targetScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(150)
                    ) + fadeOut()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .heightIn(min = 100.dp)
                    ) {
                        TransactionFiltersPanel(
                            categories = categories,
                            tagUsage = tagUsage,
                            filters = filters,
                            modifier = Modifier.padding(8.dp),
                            isCollapsed = false,
                            currencyFormat = currencyFormat,
                            onTagClick = onTagClick,
                            onQueryChange = onQueryChange,
                            onCategoryChange = onCategoryChange,
                            onMinAmountChange = onMinAmountChange,
                            singleLineCategories = true,
                            showCategories = showCategories
                        )
                    }
                }
            }
        }
    }
}
