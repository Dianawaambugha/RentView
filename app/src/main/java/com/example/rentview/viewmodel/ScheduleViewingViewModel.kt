package com.example.rentview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentview.data.BookingResult
import com.example.rentview.data.PropertyRepository
import com.example.rentview.data.ViewingRepository
import com.example.rentview.work.ViewingReminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleState(
    val isLoading: Boolean = true,
    val propertyTitle: String = "",
    val propertyLocation: String = "",
    val selectedDate: String? = null,
    val slots: List<String> = emptyList(),
    val currentSlot: String? = null,
    val message: String? = null,
    val saved: BookingResult.Success? = null,
    val fatalError: String? = null
)

class ScheduleViewingViewModel(application: Application) :
    AndroidViewModel(application) {

    private val propertyRepository = PropertyRepository()
    private val viewingRepository =
        ViewingRepository(propertyRepository)

    private val _state = MutableStateFlow(ScheduleState())
    val state: StateFlow<ScheduleState> = _state

    private var started = false

    private var propertyId: String? = null
    private var tenantId: Long = -1L

    // Supabase viewing UUID used when rescheduling.
    private var cloudViewingId: String? = null

    val isRescheduling: Boolean
        get() = cloudViewingId != null

    // Start using a Supabase property UUID.
    fun start(
        propertyId: String,
        tenantId: Long,
        viewingId: String? = null
    ) {
        if (started) return
        started = true

        this.propertyId = propertyId
        this.tenantId = tenantId
        this.cloudViewingId = viewingId?.takeIf { it.isNotBlank() }

        loadScreen()
    }

    private fun loadScreen() {
        val id = propertyId

        if (id.isNullOrBlank()) {
            _state.value = ScheduleState(
                isLoading = false,
                fatalError = "Invalid property reference."
            )
            return
        }

        viewModelScope.launch {
            try {
                val cloudId = cloudViewingId

                if (cloudId != null) {
                    val existing =
                        viewingRepository.getViewing(cloudId, tenantId)

                    if (existing == null) {
                        _state.value = ScheduleState(
                            isLoading = false,
                            fatalError = "This viewing no longer exists."
                        )
                        return@launch
                    }

                    val cloudPropertyId = existing.supabasePropertyId

                    if (cloudPropertyId.isNullOrBlank()) {
                        _state.value = ScheduleState(
                            isLoading = false,
                            fatalError =
                                "The property linked to this viewing could not be found."
                        )
                        return@launch
                    }

                    propertyId = cloudPropertyId

                    _state.value = ScheduleState(
                        isLoading = false,
                        propertyTitle = existing.propertyTitle,
                        propertyLocation = existing.propertyLocation,
                        selectedDate = existing.date,
                        currentSlot = existing.timeSlot,
                        slots = viewingRepository.getAvailableSlots(
                            cloudPropertyId,
                            existing.date,
                            cloudId
                        )
                    )

                    return@launch
                }

                val property = propertyRepository.getProperty(id)

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

            } catch (e: Exception) {
                _state.value = ScheduleState(
                    isLoading = false,
                    fatalError =
                        "Could not load this viewing. Please try again."
                )
            }
        }
    }

    fun onDateSelected(date: String) {
        viewModelScope.launch {
            try {
                val id = propertyId ?: return@launch

                val slots = viewingRepository.getAvailableSlots(
                    id,
                    date,
                    cloudViewingId
                )

                _state.update {
                    it.copy(
                        selectedDate = date,
                        slots = slots,
                        currentSlot = null,
                        message = null
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        message =
                            "Could not check availability. Please try again."
                    )
                }
            }
        }
    }

    fun submit(slot: String?) {
        val date = _state.value.selectedDate

        if (date == null) {
            _state.update {
                it.copy(message = "Please choose a viewing date.")
            }
            return
        }

        if (slot == null) {
            _state.update {
                it.copy(message = "Please choose a time slot.")
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, message = null)
            }

            try {
                val id = propertyId
                    ?: throw IllegalStateException("Property ID is missing.")

                val result = if (cloudViewingId != null) {
                    viewingRepository.reschedule(
                        cloudViewingId!!,
                        tenantId,
                        date,
                        slot
                    )
                } else {
                    viewingRepository.book(
                        id,
                        tenantId,
                        date,
                        slot
                    )
                }

                when (result) {

                    is BookingResult.Success -> {
                        val context = getApplication<Application>()

                        val cloudId = result.supabaseViewingId
                            ?: cloudViewingId

                        if (!cloudId.isNullOrBlank()) {
                            ViewingReminders.schedule(
                                context,
                                cloudId,
                                result.propertyTitle,
                                result.date,
                                result.timeSlot
                            )
                        }

                        _state.update {
                            it.copy(
                                isLoading = false,
                                saved = result
                            )
                        }
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
                                message =
                                    "You already have a viewing at that time. Choose a different slot."
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
                                message =
                                    "This property is no longer available."
                            )
                        }
                    }

                    is BookingResult.Failure -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                message = result.reason
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        message =
                            "Could not save the viewing. Please try again."
                    )
                }
            }
        }
    }

    private suspend fun refreshSlotsWithMessage(
        date: String,
        message: String
    ) {
        val id = propertyId ?: return

        val slots = viewingRepository.getAvailableSlots(
            id,
            date,
            cloudViewingId
        )

        _state.update {
            it.copy(
                isLoading = false,
                slots = slots,
                currentSlot = null,
                message = message
            )
        }
    }

    fun messageShown() {
        _state.update {
            it.copy(message = null)
        }
    }

    fun savedHandled() {
        _state.update {
            it.copy(saved = null)
        }
    }
}