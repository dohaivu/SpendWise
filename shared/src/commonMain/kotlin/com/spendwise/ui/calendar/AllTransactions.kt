package com.spendwise.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.domain.Expense
import com.spendwise.ui.AllTransactionsUiState
import com.spendwise.ui.components.TinyTopAppBar
import com.spendwise.ui.components.TransactionFiltersPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AllTransactions(
    state: AllTransactionsUiState,
    viewModel: AllTransactionsViewModel,
    onBack: () -> Unit,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryById = remember(state.categories) {
        state.categories.associateBy { it.id }
    }
    val transactionListState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TinyTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        "All Transactions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TransactionFiltersPanel(
                categories = state.categories,
                tagUsage = state.tagUsage,
                filters = state.transactionFilters,
                selectedTags = state.selectedTags,
                isCollapsed = false,
                onTagClick = viewModel::toggleTagFilter,
                onQueryChange = viewModel::updateTransactionQuery,
                onCategoryChange = viewModel::updateTransactionCategory,
                singleLineCategories = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TotalRow(
                total = state.transactionData.filteredMonthTotal,
                transactionCount = state.transactionData.monthTransactionCount,
                currencyCode = state.baseCurrencyCode
            )
            TransactionsByDateList(
                transactionItems = state.transactionData.transactionItems,
                categoryById = categoryById,
                currencyCode = state.baseCurrencyCode,
                onExpenseClick = onExpenseClick,
                listState = transactionListState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
