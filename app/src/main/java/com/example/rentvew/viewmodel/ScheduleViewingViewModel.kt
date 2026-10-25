package com.example.rentvew.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentvew.UserDatabaseHelper
import com.example.rentvew.data.BookingResult
import com.example.rentvew.data.PropertyRepository
import com.example.rentvew.data.ViewingRepository
import com.example.rentvew.work.ViewingReminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleState(
    val isLoading: Boolean = true,
    val propertyTitle: String = "",
    val propertyLocation: String = "",
    val selectedDate: String? = null,       // yyyy-MM-dd
    val slots: List<String> = emptyList(),  // free slots for the selected date
    val currentSlot: String? = null,        // pre-selected slot when rescheduling
    val message: String? = null,            // one-time message to show the user
    val saved: BookingResult.Success? = null,
    val fatalError: String? = null          // screen cannot be used
)

// Handles both booking a new viewing and rescheduling an existing one.
// If viewingId > 0 the screen is in "reschedule" mode.
class ScheduleViewingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = UserDatabaseHelper.getInstance(application)
    private val propertyRepository = PropertyRepository(database)
    private val viewingRepository = ViewingRepository(database, propertyRepository)

    private val _state = MutableStateFlow(ScheduleState())
    val state: StateFlow<ScheduleState> = _state

    private var started = false
    private var propertyId = -1L
    private var tenantId = -1L
    private var viewingId = 0L

    val isRescheduling: Boolean
        get() = viewingId > 0L

    // Called once by the Activity. Safe to call again after a rotation.
    fun start(propertyId: Long, tenantId: Long, viewingId: Long) {
        if (started) {
            return
        }
        started = true

        this.propertyId = propertyId
        this.tenantId = tenantId
        this.viewingId = viewingId

        if (tenantId <= 0L) {
            _state.value = ScheduleState(
                isLoading = false,
                fatalError = "Please log in again to book a viewing."
            )
            return
        }

        viewModelScope.launch {
            try {
                if (viewingId > 0L) {
                    val existing = viewingRepository.getViewing(viewingId, tenantId)

                    if (existing == null) {
                        _state.value = ScheduleState(
                            isLoading = false,
                            fatalError = "This viewing no longer exists."
                        )
                        return@launch
                    }

                    this@ScheduleViewingViewModel.propertyId = existing.propertyId

                    _state.value = ScheduleState(
                        isLoading = false,
                        propertyTitle = existing.propertyTitle,
                        propertyLocation = existing.propertyLocation,
                        selectedDate = existing.date,
                        currentSlot = existing.timeSlot,
                        slots = viewingRepository.getAvailableSlots(
                            existing.propertyId, existing.date, viewingId
                        )
                    )
                } else {
                    val property = propertyRepository.getProperty(propertyId)

                    _state.value = if (property == null || !property.isApproved) {
                        ScheduleState(
                            isLoading = false,
                            fatalError = "This property is no longer available."
                        )
                    } else {
                        ScheduleState(
                            isLoading = false,
                            propertyTitle = property.title,
                            propertyLocation = property.location
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = ScheduleState(
                    isLoading = false,
                    fatalError = "Something went wrong. Please try again."
                )
            }
        }
    }

    // The tenant picked a date: work out which slots are still free.
    fun onDateSelected(date: String) {
        viewModelScope.launch {
            try {
                val slots = viewingRepository.getAvailableSlots(propertyId, date, viewingId)
                _state.update {
                    it.copy(selectedDate = date, slots = slots, currentSlot = null)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Could not check availability. Please try again.")
                }
            }
        }
    }

    // The tenant pressed Confirm.
    fun submit(slot: String?) {
        val date = _state.value.selectedDate

        if (date == null) {
            _state.update { it.copy(message = "Please choose a viewing date.") }
            return
        }

        if (slot == null) {
            _state.update { it.copy(message = "Please choose a time slot.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val result = if (viewingId > 0L) {
                    viewingRepository.reschedule(viewingId, tenantId, date, slot)
                } else {
                    viewingRepository.book(propertyId, tenantId, date, slot)
                }

                when (result) {
                    is BookingResult.Success -> {
                        ViewingReminders.schedule(
                            getApplication<Application>(),
                            result.viewingId,
                            result.propertyTitle,
                            result.date,
                            result.timeSlot
                        )
                        _state.update { it.copy(isLoading = false, saved = result) }
                    }

                    BookingResult.SlotTaken -> {
                        refreshSlotsWithMessage(
                            date,
                            "That time slot has just been taken. Please choose another."
                        )
                    }

                    BookingResult.TenantBusy -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                message = "You already have a viewing at that time. Choose a different slot."
                            )
                        }
                    }

                    BookingResult.InPast -> {
                        refreshSlotsWithMessage(
                            date,
                            "That time has already passed. Please choose a later slot."
                        )
                    }

                    BookingResult.PropertyUnavailable -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                message = "This property is no longer available."
                            )
                        }
                    }

                    is BookingResult.Failure -> {
                        _state.update {
                            it.copy(isLoading = false, message = result.reason)
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        message = "Could not save the viewing. Please try again."
                    )
                }
            }
        }
    }

    private suspend fun refreshSlotsWithMessage(date: String, message: String) {
        val slots = viewingRepository.getAvailableSlots(propertyId, date, viewingId)
        _state.update {
            it.copy(isLoading = false, slots = slots, currentSlot = null, message = message)
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    fun savedHandled() {
        _state.update { it.copy(saved = null) }
    }
}
