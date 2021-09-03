package com.kz.flowstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kz.flowstore.UiStateStore.Companion.asStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val store = UiState().asStore()

    val flow: StateFlow<UiState> get() = store.flow

    fun reverseText() {
        viewModelScope.launch {
            store.text(String::reversed)
        }
    }

    fun incCount() {
        viewModelScope.launch {
            store.count(Int::inc)
        }
    }

    fun decCount() {
        viewModelScope.launch {
            store.count(Int::dec)
        }
    }
}
