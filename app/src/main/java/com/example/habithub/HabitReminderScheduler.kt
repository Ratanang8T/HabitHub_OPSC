package com.example.habithub

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.Locale

class HabitReminderScheduler(
    private val context: Context
) {

    // =========================================================
    // SCHEDULE HABIT REMINDER
    //
    // Daily
    // → Monday-Sunday
    //
    // Weekdays
    // → Monday-Friday
    //
    // Custom
    // → Only selected custom days
    // =========================================================

    fun scheduleReminder(
        habitId: String,
        habitName: String,
        reminderTime: String,
        frequency: String = "Daily",
        customDays: List<String> = emptyList()
    ) {

        // =====================================================
        // REMOVE OLD ALARMS FIRST
        // =====================================================

        cancelReminder(
            habitId
        )

        // =====================================================
        // NO REMINDER TIME
        // =====================================================

        if (
            reminderTime.isBlank()
        ) {

            return
        }

        // Expected format: HH:mm

        val parts =
            reminderTime.split(":")

        if (
            parts.size != 2
        ) {

            return
        }

        val hour =
            parts[0].toIntOrNull()
                ?: return

        val minute =
            parts[1].toIntOrNull()
                ?: return

        if (
            hour !in 0..23 ||
            minute !in 0..59
        ) {

            return
        }

        // =====================================================
        // WHICH DAYS SHOULD THIS HABIT RUN?
        // =====================================================

        val scheduledDays =
            when (
                frequency.uppercase(
                    Locale.getDefault()
                )
            ) {

                // =============================================
                // WEEKDAYS
                // =============================================

                "WEEKDAYS" ->

                    listOf(
                        Calendar.MONDAY,
                        Calendar.TUESDAY,
                        Calendar.WEDNESDAY,
                        Calendar.THURSDAY,
                        Calendar.FRIDAY
                    )

                // =============================================
                // CUSTOM
                // =============================================

                "CUSTOM" ->

                    customDays
                        .mapNotNull { dayName ->

                            dayNameToCalendarDay(
                                dayName
                            )
                        }
                        .distinct()

                // =============================================
                // DAILY / FALLBACK
                // =============================================

                else ->

                    listOf(
                        Calendar.MONDAY,
                        Calendar.TUESDAY,
                        Calendar.WEDNESDAY,
                        Calendar.THURSDAY,
                        Calendar.FRIDAY,
                        Calendar.SATURDAY,
                        Calendar.SUNDAY
                    )
            }

        if (
            scheduledDays.isEmpty()
        ) {

            return
        }

        // =====================================================
        // CREATE ONE WEEKLY ALARM FOR EACH SCHEDULED DAY
        // =====================================================

        scheduledDays.forEach { dayOfWeek ->

            scheduleWeeklyAlarm(
                habitId = habitId,
                habitName = habitName,
                hour = hour,
                minute = minute,
                dayOfWeek = dayOfWeek
            )
        }
    }

    // =========================================================
    // SCHEDULE ONE WEEKLY ALARM
    // =========================================================

    private fun scheduleWeeklyAlarm(
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int,
        dayOfWeek: Int
    ) {

        val calendar =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    hour
                )

                set(
                    Calendar.MINUTE,
                    minute
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )

                // =================================================
                // MOVE TO THE NEXT CORRECT WEEKDAY
                // =================================================

                val currentDay =
                    get(
                        Calendar.DAY_OF_WEEK
                    )

                var daysUntilTarget =
                    dayOfWeek -
                            currentDay

                if (
                    daysUntilTarget < 0
                ) {

                    daysUntilTarget +=
                        7
                }

                // =================================================
                // IF IT IS THE CORRECT DAY BUT THE TIME HAS PASSED,
                // USE NEXT WEEK.
                // =================================================

                if (
                    daysUntilTarget == 0 &&
                    timeInMillis <=
                    System.currentTimeMillis()
                ) {

                    daysUntilTarget =
                        7
                }

                add(
                    Calendar.DAY_OF_YEAR,
                    daysUntilTarget
                )
            }

        // =====================================================
        // UNIQUE REQUEST CODE
        //
        // Each habit/day combination must have its own alarm.
        // =====================================================

        val requestCode =
            createRequestCode(
                habitId,
                dayOfWeek
            )

        // =====================================================
        // RECEIVER INTENT
        // =====================================================

        val intent =
            Intent(
                context,
                HabitReminderReceiver::class.java
            ).apply {

                putExtra(
                    "HABIT_NAME",
                    habitName
                )

                putExtra(
                    "HABIT_ID",
                    habitId
                )

                putExtra(
                    "DAY_OF_WEEK",
                    dayOfWeek
                )

                putExtra(
                    "NOTIFICATION_ID",
                    requestCode
                )
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        // =====================================================
        // WEEKLY REPEATING ALARM
        //
        // No exact-alarm permission required.
        // Android may adjust delivery slightly for battery.
        // =====================================================

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    }

    // =========================================================
    // CANCEL ALL REMINDERS FOR A HABIT
    //
    // We check all seven possible weekday alarms.
    // =========================================================

    fun cancelReminder(
        habitId: String
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val allDays =
            listOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
                Calendar.SUNDAY
            )

        allDays.forEach { dayOfWeek ->

            val requestCode =
                createRequestCode(
                    habitId,
                    dayOfWeek
                )

            val intent =
                Intent(
                    context,
                    HabitReminderReceiver::class.java
                )

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or
                            PendingIntent.FLAG_IMMUTABLE
                )

            if (
                pendingIntent != null
            ) {

                alarmManager.cancel(
                    pendingIntent
                )

                pendingIntent.cancel()
            }
        }

        // =====================================================
        // ALSO CANCEL THE OLD DAILY ALARM
        //
        // Your previous scheduler used habitId.hashCode().
        // This removes it during migration to the new system.
        // =====================================================

        val oldIntent =
            Intent(
                context,
                HabitReminderReceiver::class.java
            )

        val oldPendingIntent =
            PendingIntent.getBroadcast(
                context,
                habitId.hashCode(),
                oldIntent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
            )

        if (
            oldPendingIntent != null
        ) {

            alarmManager.cancel(
                oldPendingIntent
            )

            oldPendingIntent.cancel()
        }
    }

    // =========================================================
    // CUSTOM DAY NAME → CALENDAR DAY
    // =========================================================

    private fun dayNameToCalendarDay(
        dayName: String
    ): Int? {

        return when (
            dayName.trim()
                .uppercase(
                    Locale.getDefault()
                )
        ) {

            "MONDAY" ->
                Calendar.MONDAY

            "TUESDAY" ->
                Calendar.TUESDAY

            "WEDNESDAY" ->
                Calendar.WEDNESDAY

            "THURSDAY" ->
                Calendar.THURSDAY

            "FRIDAY" ->
                Calendar.FRIDAY

            "SATURDAY" ->
                Calendar.SATURDAY

            "SUNDAY" ->
                Calendar.SUNDAY

            else ->
                null
        }
    }

    // =========================================================
    // UNIQUE REQUEST CODE
    // =========================================================

    private fun createRequestCode(
        habitId: String,
        dayOfWeek: Int
    ): Int {

        return "$habitId-$dayOfWeek"
            .hashCode()
    }
}