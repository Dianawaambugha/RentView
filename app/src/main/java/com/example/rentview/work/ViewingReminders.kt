package com.example.rentview.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.rentview.data.ViewingTime
import java.util.concurrent.TimeUnit

// Schedules and cancels the "your viewing is in one hour" reminders.
//
// Why WorkManager: the reminder must still fire if the app is closed or the
// phone restarts, and it does not need to run at an exact second. WorkManager
// is built for this kind of deferrable, guaranteed background work.
object ViewingReminders {

    private const val REMIND_BEFORE_MILLIS = 60 * 60 * 1000L   // one hour

    private fun workName(viewingId: Long) = "viewing_reminder_$viewingId"

    // Schedule (or replace) the reminder for a viewing.
    // If the viewing is less than an hour away, the reminder fires right away.
    fun schedule(
        context: Context,
        viewingId: Long,
        propertyTitle: String,
        date: String,
        slot: String
    ) {
        val viewingTime = ViewingTime.toMillis(date, slot) ?: return
        val now = System.currentTimeMillis()

        if (viewingTime <= now) {
            return
        }

        val delay = (viewingTime - REMIND_BEFORE_MILLIS - now).coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<ViewingReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ViewingReminderWorker.KEY_TITLE to propertyTitle,
                    ViewingReminderWorker.KEY_DATE to date,
                    ViewingReminderWorker.KEY_SLOT to slot
                )
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                workName(viewingId),
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    // Cancel the reminder when a viewing is cancelled.
    fun cancel(context: Context, viewingId: Long) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(workName(viewingId))
    }
}
