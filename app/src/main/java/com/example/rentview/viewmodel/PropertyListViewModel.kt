package com.example.rentview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentview.UserDatabaseHelper
import com.example.rentview.data.PropertyItem
import com.example.rentview.data.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// What the property list screen can be showing.
sealed class PropertyListState {
    object Loading : PropertyListState()
    object Empty : PropertyListState()
    data class Success(val items: List<PropertyItem>) : PropertyListState()
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

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = PropertyListState.Loading
            try {
                val items = repository.getApprovedProperties()
                _state.value = if (items.isEmpty()) {
                    PropertyListState.Empty
                } else {
                    PropertyListState.Success(items)
                }
            } catch (e: Exception) {
                _state.value = PropertyListState.Error(
                    "Could not load properties. Please try again."
                )
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
