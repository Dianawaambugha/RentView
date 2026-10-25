package com.example.rentview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentview.UserDatabaseHelper
import com.example.rentview.data.PropertyRepository
import com.example.rentview.data.ViewingItem
import com.example.rentview.data.ViewingRepository
import com.example.rentview.work.ViewingReminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MyViewingsState {
    object Loading : MyViewingsState()
    object Empty : MyViewingsState()
    data class Success(val items: List<ViewingItem>) : MyViewingsState()
    data class Error(val message: String) : MyViewingsState()
}

class MyViewingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = UserDatabaseHelper.getInstance(application)
    private val repository = ViewingRepository(database, PropertyRepository(database))

    private val _state = MutableStateFlow<MyViewingsState>(MyViewingsState.Loading)
    val state: StateFlow<MyViewingsState> = _state

    // One-time message (e.g. "Viewing cancelled") for the Activity to toast.
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun load(tenantId: Long) {
        if (tenantId <= 0L) {
            _state.value = MyViewingsState.Error("Please log in again to see your viewings.")
            return
        }

        viewModelScope.launch {
            // Only show the spinner on the first load, so the list does not
            // flash every time the user comes back to this screen.
            if (_state.value !is MyViewingsState.Success) {
                _state.value = MyViewingsState.Loading
            }

            try {
                val items = repository.getViewings(tenantId)
                _state.value = if (items.isEmpty()) {
                    MyViewingsState.Empty
                } else {
                    MyViewingsState.Success(items)
                }
            } catch (e: Exception) {
                _state.value = MyViewingsState.Error(
                    "Could not load your viewings. Please try again."
                )
            }
        }
    }

    // DELETE: cancel a viewing, stop its reminder and refresh the list.
    fun cancel(viewing: ViewingItem, tenantId: Long) {
        viewModelScope.launch {
            try {
                val deleted = repository.cancel(viewing.id, tenantId)

                if (deleted) {
                    ViewingReminders.cancel(getApplication<Application>(), viewing.id)
                    _message.value = "Viewing at ${viewing.propertyTitle} cancelled."
                } else {
                    _message.value = "That viewing could not be cancelled."
                }
            } catch (e: Exception) {
                _message.value = "Could not cancel the viewing. Please try again."
            }

            load(tenantId)
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
