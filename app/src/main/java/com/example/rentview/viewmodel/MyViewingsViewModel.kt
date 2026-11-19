package com.example.rentview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class MyViewingsViewModel(application: Application) :
    AndroidViewModel(application) {

    private val propertyRepository = PropertyRepository()
    private val repository = ViewingRepository(propertyRepository)

    private val _state =
        MutableStateFlow<MyViewingsState>(MyViewingsState.Loading)

    val state: StateFlow<MyViewingsState> = _state

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Load bookings belonging to the authenticated Supabase user.
    fun load(tenantId: Long) {
        viewModelScope.launch {
            if (_state.value !is MyViewingsState.Success) {
                _state.value = MyViewingsState.Loading
            }

            try {
                val items = repository.getCloudViewings()

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

    // Cancel a Supabase viewing and its scheduled notification.
    fun cancel(viewing: ViewingItem, tenantId: Long) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val cloudId = viewing.supabaseId

                if (cloudId.isNullOrBlank()) {
                    _message.value =
                        "This viewing has no valid cloud reference."
                    return@launch
                }

                val cancelled = repository.cancel(cloudId, tenantId)

                if (cancelled) {
                    ViewingReminders.cancel(context, cloudId)
                }

                _message.value = if (cancelled) {
                    "Viewing at ${viewing.propertyTitle} cancelled."
                } else {
                    "That viewing could not be cancelled."
                }

            } catch (e: Exception) {
                _message.value =
                    "Could not cancel the viewing. Please try again."
            }

            load(tenantId)
        }
    }

    fun messageShown() {
        _message.value = null
    }
}