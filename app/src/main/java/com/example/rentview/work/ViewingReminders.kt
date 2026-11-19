package com.example.rentview.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.rentview.data.ViewingTime
import java.util.concurrent.TimeUnit

// Schedules and cancels viewing reminders.
// Supports both local Room IDs and Supabase UUIDs.
object ViewingReminders {

    private const val REMIND_BEFORE_MILLIS = 60 * 60 * 1000L

    private fun workName(viewingId: String) =
        "viewing_reminder_$viewingId"

    fun schedule(
        context: Context,
        viewingId: Long,
        propertyTitle: String,
        date: String,
        slot: String
    ) {
        if (viewingId <= 0L) return

        scheduleInternal(
            context,
            viewingId.toString(),
            propertyTitle,
            date,
            slot
        )
    }

    fun schedule(
        context: Context,
        viewingId: String,
        propertyTitle: String,
        date: String,
        slot: String
    ) {
        if (viewingId.isBlank()) return

        scheduleInternal(
            context,
            viewingId,
            propertyTitle,
            date,
            slot
        )
    }

    private fun scheduleInternal(
        context: Context,
        viewingId: String,
        propertyTitle: String,
        date: String,
        slot: String
    ) {
        val viewingTime = ViewingTime.toMillis(date, slot) ?: return
        val now = System.currentTimeMillis()

        if (viewingTime <= now) return

        val delay = (viewingTime - REMIND_BEFORE_MILLIS - now)
            .coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<ViewingReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ViewingReminderWorker.KEY_VIEWING_ID to viewingId,
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

    fun cancel(context: Context, viewingId: Long) {
        if (viewingId <= 0L) return
        cancelInternal(context, viewingId.toString())
    }

    fun cancel(context: Context, viewingId: String) {
        if (viewingId.isBlank()) return
        cancelInternal(context, viewingId)
    }

    private fun cancelInternal(
        context: Context,
        viewingId: String
    ) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(workName(viewingId))
    }
}