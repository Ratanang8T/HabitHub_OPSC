package com.example.habithub

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val habitName =
            intent.getStringExtra("HABIT_NAME")
                ?: "Your habit"

        val notificationId =
            intent.getIntExtra(
                "NOTIFICATION_ID",
                System.currentTimeMillis().toInt()
            )

        createNotificationChannel(context)

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    "HabitHub Reminder"
                )
                .setContentText(
                    "Time for $habitName ✨"
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .build()

        // Android 13+ requires POST_NOTIFICATIONS permission
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                // Permission has not been granted.
                // Do not attempt to post the notification.
                return
            }
        }

        NotificationManagerCompat
            .from(context)
            .notify(
                notificationId,
                notification
            )
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Habit Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.description =
                "Notifications reminding you to complete your habits."

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager
                .createNotificationChannel(
                    channel
                )
        }
    }
}