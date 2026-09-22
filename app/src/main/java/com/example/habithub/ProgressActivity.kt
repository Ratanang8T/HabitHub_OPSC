package com.example.habithub

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProgressActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var progressOverall: ProgressBar
    private lateinit var tvOverallPercentage: TextView
    private lateinit var tvProgressMessage: TextView

    private lateinit var dailyActivityContainer: LinearLayout
    private lateinit var categoryContainer: LinearLayout
    private lateinit var tvNoCategoryData: TextView

    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvBestStreak: TextView
    private lateinit var tvTotalCompletions: TextView
    private lateinit var tvHabitsTracked: TextView

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_progress
        )

        firebaseAuth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        // =====================================================
        // VIEWS
        // =====================================================

        progressOverall =
            findViewById(
                R.id.progressOverall
            )

        tvOverallPercentage =
            findViewById(
                R.id.tvOverallPercentage
            )

        tvProgressMessage =
            findViewById(
                R.id.tvProgressMessage
            )

        dailyActivityContainer =
            findViewById(
                R.id.dailyActivityContainer
            )

        categoryContainer =
            findViewById(
                R.id.categoryContainer
            )

        tvNoCategoryData =
            findViewById(
                R.id.tvNoCategoryData
            )

        tvCurrentStreak =
            findViewById(
                R.id.tvCurrentStreak
            )

        tvBestStreak =
            findViewById(
                R.id.tvBestStreak
            )

        tvTotalCompletions =
            findViewById(
                R.id.tvTotalCompletions
            )

        tvHabitsTracked =
            findViewById(
                R.id.tvHabitsTracked
            )

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        BottomNavHelper.setup(
            this,
            "PROGRESS"
        )

        loadProgress()
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (
            ::firestore.isInitialized
        ) {
            loadProgress()
        }
    }

    // =========================================================
    // LOAD HABITS
    // =========================================================

    private fun loadProgress() {

        val user =
            firebaseAuth.currentUser

        if (
            user == null
        ) {

            resetScreen()

            return
        }

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .get()
            .addOnSuccessListener { snapshot ->

                val habits =
                    snapshot.documents
                        .mapNotNull { document ->

                            val habit =
                                document.toObject(
                                    Habit::class.java
                                )

                            habit?.apply {
                                id = document.id
                            }
                        }

                tvHabitsTracked.text =
                    habits.size.toString()

                if (
                    habits.isEmpty()
                ) {

                    resetScreen()

                    tvHabitsTracked.text =
                        "0"

                    return@addOnSuccessListener
                }

                loadCompletionHistories(
                    habits
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Could not load progress: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()

                resetScreen()
            }
    }

    // =========================================================
    // LOAD COMPLETION HISTORIES
    // =========================================================

    private fun loadCompletionHistories(
        habits: List<Habit>
    ) {

        val user =
            firebaseAuth.currentUser
                ?: return

        val completionDatesByHabit =
            mutableMapOf<String, Set<String>>()

        var finishedRequests =
            0

        habits.forEach { habit ->

            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habit.id)
                .collection("completions")
                .get()
                .addOnSuccessListener { snapshot ->

                    val dates =
                        snapshot.documents
                            .mapNotNull { document ->

                                document.getString(
                                    "completionDate"
                                )
                            }
                            .toSet()

                    completionDatesByHabit[
                        habit.id
                    ] = dates

                    finishedRequests++

                    if (
                        finishedRequests ==
                        habits.size
                    ) {

                        calculateProgress(
                            habits,
                            completionDatesByHabit
                        )
                    }
                }
                .addOnFailureListener {

                    completionDatesByHabit[
                        habit.id
                    ] = emptySet()

                    finishedRequests++

                    if (
                        finishedRequests ==
                        habits.size
                    ) {

                        calculateProgress(
                            habits,
                            completionDatesByHabit
                        )
                    }
                }
        }
    }

    // =========================================================
    // CALCULATE EVERYTHING
    // =========================================================

    private fun calculateProgress(
        habits: List<Habit>,
        completionDatesByHabit:
        Map<String, Set<String>>
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {

            return
        }

        // =====================================================
        // TOTAL COMPLETIONS
        // =====================================================

        val totalCompletions =
            completionDatesByHabit
                .values
                .sumOf {
                    it.size
                }

        tvTotalCompletions.text =
            totalCompletions.toString()

        tvHabitsTracked.text =
            habits.size.toString()

        // =====================================================
        // EXPECTED VS ACTUAL
        //
        // Only scheduled days count.
        // =====================================================

        var totalExpected =
            0

        var totalActual =
            0

        habits.forEach { habit ->

            totalExpected +=
                calculateExpectedDays(
                    habit
                )

            totalActual +=
                countRelevantCompletions(
                    habit,
                    completionDatesByHabit[
                        habit.id
                    ] ?: emptySet()
                )
        }

        val overallPercentage =
            if (
                totalExpected == 0
            ) {

                0

            } else {

                (
                        totalActual
                            .toDouble() /
                                totalExpected *
                                100
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        100
                    )
            }

        progressOverall.progress =
            overallPercentage

        tvOverallPercentage.text =
            "$overallPercentage%"

        tvProgressMessage.text =
            when {

                totalCompletions == 0 ->

                    "Complete your first habit to start building progress."

                overallPercentage >= 90 ->

                    "Excellent consistency. Keep your momentum going!"

                overallPercentage >= 70 ->

                    "Great progress! You're building strong habits."

                overallPercentage >= 50 ->

                    "You're making progress. Keep showing up."

                else ->

                    "Every completion counts. Keep building your routine."
            }

        // =====================================================
        // ALL UNIQUE COMPLETION DATES
        // =====================================================

        val allCompletionDates =
            completionDatesByHabit
                .values
                .flatten()
                .toSet()

        // =====================================================
        // CURRENT STREAK
        // =====================================================

        val currentStreak =
            calculateCurrentStreak(
                allCompletionDates
            )

        tvCurrentStreak.text =
            currentStreak.toString()

        // =====================================================
        // BEST STREAK
        // =====================================================

        val bestStreak =
            calculateBestStreak(
                allCompletionDates
            )

        tvBestStreak.text =
            bestStreak.toString()

        // =====================================================
        // DAILY ACTIVITY GRAPH
        // =====================================================

        createDailyActivityGraph(
            completionDatesByHabit
        )

        // =====================================================
        // CATEGORY PERFORMANCE
        // =====================================================

        createCategoryPerformance(
            habits,
            completionDatesByHabit
        )
    }

    // =========================================================
    // EXPECTED DAYS
    // =========================================================

    private fun calculateExpectedDays(
        habit: Habit
    ): Int {

        if (
            habit.createdAt <= 0L
        ) {

            return 0
        }

        val start =
            Calendar.getInstance().apply {

                timeInMillis =
                    habit.createdAt

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val today =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        var expected =
            0

        val cursor =
            start.clone()
                    as Calendar

        while (
            !cursor.after(
                today
            )
        ) {

            if (
                isScheduledDay(
                    habit,
                    cursor
                )
            ) {

                expected++
            }

            cursor.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return expected
    }

    // =========================================================
    // RELEVANT COMPLETIONS
    //
    // A completion only counts toward the percentage if the
    // habit was actually scheduled for that day.
    // =========================================================

    private fun countRelevantCompletions(
        habit: Habit,
        dates: Set<String>
    ): Int {

        var count =
            0

        val today =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    23
                )

                set(
                    Calendar.MINUTE,
                    59
                )

                set(
                    Calendar.SECOND,
                    59
                )

                set(
                    Calendar.MILLISECOND,
                    999
                )
            }

        dates.forEach { dateString ->

            try {

                val date =
                    dateFormat.parse(
                        dateString
                    ) ?: return@forEach

                val calendar =
                    Calendar.getInstance().apply {

                        time =
                            date
                    }

                // =============================================
                // Ignore abnormal future completion documents.
                // =============================================

                if (
                    calendar.after(
                        today
                    )
                ) {

                    return@forEach
                }

                // =============================================
                // Ignore completions before habit creation.
                // =============================================

                if (
                    date.time <
                    habit.createdAt
                ) {

                    val creationDay =
                        dateFormat.format(
                            habit.createdAt
                        )

                    if (
                        creationDay !=
                        dateString
                    ) {

                        return@forEach
                    }
                }

                if (
                    isScheduledDay(
                        habit,
                        calendar
                    )
                ) {

                    count++
                }

            } catch (_: Exception) {

                // Ignore malformed completion date.
            }
        }

        return count
    }

    // =========================================================
    // SCHEDULED DAY
    //
    // DAILY
    // → Every day
    //
    // WEEKDAYS
    // → Monday-Friday
    //
    // CUSTOM
    // → Only customDays
    // =========================================================

    private fun isScheduledDay(
        habit: Habit,
        calendar: Calendar
    ): Boolean {

        val dayOfWeek =
            calendar.get(
                Calendar.DAY_OF_WEEK
            )

        return when (
            habit.frequency.uppercase(
                Locale.getDefault()
            )
        ) {

            // =================================================
            // DAILY
            // =================================================

            "DAILY" ->

                true

            // =================================================
            // WEEKDAYS
            // =================================================

            "WEEKDAYS" ->

                dayOfWeek !=
                        Calendar.SATURDAY &&
                        dayOfWeek !=
                        Calendar.SUNDAY

            // =================================================
            // CUSTOM
            // =================================================

            "CUSTOM" -> {

                val dayName =
                    getDayName(
                        dayOfWeek
                    )

                habit.customDays.any { selectedDay ->

                    selectedDay.equals(
                        dayName,
                        ignoreCase = true
                    )
                }
            }

            // =================================================
            // SAFE FALLBACK FOR OLD HABITS
            // =================================================

            else ->

                true
        }
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

    // =========================================================
    // CURRENT STREAK
    // =========================================================

    private fun calculateCurrentStreak(
        completedDates: Set<String>
    ): Int {

        if (
            completedDates.isEmpty()
        ) {

            return 0
        }

        val calendar =
            Calendar.getInstance()

        val today =
            dateFormat.format(
                calendar.time
            )

        if (
            !completedDates.contains(
                today
            )
        ) {

            calendar.add(
                Calendar.DAY_OF_YEAR,
                -1
            )
        }

        var streak =
            0

        while (
            true
        ) {

            val date =
                dateFormat.format(
                    calendar.time
                )

            if (
                completedDates.contains(
                    date
                )
            ) {

                streak++

                calendar.add(
                    Calendar.DAY_OF_YEAR,
                    -1
                )

            } else {

                break
            }
        }

        return streak
    }

    // =========================================================
    // BEST STREAK
    // =========================================================

    private fun calculateBestStreak(
        completedDates: Set<String>
    ): Int {

        if (
            completedDates.isEmpty()
        ) {

            return 0
        }

        val dates =
            completedDates
                .mapNotNull { dateString ->

                    try {

                        dateFormat.parse(
                            dateString
                        )

                    } catch (_: Exception) {

                        null
                    }
                }
                .sorted()

        if (
            dates.isEmpty()
        ) {

            return 0
        }

        var best =
            1

        var current =
            1

        for (
        index in 1 until dates.size
        ) {

            val previous =
                Calendar.getInstance().apply {

                    time =
                        dates[index - 1]
                }

            previous.add(
                Calendar.DAY_OF_YEAR,
                1
            )

            val expectedNext =
                dateFormat.format(
                    previous.time
                )

            val actual =
                dateFormat.format(
                    dates[index]
                )

            if (
                expectedNext ==
                actual
            ) {

                current++

                if (
                    current >
                    best
                ) {

                    best =
                        current
                }

            } else {

                current =
                    1
            }
        }

        return best
    }

    // =========================================================
    // DAILY ACTIVITY GRAPH
    // =========================================================

    private fun createDailyActivityGraph(
        completionDatesByHabit:
        Map<String, Set<String>>
    ) {

        dailyActivityContainer
            .removeAllViews()

        val today =
            Calendar.getInstance()

        val completionCounts =
            mutableListOf<Int>()

        val calendars =
            mutableListOf<Calendar>()

        // =====================================================
        // LAST 7 DAYS
        // =====================================================

        for (
        offset in 6 downTo 0
        ) {

            val day =
                today.clone()
                        as Calendar

            day.add(
                Calendar.DAY_OF_YEAR,
                -offset
            )

            calendars.add(
                day
            )

            val date =
                dateFormat.format(
                    day.time
                )

            val count =
                completionDatesByHabit
                    .values
                    .count { dates ->

                        dates.contains(
                            date
                        )
                    }

            completionCounts.add(
                count
            )
        }

        val maxCount =
            completionCounts
                .maxOrNull()
                ?.coerceAtLeast(
                    1
                )
                ?: 1

        // =====================================================
        // DRAW GRAPH
        // =====================================================

        completionCounts
            .forEachIndexed { index, count ->

                val column =
                    LinearLayout(this)

                column.orientation =
                    LinearLayout.VERTICAL

                column.gravity =
                    Gravity.BOTTOM or
                            Gravity.CENTER_HORIZONTAL

                column.layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                    )

                // =============================================
                // COUNT
                // =============================================

                val countText =
                    TextView(this)

                countText.text =
                    if (
                        count == 0
                    ) {

                        ""

                    } else {

                        count.toString()
                    }

                countText.textSize =
                    10f

                countText.gravity =
                    Gravity.CENTER

                countText.setTextColor(
                    Color.parseColor(
                        "#5B4CF0"
                    )
                )

                // =============================================
                // BAR
                // =============================================

                val bar =
                    View(this)

                val height =
                    if (
                        count == 0
                    ) {

                        dp(8)

                    } else {

                        val ratio =
                            count.toFloat() /
                                    maxCount.toFloat()

                        (
                                dp(130) *
                                        ratio
                                )
                            .toInt()
                            .coerceAtLeast(
                                dp(30)
                            )
                    }

                val barParams =
                    LinearLayout.LayoutParams(
                        dp(22),
                        height
                    )

                barParams.setMargins(
                    dp(3),
                    dp(4),
                    dp(3),
                    dp(8)
                )

                bar.layoutParams =
                    barParams

                bar.setBackgroundColor(

                    if (
                        count > 0
                    ) {

                        Color.parseColor(
                            "#5B4CF0"
                        )

                    } else {

                        Color.parseColor(
                            "#E2E0F8"
                        )
                    }
                )

                // =============================================
                // DAY
                // =============================================

                val label =
                    TextView(this)

                label.text =
                    SimpleDateFormat(
                        "EEE",
                        Locale.getDefault()
                    )
                        .format(
                            calendars[index].time
                        )
                        .take(1)

                label.textSize =
                    10f

                label.gravity =
                    Gravity.CENTER

                label.setTextColor(
                    Color.parseColor(
                        "#858998"
                    )
                )

                column.addView(
                    countText
                )

                column.addView(
                    bar
                )

                column.addView(
                    label
                )

                dailyActivityContainer
                    .addView(
                        column
                    )
            }
    }

    // =========================================================
    // CATEGORY PERFORMANCE
    // =========================================================

    private fun createCategoryPerformance(
        habits: List<Habit>,
        completionDatesByHabit:
        Map<String, Set<String>>
    ) {

        categoryContainer
            .removeAllViews()

        val categories =
            habits
                .groupBy {

                    it.category.ifBlank {
                        "Other"
                    }
                }

        if (
            categories.isEmpty()
        ) {

            val empty =
                TextView(this)

            empty.text =
                "No category data yet."

            empty.gravity =
                Gravity.CENTER

            empty.setPadding(
                0,
                dp(20),
                0,
                dp(20)
            )

            empty.setTextColor(
                Color.parseColor(
                    "#858998"
                )
            )

            categoryContainer.addView(
                empty
            )

            return
        }

        categories
            .forEach { (category, categoryHabits) ->

                var expected =
                    0

                var actual =
                    0

                categoryHabits
                    .forEach { habit ->

                        expected +=
                            calculateExpectedDays(
                                habit
                            )

                        actual +=
                            countRelevantCompletions(
                                habit,
                                completionDatesByHabit[
                                    habit.id
                                ] ?: emptySet()
                            )
                    }

                val percentage =
                    if (
                        expected == 0
                    ) {

                        0

                    } else {

                        (
                                actual
                                    .toDouble() /
                                        expected *
                                        100
                                )
                            .toInt()
                            .coerceIn(
                                0,
                                100
                            )
                    }

                addCategoryRow(
                    category,
                    percentage
                )
            }
    }

    // =========================================================
    // CATEGORY ROW
    // =========================================================

    private fun addCategoryRow(
        category: String,
        percentage: Int
    ) {

        val wrapper =
            LinearLayout(this)

        wrapper.orientation =
            LinearLayout.VERTICAL

        val wrapperParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        wrapperParams.setMargins(
            0,
            dp(8),
            0,
            dp(12)
        )

        wrapper.layoutParams =
            wrapperParams

        // =====================================================
        // TOP ROW
        // =====================================================

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        val name =
            TextView(this)

        name.text =
            category

        name.textSize =
            14f

        name.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        name.setTextColor(
            Color.parseColor(
                "#15182A"
            )
        )

        name.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        val percentageText =
            TextView(this)

        percentageText.text =
            "$percentage%"

        percentageText.textSize =
            14f

        percentageText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        percentageText.setTextColor(
            Color.parseColor(
                "#5B4CF0"
            )
        )

        row.addView(
            name
        )

        row.addView(
            percentageText
        )

        // =====================================================
        // PROGRESS BAR
        // =====================================================

        val progress =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            )

        progress.max =
            100

        progress.progress =
            percentage

        val progressParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
            )

        progressParams.setMargins(
            0,
            dp(8),
            0,
            0
        )

        progress.layoutParams =
            progressParams

        wrapper.addView(
            row
        )

        wrapper.addView(
            progress
        )

        categoryContainer.addView(
            wrapper
        )
    }

    // =========================================================
    // RESET
    // =========================================================

    private fun resetScreen() {

        if (
            !::progressOverall.isInitialized
        ) {

            return
        }

        progressOverall.progress =
            0

        tvOverallPercentage.text =
            "0%"

        tvProgressMessage.text =
            "Start completing habits to build your progress."

        tvCurrentStreak.text =
            "0"

        tvBestStreak.text =
            "0"

        tvTotalCompletions.text =
            "0"

        tvHabitsTracked.text =
            "0"

        dailyActivityContainer
            .removeAllViews()

        categoryContainer
            .removeAllViews()

        val empty =
            TextView(this)

        empty.text =
            "No category data yet."

        empty.gravity =
            Gravity.CENTER

        empty.setPadding(
            0,
            dp(20),
            0,
            dp(20)
        )

        empty.setTextColor(
            Color.parseColor(
                "#858998"
            )
        )

        categoryContainer.addView(
            empty
        )
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
    }
}