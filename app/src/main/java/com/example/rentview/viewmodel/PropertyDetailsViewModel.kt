// ============================================================
// FILE: PropertyDetailsViewModel.kt
// ============================================================

package com.example.rentview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentview.data.PropertyItem
import com.example.rentview.data.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PropertyDetailsState(
    val isLoading: Boolean = true,
    val property: PropertyItem? = null,
    val photoUris: List<String> = emptyList(),
    val error: String? = null
)

class PropertyDetailsViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository = PropertyRepository()

    private val _state = MutableStateFlow(PropertyDetailsState())
    val state: StateFlow<PropertyDetailsState> = _state

    private var loadedPropertyId: String? = null

    // Load a property using its Supabase UUID.
    fun load(propertyId: String) {
        if (propertyId.isBlank()) {
            _state.value = PropertyDetailsState(
                isLoading = false,
                error = "Invalid property reference."
            )
            return
        }

        if (propertyId == loadedPropertyId) return

        loadedPropertyId = propertyId

        viewModelScope.launch {
            _state.value = PropertyDetailsState(isLoading = true)

            try {
                val property = repository.getProperty(propertyId)

                _state.value = if (property == null || !property.isApproved) {
                    PropertyDetailsState(
                        isLoading = false,
                        error = "This property is no longer available."
                    )
                } else {
                    PropertyDetailsState(
                        isLoading = false,
                        property = property,
                        photoUris = property.photoUris
                    )
                }

            } catch (e: Exception) {
                loadedPropertyId = null

                _state.value = PropertyDetailsState(
                    isLoading = false,
                    error = e.message
                        ?: "Could not load this property. Please try again."
                )
            }
        }
    }
}
