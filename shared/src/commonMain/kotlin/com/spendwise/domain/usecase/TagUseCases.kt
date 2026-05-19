package com.spendwise.domain.usecase

import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.ActiveTagToken
import com.spendwise.domain.SpendWiseSnapshot
import com.spendwise.domain.TagParser
import com.spendwise.domain.TagUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParseTagsFromNoteUseCase {
    operator fun invoke(note: String): List<String> = TagParser.parse(note)
}

class GetTagAutocompleteSuggestionsUseCase {
    operator fun invoke(token: ActiveTagToken?, snapshot: SpendWiseSnapshot, limit: Int = 5): List<String> {
        if (token == null) return emptyList()
        return snapshot.tagUsage
            .asSequence()
            .filter { it.name.startsWith(token.query, ignoreCase = true) }
            .sortedByDescending { it.expenseCount }
            .map { it.name }
            .take(limit)
            .toList()
    }
}

class GetTagUsageStatsUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<TagUsage>> =
        repository.observeSnapshot().map { it.tagUsage }
}

class GetKnownTagsUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<String>> =
        repository.observeSnapshot().map { snapshot -> snapshot.tagUsage.map { it.name } }
}

