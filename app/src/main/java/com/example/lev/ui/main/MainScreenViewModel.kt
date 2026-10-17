package com.example.lev.ui.main

import androidx.lifecycle.ViewModel
import com.example.lev.data.DataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface MainScreenUiState {
    data object Loading : MainScreenUiState

    data class Content(val items: List<String>) : MainScreenUiState
}

class MainScreenViewModel(
    repository: DataRepository,
) : ViewModel() {
    val uiState: Flow<MainScreenUiState> =
        flow {
            emit(MainScreenUiState.Loading)
            repository.data.collect { items ->
                emit(MainScreenUiState.Content(items))
            }
        }
}
