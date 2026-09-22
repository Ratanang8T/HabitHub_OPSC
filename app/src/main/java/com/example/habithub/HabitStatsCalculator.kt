package com.example.habithub

import java.util.Calendar
import java.util.Locale

object HabitStatsCalculator {

    // =========================================================
    // IS HABIT EXPECTED ON THIS DAY?
    // =========================================================

    fun isHabitExpectedOnDay(
        frequency: String,
        customDays: List<String>,
        dayOfWeek: Int
    ): Boolean {

        return when (
            frequency.uppercase(Locale.getDefault())
        ) {

            "DAILY" -> true

            "WEEKDAYS" -> {
                dayOfWeek != Calendar.SATURDAY &&
                        dayOfWeek != Calendar.SUNDAY
            }

            "CUSTOM" -> {

                val dayName =
                    getDayName(dayOfWeek)

                customDays.any { selectedDay ->

                    selectedDay.equals(
                        dayName,
                        ignoreCase = true
                    )
                }
            }

            else -> true
        }
    }

    // =========================================================
    // CALCULATE COMPLETION PERCENTAGE
    // =========================================================

    fun calculateCompletionPercentage(
        expectedDays: Int,
        completedDays: Int
    ): Int {

        if (
            expectedDays <= 0
        ) {
            return 0
        }

        val safeCompletedDays =
            completedDays.coerceIn(
                0,
                expectedDays
            )

        return (
                safeCompletedDays.toDouble() /
                        expectedDays.toDouble() *
                        100
                ).toInt()
    }

    // =========================================================
    // CALCULATE STREAK
    //
    // scheduledDates should be ordered:
    // oldest -> newest
    //
    // Example:
    // ["2026-09-14", "2026-09-15", "2026-09-16"]
    //
    // completedDates contains the dates completed.
    // =========================================================

    fun calculateCurrentStreak(
        scheduledDates: List<String>,
        completedDates: Set<String>
    ): Int {

        if (
            scheduledDates.isEmpty() ||
            completedDates.isEmpty()
        ) {
            return 0
        }

        var streak =
            0

        // Start with the newest scheduled date
        // and move backwards.

        for (
        date in scheduledDates.asReversed()
        ) {

            if (
                completedDates.contains(date)
            ) {

                streak++

            } else {

                break
            }
        }

        return streak
    }

    // =========================================================
    // CALCULATE BEST STREAK
    // =========================================================

    fun calculateBestStreak(
        scheduledDates: List<String>,
        completedDates: Set<String>
    ): Int {

        if (
            scheduledDates.isEmpty() ||
            completedDates.isEmpty()
        ) {
            return 0
        }

        var currentStreak =
            0

        var bestStreak =
            0

        scheduledDates.forEach { date ->

            if (
                completedDates.contains(date)
            ) {

                currentStreak++

                if (
                    currentStreak > bestStreak
                ) {

                    bestStreak =
                        currentStreak
                }

            } else {

                currentStreak =
                    0
            }
        }

        return bestStreak
    }

    // =========================================================
    // CUSTOM FREQUENCY VALIDATION
    // =========================================================

    fun isCustomFrequencyValid(
        frequency: String,
        customDays: List<String>
    ): Boolean {

        if (
            frequency.equals(
                "Custom",
                ignoreCase = true
            )
        ) {

            return customDays.isNotEmpty()
        }

        return true
    }

    // =========================================================
    // DAY NAME
    // =========================================================

    private fun getDayName(
        dayOfWeek: Int
    ): String {

        return when (
            dayOfWeek
        ) {

            Calendar.MONDAY ->
                "Monday"

            Calendar.TUESDAY ->
                "Tuesday"

            Calendar.WEDNESDAY ->
                "Wednesday"

            Calendar.THURSDAY ->
                "Thursday"

            Calendar.FRIDAY ->
                "Friday"

            Calendar.SATURDAY ->
                "Saturday"

            Calendar.SUNDAY ->
                "Sunday"

            else ->
                ""
        }
    }
}