package com.example.rentvew.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.rentvew.data.ViewingTime

// Runs in the background (started by WorkManager) and shows the reminder
// notification for an upcoming viewing.
class ViewingReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_DATE = "date"
        const val KEY_SLOT = "slot"
        private const val CHANNEL_ID = "viewing_reminders"
    }

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {

        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val date = inputData.getString(KEY_DATE) ?: return Result.failure()
        val slot = inputData.getString(KEY_SLOT) ?: return Result.failure()

        createChannel()

        // On Android 13+ the user must allow notifications. If they have not,
        // there is nothing to show, but the work itself still succeeded.
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        if (needsPermission &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Viewing reminder")
            .setContentText("$title - ${ViewingTime.displayDate(date)} at $slot")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your viewing of $title is on ${ViewingTime.displayDate(date)} at $slot.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify("$title$date$slot".hashCode(), notification)

        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Viewing reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for your upcoming property viewings"
            }

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
