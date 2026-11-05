package com.example.habithub

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HabitHubMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)

        val title =
            remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: "HabitHub"

        val message =
            remoteMessage.notification?.body
                ?: remoteMessage.data["message"]
                ?: "You have a new HabitHub update."

        showNotification(
            title,
            message
        )
    }

    override fun onNewToken(
        token: String
    ) {
        super.onNewToken(token)

        // The latest FCM token can later be stored in Firestore.
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(
        token: String
    ) {

        val user =
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser
                ?: return

        com.google.firebase.firestore.FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(user.uid)
            .update(
                "fcmToken",
                token
            )
    }

    private fun showNotification(
        title: String,
        message: String
    ) {

        val channelId =
            "habithub_push_notifications"

        val notificationManager =
            getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    channelId,
                    "HabitHub Updates",
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.description =
                "Real-time HabitHub notifications"

            notificationManager.createNotificationChannel(
                channel
            )
        }

        val intent =
            Intent(
                this,
                HomeActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat
                .Builder(
                    this,
                    channelId
                )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    message
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )
                .build()

        notificationManager.notify(
            System.currentTimeMillis()
                .toInt(),
            notification
        )
    }
}