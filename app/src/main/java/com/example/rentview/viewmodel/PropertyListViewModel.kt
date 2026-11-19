package com.example.rentview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentview.data.PropertyItem
import com.example.rentview.data.PropertyRepository
import com.example.rentview.data.PropertyFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PropertyListState {
    object Loading : PropertyListState()
    object Empty : PropertyListState()
    object NoMatches : PropertyListState()

    data class Success(
        val items: List<PropertyItem>,
        val total: Int
    ) : PropertyListState()

    data class Error(val message: String) : PropertyListState()
}

class PropertyListViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository = PropertyRepository()

    private val _state = MutableStateFlow<PropertyListState>(
        PropertyListState.Loading
    )

    val state: StateFlow<PropertyListState> = _state

    private var allProperties: List<PropertyItem> = emptyList()
    private var filter = PropertyFilter()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = PropertyListState.Loading

            try {
                allProperties = repository.getApprovedProperties()
                showFiltered()

            } catch (e: Exception) {
                _state.value = PropertyListState.Error(
                    e.message ?: "Could not load properties. Please try again."
                )
            }
        }
    }

    fun updateFilter(newFilter: PropertyFilter) {
        if (filter == newFilter) return

        filter = newFilter

        if (_state.value !is PropertyListState.Loading &&
            _state.value !is PropertyListState.Error
        ) {
            showFiltered()
        }
    }

    private fun showFiltered() {
        if (allProperties.isEmpty()) {
            _state.value = PropertyListState.Empty
            return
        }

        val matches = allProperties.filter {
            filter.matches(it)
        }

        _state.value = if (matches.isEmpty()) {
            PropertyListState.NoMatches
        } else {
            PropertyListState.Success(
                items = matches,
                total = allProperties.size
            )
        }
    }

    // Refresh the shared Supabase property listings.
    fun loadSampleData() {
        load()
    }
}