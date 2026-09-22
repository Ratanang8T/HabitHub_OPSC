package com.example.habithub

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
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

class HomeActivity : AppCompatActivity() {

    private lateinit var habitContainer: LinearLayout

    // =========================================================
    // RETROFIT REST REPOSITORY
    // =========================================================

    private val restRepository =
        FirestoreRestRepository()

    // =========================================================
    // FIREBASE
    // =========================================================

    private val firebaseAuth =
        FirebaseAuth.getInstance()

    private val firestore =
        FirebaseFirestore.getInstance()

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_home
        )

        // =====================================================
        // XML VIEWS
        // =====================================================

        val tvGreeting =
            findViewById<TextView>(
                R.id.tvGreeting
            )

        val btnCreateHabit =
            findViewById<Button>(
                R.id.btnCreateHabit
            )

        val btnViewHabits =
            findViewById<Button>(
                R.id.btnViewHabits
            )

        habitContainer =
            findViewById(
                R.id.habitContainer
            )

        // =====================================================
        // USER GREETING
        // =====================================================

        val fullNameFromIntent =
            intent.getStringExtra(
                "FULL_NAME"
            )

        val userPreferences =
            getSharedPreferences(
                "UserAccount",
                MODE_PRIVATE
            )

        val savedName =
            userPreferences.getString(
                "FULL_NAME",
                "Student"
            ) ?: "Student"

        val fullName =
            fullNameFromIntent
                ?: savedName

        val firstName =
            fullName
                .trim()
                .split(" ")
                .firstOrNull()
                ?: "Student"

        tvGreeting.text =
            getGreeting(
                firstName
            )

        // =====================================================
        // CREATE HABIT
        // =====================================================

        btnCreateHabit.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CreateHabitActivity::class.java
                )
            )
        }

        // =====================================================
        // VIEW HABITS
        // =====================================================

        btnViewHabits.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    HabitsActivity::class.java
                )
            )
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        BottomNavHelper.setup(
            this,
            "HOME"
        )
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (
            ::habitContainer.isInitialized
        ) {

            loadHabits()
        }
    }

    // =========================================================
    // GREETING
    // =========================================================

    private fun getGreeting(
        firstName: String
    ): String {

        val hour =
            Calendar
                .getInstance()
                .get(
                    Calendar.HOUR_OF_DAY
                )

        return when (hour) {

            in 5..11 ->
                "Good morning, $firstName"

            in 12..17 ->
                "Good afternoon, $firstName"

            else ->
                "Good evening, $firstName"
        }
    }

    // =========================================================
    // LOAD HABITS
    //
    // IMPORTANT:
    // Habit list still comes through Retrofit + REST.
    // =========================================================

    private fun loadHabits() {

        habitContainer.removeAllViews()

        val loadingText =
            TextView(this)

        loadingText.text =
            "Loading habits..."

        loadingText.textSize =
            14f

        loadingText.setTextColor(
            Color.parseColor(
                "#7D8190"
            )
        )

        loadingText.setPadding(
            0,
            dp(24),
            0,
            dp(24)
        )

        habitContainer.addView(
            loadingText
        )

        // =====================================================
        // RETROFIT + FIRESTORE REST API
        // =====================================================

        restRepository.getHabits(

            onSuccess = { habits ->

                if (
                    isFinishing ||
                    isDestroyed
                ) {

                    return@getHabits
                }

                // =================================================
                // EMPTY STATE
                // =================================================

                if (
                    habits.isEmpty()
                ) {

                    habitContainer.removeAllViews()

                    resetDashboard()

                    showEmptyMessage()

                    return@getHabits
                }

                // =================================================
                // LOAD REAL COMPLETION HISTORY
                // =================================================

                loadCompletionHistory(
                    habits
                )
            },

            onError = { errorMessage ->

                if (
                    isFinishing ||
                    isDestroyed
                ) {

                    return@getHabits
                }

                habitContainer.removeAllViews()

                resetDashboard()

                Toast.makeText(
                    this,
                    "Could not load habits: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()

                showErrorMessage()
            }
        )
    }

    // =========================================================
    // LOAD COMPLETION HISTORY
    // =========================================================

    private fun loadCompletionHistory(
        habits: List<Habit>
    ) {

        val user =
            firebaseAuth.currentUser

        if (
            user == null
        ) {

            habitContainer.removeAllViews()

            resetDashboard()

            Toast.makeText(
                this,
                "Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

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

                        finishLoadingDashboard(
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

                        finishLoadingDashboard(
                            habits,
                            completionDatesByHabit
                        )
                    }
                }
        }
    }

    // =========================================================
    // FINISH LOADING DASHBOARD
    // =========================================================

    private fun finishLoadingDashboard(
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

        val today =
            getTodayDate()

        // =====================================================
        // TODAY'S REAL COMPLETION STATE
        // =====================================================

        habits.forEach { habit ->

            val completionDates =
                completionDatesByHabit[
                    habit.id
                ] ?: emptySet()

            habit.completed =
                completionDates.contains(
                    today
                )
        }

        // =====================================================
        // TODAY'S COMPLETED COUNT
        // =====================================================

        val activeHabits =
            habits.filter {

                !it.paused
            }

        val completedToday =
            activeHabits.count {

                it.completed
            }

        updateTodayStatistics(
            totalHabits =
                activeHabits.size,

            completedHabits =
                completedToday
        )

        // =====================================================
        // STREAK + WEEKLY PROGRESS
        // =====================================================

        calculateDashboardProgress(
            habits,
            completionDatesByHabit
        )

        // =====================================================
        // DISPLAY CARDS
        // =====================================================

        habitContainer.removeAllViews()

        habits.forEach { habit ->

            createHomeHabitCard(
                habit
            )
        }
    }

    // =========================================================
    // TODAY STATISTICS
    // =========================================================

    private fun updateTodayStatistics(
        totalHabits: Int,
        completedHabits: Int
    ) {

        val tvCompleted =
            findViewById<TextView>(
                R.id.tvCompleted
            )

        tvCompleted.text =
            "$completedHabits/$totalHabits"
    }

    // =========================================================
    // DASHBOARD PROGRESS
    // =========================================================

    private fun calculateDashboardProgress(
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

        val tvStreak =
            findViewById<TextView>(
                R.id.tvStreak
            )

        val tvFocusScore =
            findViewById<TextView>(
                R.id.tvFocusScore
            )

        val progressWeekly =
            findViewById<ProgressBar>(
                R.id.progressWeekly
            )

        val tvWeeklyProgress =
            findViewById<TextView>(
                R.id.tvWeeklyProgress
            )

        // =====================================================
        // OVERALL STREAK
        // =====================================================

        val allCompletionDates =
            completionDatesByHabit
                .values
                .flatten()
                .toSet()

        val currentStreak =
            calculateOverallStreak(
                allCompletionDates
            )

        tvStreak.text =
            currentStreak.toString()

        // =====================================================
        // DATE FORMAT
        // =====================================================

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        // =====================================================
        // MONDAY OF CURRENT WEEK
        // =====================================================

        val monday =
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

                firstDayOfWeek =
                    Calendar.MONDAY

                val currentDay =
                    get(
                        Calendar.DAY_OF_WEEK
                    )

                val daysFromMonday =
                    when (
                        currentDay
                    ) {

                        Calendar.SUNDAY ->
                            6

                        else ->
                            currentDay -
                                    Calendar.MONDAY
                    }

                add(
                    Calendar.DAY_OF_YEAR,
                    -daysFromMonday
                )
            }

        // =====================================================
        // TODAY
        // =====================================================

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

        var expectedCompletions =
            0

        var actualCompletions =
            0

        // =====================================================
        // EACH HABIT
        // =====================================================

        habits.forEach { habit ->

            // =================================================
            // PAUSED HABITS
            // =================================================

            if (
                habit.paused
            ) {

                return@forEach
            }

            // =================================================
            // CREATION DATE
            // =================================================

            val createdDate =
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

            // =================================================
            // START FROM THE LATER OF:
            //
            // - Monday
            // - Habit creation date
            // =================================================

            val startDate =
                if (
                    createdDate.after(
                        monday
                    )
                ) {

                    createdDate.clone()
                            as Calendar

                } else {

                    monday.clone()
                            as Calendar
                }

            val cursor =
                startDate.clone()
                        as Calendar

            // =================================================
            // EXPECTED + ACTUAL
            // =================================================

            while (
                !cursor.after(
                    today
                )
            ) {

                val expectedToday =
                    isHabitExpectedOnDate(
                        habit,
                        cursor
                    )

                if (
                    expectedToday
                ) {

                    expectedCompletions++

                    val dateString =
                        dateFormat.format(
                            cursor.time
                        )

                    val completionDates =
                        completionDatesByHabit[
                            habit.id
                        ] ?: emptySet()

                    if (
                        completionDates.contains(
                            dateString
                        )
                    ) {

                        actualCompletions++
                    }
                }

                cursor.add(
                    Calendar.DAY_OF_YEAR,
                    1
                )
            }
        }

        // =====================================================
        // WEEKLY %
        // =====================================================

        val weeklyPercentage =
            if (
                expectedCompletions <= 0
            ) {

                0

            } else {

                (
                        actualCompletions
                            .toDouble() /
                                expectedCompletions *
                                100
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        100
                    )
            }

        progressWeekly.progress =
            weeklyPercentage

        tvWeeklyProgress.text =
            "$weeklyPercentage% completed"

        // =====================================================
        // FOCUS SCORE
        // =====================================================

        tvFocusScore.text =
            "$weeklyPercentage%"
    }

    // =========================================================
    // IS HABIT EXPECTED ON DATE?
    //
    // This is now the central Home frequency calculation.
    //
    // DAILY
    // → Every day
    //
    // WEEKDAYS
    // → Monday-Friday
    //
    // CUSTOM
    // → Only selected customDays
    // =========================================================

    private fun isHabitExpectedOnDate(
        habit: Habit,
        date: Calendar
    ): Boolean {

        val dayOfWeek =
            date.get(
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
            // SAFE FALLBACK FOR OLD DATA
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
    // OVERALL STREAK
    // =========================================================

    private fun calculateOverallStreak(
        completedDates: Set<String>
    ): Int {

        if (
            completedDates.isEmpty()
        ) {

            return 0
        }

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        val calendar =
            Calendar.getInstance()

        val today =
            dateFormat.format(
                calendar.time
            )

        // =====================================================
        // If nothing was completed today, start yesterday.
        // =====================================================

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
    // RESET DASHBOARD
    // =========================================================

    private fun resetDashboard() {

        val tvCompleted =
            findViewById<TextView>(
                R.id.tvCompleted
            )

        tvCompleted.text =
            "0/0"

        resetProgressStatistics()
    }

    // =========================================================
    // RESET PROGRESS
    // =========================================================

    private fun resetProgressStatistics() {

        val tvStreak =
            findViewById<TextView>(
                R.id.tvStreak
            )

        val tvFocusScore =
            findViewById<TextView>(
                R.id.tvFocusScore
            )

        val progressWeekly =
            findViewById<ProgressBar>(
                R.id.progressWeekly
            )

        val tvWeeklyProgress =
            findViewById<TextView>(
                R.id.tvWeeklyProgress
            )

        tvStreak.text =
            "0"

        tvFocusScore.text =
            "0%"

        progressWeekly.progress =
            0

        tvWeeklyProgress.text =
            "0% completed"
    }

    // =========================================================
    // HOME HABIT CARD
    // =========================================================

    private fun createHomeHabitCard(
        habit: Habit
    ) {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )

        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            dp(12),
            0,
            0
        )

        card.layoutParams =
            params

        card.setBackgroundResource(
            R.drawable.bg_card
        )

        card.isClickable =
            true

        // =====================================================
        // NAME
        // =====================================================

        val title =
            TextView(this)

        title.text =
            habit.name

        title.textSize =
            16f

        title.setTextColor(
            Color.parseColor(
                "#15182A"
            )
        )

        title.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        // =====================================================
        // REMINDER + CATEGORY
        // =====================================================

        val details =
            TextView(this)

        val displayTime =
            if (
                habit.reminderTime.isBlank()
            ) {

                "No reminder"

            } else {

                habit.reminderTime
            }

        details.text =
            "$displayTime  •  ${habit.category}"

        details.textSize =
            12f

        details.setTextColor(
            Color.parseColor(
                "#858998"
            )
        )

        details.setPadding(
            0,
            dp(7),
            0,
            0
        )

        // =====================================================
        // GOAL
        // =====================================================

        val goalText =
            TextView(this)

        val goalDisplay =
            if (
                habit.goal.isBlank() &&
                habit.unit.isBlank()
            ) {

                "No goal set"

            } else {

                "${habit.goal} ${habit.unit}"
                    .trim()
            }

        goalText.text =
            "Goal: $goalDisplay"

        goalText.textSize =
            12f

        goalText.setTextColor(
            Color.parseColor(
                "#5B4CF0"
            )
        )

        goalText.setPadding(
            0,
            dp(8),
            0,
            0
        )

        // =====================================================
        // FREQUENCY
        //
        // Custom habits show their selected days.
        // =====================================================

        val frequencyText =
            TextView(this)

        frequencyText.text =
            when {

                habit.frequency.equals(
                    "Custom",
                    ignoreCase = true
                ) &&
                        habit.customDays.isNotEmpty() ->

                    "Repeats: ${
                        habit.customDays.joinToString(
                            ", "
                        )
                    }"

                else ->

                    "Repeats: ${habit.frequency}"
            }

        frequencyText.textSize =
            12f

        frequencyText.setTextColor(
            Color.parseColor(
                "#7D8190"
            )
        )

        frequencyText.setPadding(
            0,
            dp(7),
            0,
            0
        )

        // =====================================================
        // STATUS
        // =====================================================

        val status =
            TextView(this)

        status.text =
            when {

                habit.paused ->

                    "⏸ Habit paused"

                habit.completed ->

                    "✓ Completed today"

                else ->

                    "○ Not completed yet"
            }

        status.textSize =
            12f

        status.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        status.setTextColor(

            when {

                habit.paused ->

                    Color.parseColor(
                        "#E28A24"
                    )

                habit.completed ->

                    Color.parseColor(
                        "#21A78D"
                    )

                else ->

                    Color.parseColor(
                        "#858998"
                    )
            }
        )

        status.setPadding(
            0,
            dp(10),
            0,
            0
        )

        // =====================================================
        // ADD VIEWS
        // =====================================================

        card.addView(
            title
        )

        card.addView(
            details
        )

        card.addView(
            goalText
        )

        card.addView(
            frequencyText
        )

        card.addView(
            status
        )

        // =====================================================
        // OPEN DETAILS
        // =====================================================

        card.setOnClickListener {

            if (
                habit.id.isBlank()
            ) {

                Toast.makeText(
                    this,
                    "Could not open this habit.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val intent =
                Intent(
                    this,
                    HabitDetailsActivity::class.java
                )

            intent.putExtra(
                "HABIT_ID",
                habit.id
            )

            startActivity(
                intent
            )
        }

        habitContainer.addView(
            card
        )
    }

    // =========================================================
    // TODAY
    // =========================================================

    private fun getTodayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(
            Calendar
                .getInstance()
                .time
        )
    }

    // =========================================================
    // EMPTY
    // =========================================================

    private fun showEmptyMessage() {

        val emptyText =
            TextView(this)

        emptyText.text =
            "No habits yet. Create your first habit."

        emptyText.textSize =
            14f

        emptyText.setTextColor(
            Color.parseColor(
                "#7D8190"
            )
        )

        emptyText.setPadding(
            0,
            dp(24),
            0,
            dp(24)
        )

        habitContainer.addView(
            emptyText
        )
    }

    // =========================================================
    // ERROR
    // =========================================================

    private fun showErrorMessage() {

        val errorText =
            TextView(this)

        errorText.text =
            "Unable to load your habits. Please try again."

        errorText.textSize =
            14f

        errorText.setTextColor(
            Color.parseColor(
                "#7D8190"
            )
        )

        errorText.setPadding(
            0,
            dp(24),
            0,
            dp(24)
        )

        habitContainer.addView(
            errorText
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