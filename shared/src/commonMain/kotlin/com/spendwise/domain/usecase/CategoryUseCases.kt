package com.spendwise.domain.usecase

import com.spendwise.data.ExpenseRepository
import com.spendwise.domain.CategoryDraft

class SaveCategoryUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(draft: CategoryDraft): Long =
        repository.saveCategory(draft)
}

class ArchiveCategoryUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.archiveCategory(id)
    }
}

class MoveCategoryUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(id: Long, direction: Int) {
        repository.moveCategory(id, direction)
    }
}

