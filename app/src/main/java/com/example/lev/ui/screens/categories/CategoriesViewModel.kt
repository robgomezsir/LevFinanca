package com.example.lev.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.model.Category
import com.example.lev.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
)

class CategoriesViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    val uiState: StateFlow<CategoriesUiState> =
        financeRepository
            .getCategories()
            .map { categories ->
                CategoriesUiState(categories = categories, isLoading = false)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = CategoriesUiState(isLoading = true),
            )

    fun createCategory(
        name: String,
        kind: String,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val category =
                Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    kind = kind,
                    color = if (kind == "INCOME") "#00C853" else "#FF1744",
                    icon = if (kind == "INCOME") "attach_money" else "shopping_cart",
                    createdAt = now,
                    updatedAt = now,
                )
            financeRepository.saveCategory(category)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            financeRepository.deleteCategory(id)
        }
    }
}
