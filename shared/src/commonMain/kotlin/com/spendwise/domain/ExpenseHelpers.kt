package com.spendwise.domain

import doist.x.normalize.Form
import doist.x.normalize.normalize

fun String.removeAccents(): String {
    val normalized = this.normalize(Form.NFD)

    return normalized.filter { char ->
        char.code !in 0x0300..0x036F
    }.replace('đ', 'd')
        .replace('Đ', 'D')
}


fun List<Expense>.filterByTransactionFilters(filters: TransactionFilters): List<Expense> {
    val normalizedQuery = filters.query.removeAccents()

    return with(ReportCalculator) { filterByTags(filters.selectedTags) }
        .filter { filters.categoryId == null || it.categoryId == filters.categoryId }
        .filter { normalizedQuery.isBlank() || it.note.removeAccents().contains(normalizedQuery, ignoreCase = true) }
}

fun List<Expense>.filterByTags(selectedTags: Set<String>): List<Expense> {
    if (selectedTags.isEmpty()) return this
    val normalized = selectedTags.map(TagParser::normalize).filter { it.isNotBlank() }.toSet()
    if (normalized.isEmpty()) return this
    return filter { expense -> expense.tags.map(TagParser::normalize).any { it in normalized } }
}
