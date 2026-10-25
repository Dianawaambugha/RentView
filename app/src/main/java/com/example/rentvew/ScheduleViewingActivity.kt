package com.example.rentvew

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.rentvew.data.ViewingTime
import com.example.rentvew.viewmodel.ScheduleViewingViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

// Tenant screen: pick a date and a free time slot for a property viewing.
// Also used to reschedule an existing viewing (when VIEWING_ID is passed).
class ScheduleViewingActivity : AppCompatActivity() {

    private val viewModel: ScheduleViewingViewModel by viewModels()

    // Android 13+ needs the user's permission before we can show reminders.
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Booking continues whether or not it was granted. */ }

    private var shownSlots: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_viewing)

        val propertyId = intent.getLongExtra("PROPERTY_ID", -1L)
        val userId = intent.getLongExtra("USER_ID", -1L)
        val viewingId = intent.getLongExtra("VIEWING_ID", 0L)

        val heading = findViewById<TextView>(R.id.tvScheduleHeading)
        val progress = findViewById<ProgressBar>(R.id.progressSchedule)
        val errorText = findViewById<TextView>(R.id.tvScheduleError)
        val formLayout = findViewById<LinearLayout>(R.id.layoutSchedule)
        val propertyTitle = findViewById<TextView>(R.id.tvSchedulePropertyTitle)
        val propertyLocation = findViewById<TextView>(R.id.tvSchedulePropertyLocation)
        val dateButton = findViewById<Button>(R.id.btnPickDate)
        val slotHint = findViewById<TextView>(R.id.tvSlotHint)
        val slotSpinner = findViewById<Spinner>(R.id.spinnerSlots)
        val confirmButton = findViewById<Button>(R.id.btnConfirmViewing)
        val cancelButton = findViewById<Button>(R.id.btnScheduleCancel)

        val slotAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            mutableListOf()
        )
        slotSpinner.adapter = slotAdapter

        viewModel.start(propertyId, userId, viewingId)

        if (viewModel.isRescheduling) {
            heading.text = "Reschedule Viewing"
            confirmButton.text = "Save New Time"
        }

        cancelButton.setOnClickListener { finish() }

        dateButton.setOnClickListener { showDatePicker() }

        confirmButton.setOnClickListener {
            requestNotificationPermissionIfNeeded()
            val chosenSlot = if (slotSpinner.visibility == View.VISIBLE) {
                slotSpinner.selectedItem as? String
            } else {
                null
            }
            viewModel.submit(chosenSlot)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    progress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    // The screen cannot be used (not logged in, property gone...).
                    if (state.fatalError != null) {
                        errorText.text = state.fatalError
                        errorText.visibility = View.VISIBLE
                        formLayout.visibility = View.GONE
                        return@collect
                    }

                    errorText.visibility = View.GONE
                    formLayout.visibility = if (state.propertyTitle.isEmpty()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                    propertyTitle.text = state.propertyTitle
                    propertyLocation.text = state.propertyLocation

                    dateButton.text = state.selectedDate
                        ?.let { ViewingTime.displayDate(it) }
                        ?: "Choose a date"

                    // Refill the slot spinner only when the free slots changed.
                    if (state.slots != shownSlots) {
                        shownSlots = state.slots
                        slotAdapter.clear()
                        slotAdapter.addAll(state.slots)

                        val preselect = state.slots.indexOf(state.currentSlot)
                        if (preselect >= 0) {
                            slotSpinner.setSelection(preselect)
                        }
                    }

                    when {
                        state.selectedDate == null -> {
                            slotHint.text = "Pick a date to see the free time slots."
                            slotSpinner.visibility = View.GONE
                        }

                        state.slots.isEmpty() -> {
                            slotHint.text = "No free time slots on this date. " +
                                    "Please choose another date."
                            slotSpinner.visibility = View.GONE
                        }

                        else -> {
                            slotHint.text = "Free time slots:"
                            slotSpinner.visibility = View.VISIBLE
                        }
                    }

                    confirmButton.isEnabled =
                        !state.isLoading && state.slots.isNotEmpty()

                    state.message?.let {
                        Toast.makeText(this@ScheduleViewingActivity, it, Toast.LENGTH_LONG).show()
                        viewModel.messageShown()
                    }

                    state.saved?.let { saved ->
                        viewModel.savedHandled()

                        Toast.makeText(
                            this@ScheduleViewingActivity,
                            "Viewing confirmed: ${ViewingTime.displayDate(saved.date)} at ${saved.timeSlot}",
                            Toast.LENGTH_LONG
                        ).show()

                        // After a new booking, show the tenant their viewings.
                        // After a reschedule we simply go back to that list.
                        if (!viewModel.isRescheduling) {
                            val viewingsIntent = Intent(
                                this@ScheduleViewingActivity,
                                MyViewingsActivity::class.java
                            )
                            viewingsIntent.putExtra("USER_ID", userId)
                            startActivity(viewingsIntent)
                        }
                        finish()
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val today = Calendar.getInstance()

        val picker = DatePickerDialog(
            this,
            { _, year, month, day ->
                viewModel.onDateSelected(ViewingTime.format(year, month, day))
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )

        // Viewings cannot be booked in the past.
        picker.datePicker.minDate = System.currentTimeMillis() - 1000
        picker.show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
