package com.example.habithub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HabitStatsCalculatorTest {

    // =========================================================
    // DAILY FREQUENCY
    // =========================================================

    @Test
    fun dailyHabit_shouldBeExpectedOnSaturday() {

        val result =
            HabitStatsCalculator.isHabitExpectedOnDay(
                frequency = "Daily",
                customDays = emptyList(),
                dayOfWeek = Calendar.SATURDAY
            )

        assertTrue(result)
    }

    // =========================================================
    // WEEKDAY FREQUENCY
    // =========================================================

    @Test
    fun weekdayHabit_shouldBeExpectedOnMonday() {

        val result =
            HabitStatsCalculator.isHabitExpectedOnDay(
                frequency = "Weekdays",
                customDays = emptyList(),
                dayOfWeek = Calendar.MONDAY
            )

        assertTrue(result)
    }

    @Test
    fun weekdayHabit_shouldNotBeExpectedOnSaturday() {

        val result =
            HabitStatsCalculator.isHabitExpectedOnDay(
                frequency = "Weekdays",
                customDays = emptyList(),
                dayOfWeek = Calendar.SATURDAY
            )

        assertFalse(result)
    }

    // =========================================================
    // CUSTOM FREQUENCY
    // =========================================================

    @Test
    fun customHabit_shouldBeExpectedOnSelectedDay() {

        val result =
            HabitStatsCalculator.isHabitExpectedOnDay(
                frequency = "Custom",
                customDays =
                    listOf(
                        "Monday",
                        "Wednesday",
                        "Friday"
                    ),
                dayOfWeek = Calendar.WEDNESDAY
            )

        assertTrue(result)
    }

    @Test
    fun customHabit_shouldNotBeExpectedOnUnselectedDay() {

        val result =
            HabitStatsCalculator.isHabitExpectedOnDay(
                frequency = "Custom",
                customDays =
                    listOf(
                        "Monday",
                        "Wednesday",
                        "Friday"
                    ),
                dayOfWeek = Calendar.TUESDAY
            )

        assertFalse(result)
    }

    // =========================================================
    // COMPLETION PERCENTAGE
    // =========================================================

    @Test
    fun completionPercentage_threeOutOfFour_shouldBe75() {

        val result =
            HabitStatsCalculator.calculateCompletionPercentage(
                expectedDays = 4,
                completedDays = 3
            )

        assertEquals(
            75,
            result
        )
    }

    @Test
    fun completionPercentage_zeroExpectedDays_shouldBeZero() {

        val result =
            HabitStatsCalculator.calculateCompletionPercentage(
                expectedDays = 0,
                completedDays = 0
            )

        assertEquals(
            0,
            result
        )
    }

    // =========================================================
    // CURRENT STREAK
    // =========================================================

    @Test
    fun fourCompletedScheduledDays_shouldGiveCurrentStreakOfFour() {

        val scheduledDates =
            listOf(
                "2026-09-14",
                "2026-09-15",
                "2026-09-16",
                "2026-09-17"
            )

        val completedDates =
            setOf(
                "2026-09-14",
                "2026-09-15",
                "2026-09-16",
                "2026-09-17"
            )

        val result =
            HabitStatsCalculator.calculateCurrentStreak(
                scheduledDates,
                completedDates
            )

        assertEquals(
            4,
            result
        )
    }

    // =========================================================
    // BEST STREAK
    // =========================================================

    @Test
    fun missedDay_shouldBreakStreakButPreserveBestStreak() {

        val scheduledDates =
            listOf(
                "2026-09-14",
                "2026-09-15",
                "2026-09-16",
                "2026-09-17",
                "2026-09-18"
            )

        val completedDates =
            setOf(
                "2026-09-14",
                "2026-09-15",
                "2026-09-17",
                "2026-09-18"
            )

        val result =
            HabitStatsCalculator.calculateBestStreak(
                scheduledDates,
                completedDates
            )

        assertEquals(
            2,
            result
        )
    }

    // =========================================================
    // CUSTOM VALIDATION
    // =========================================================

    @Test
    fun customFrequency_withoutSelectedDays_shouldBeInvalid() {

        val result =
            HabitStatsCalculator.isCustomFrequencyValid(
                frequency = "Custom",
                customDays = emptyList()
            )

        assertFalse(result)
    }

    @Test
    fun customFrequency_withSelectedDays_shouldBeValid() {

        val result =
            HabitStatsCalculator.isCustomFrequencyValid(
                frequency = "Custom",
                customDays =
                    listOf(
                        "Monday",
                        "Friday"
                    )
            )

        assertTrue(result)
    }
}