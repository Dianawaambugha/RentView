package com.example.rentview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentview.UserDatabaseHelper
import com.example.rentview.data.PropertyFilter
import com.example.rentview.data.PropertyItem
import com.example.rentview.data.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// What the property list screen can be showing.
sealed class PropertyListState {
    object Loading : PropertyListState()

    // There are no approved properties at all.
    object Empty : PropertyListState()

    // There are properties, but none pass the search and filters.
    object NoMatches : PropertyListState()

    // items = the properties that pass the filters, total = all approved properties.
    data class Success(
        val items: List<PropertyItem>,
        val total: Int = items.size
    ) : PropertyListState()

    data class Error(val message: String) : PropertyListState()
}

// ViewModel: holds the screen's state and survives screen rotation.
// It never touches views; the Activity just displays whatever state it emits.
class PropertyListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PropertyRepository(
        UserDatabaseHelper.getInstance(application)
    )

    private val _state = MutableStateFlow<PropertyListState>(PropertyListState.Loading)
    val state: StateFlow<PropertyListState> = _state

    // Every approved property, loaded once. Searching and filtering work on this
    // list in memory, so typing in the search box never touches the database.
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
                    "Could not load properties. Please try again."
                )
            }
        }
    }

    // Called whenever the search text or a filter field changes.
    fun updateFilter(newFilter: PropertyFilter) {
        if (newFilter == filter) return
        filter = newFilter

        // While loading (or after an error) there is nothing to filter yet.
        // load() applies the latest filter when it finishes.
        val current = _state.value
        if (current !is PropertyListState.Loading && current !is PropertyListState.Error) {
            showFiltered()
        }
    }

    private fun showFiltered() {
        _state.value = if (allProperties.isEmpty()) {
            PropertyListState.Empty
        } else {
            val matches = allProperties.filter { filter.matches(it) }
            if (matches.isEmpty()) {
                PropertyListState.NoMatches
            } else {
                PropertyListState.Success(matches, allProperties.size)
            }
        }
    }

    // Testing only: adds sample approved properties, then reloads the list.
    fun loadSampleData() {
        viewModelScope.launch {
            try {
                repository.seedSampleProperties()
            } catch (e: Exception) {
                // The reload below will show the empty or error state.
            }
            load()
        }
    }
}
