package com.example.rentview.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.rentview.R
import com.example.rentview.data.ViewingTime

// Runs in the background and displays a notification for an upcoming viewing.
class ViewingReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_DATE = "date"
        const val KEY_SLOT = "slot"
        const val KEY_VIEWING_ID = "viewing_id"

        private const val CHANNEL_ID = "viewing_reminders"
    }

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {

        val title = inputData.getString(KEY_TITLE)
            ?: return Result.failure()

        val date = inputData.getString(KEY_DATE)
            ?: return Result.failure()

        val slot = inputData.getString(KEY_SLOT)
            ?: return Result.failure()

        val viewingId = inputData.getString(KEY_VIEWING_ID)
            ?: "$title$date$slot"

        createChannel()

        // Android 13 and later require notification permission.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        // Open RentView when the tenant taps the notification.
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)

        val contentPendingIntent = launchIntent?.let { intent ->

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP

            PendingIntent.getActivity(
                applicationContext,
                viewingId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Your property viewing is coming up")
            .setContentText(
                "$title — ${ViewingTime.displayDate(date)} at $slot"
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Reminder: You have a property viewing scheduled.\n\n" +
                                "Property: $title\n" +
                                "Date: ${ViewingTime.displayDate(date)}\n" +
                                "Time: $slot\n\n" +
                                "Please be ready for your appointment."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(viewingId.hashCode(), notification)

        return Result.success()
    }

    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Viewing reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Notifications reminding tenants about upcoming property viewings"

                enableVibration(true)
            }

            val manager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}