package com.example.rentvew.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentvew.UserDatabaseHelper
import com.example.rentvew.data.PropertyItem
import com.example.rentvew.data.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PropertyDetailsState(
    val isLoading: Boolean = true,
    val property: PropertyItem? = null,
    val photoCount: Int = 0,
    val error: String? = null
)

class PropertyDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PropertyRepository(
        UserDatabaseHelper.getInstance(application)
    )

    private val _state = MutableStateFlow(PropertyDetailsState())
    val state: StateFlow<PropertyDetailsState> = _state

    private var loadedPropertyId: Long = -1L

    fun load(propertyId: Long) {
        if (propertyId == loadedPropertyId) {
            return
        }
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
                        photoCount = repository.getPhotoCount(propertyId)
                    )
                }
            } catch (e: Exception) {
                loadedPropertyId = -1L
                _state.value = PropertyDetailsState(
                    isLoading = false,
                    error = "Could not load this property. Please try again."
                )
            }
        }
    }
}
