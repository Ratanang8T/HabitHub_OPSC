package com.example.habithub

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
import java.util.Date
import java.util.Locale

class HabitDetailsActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvHabitTitle: TextView
    private lateinit var tvHabitCategory: TextView
    private lateinit var tvReminder: TextView
    private lateinit var tvGoal: TextView

    private lateinit var tvCompletionPercentage: TextView
    private lateinit var progressHabit: ProgressBar

    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvBestStreak: TextView
    private lateinit var tvTotalDays: TextView

    private lateinit var tvMonday: TextView
    private lateinit var tvTuesday: TextView
    private lateinit var tvWednesday: TextView
    private lateinit var tvThursday: TextView
    private lateinit var tvFriday: TextView
    private lateinit var tvSaturday: TextView
    private lateinit var tvSunday: TextView

    private lateinit var pastSevenDaysContainer: LinearLayout
    private lateinit var tvInsight: TextView

    private lateinit var btnCompleteToday: Button

    private var habitId: String = ""
    private var currentHabit: Habit? = null

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
            R.layout.activity_habit_details
        )

        firebaseAuth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        // =====================================================
        // VIEWS
        // =====================================================

        val tvBack =
            findViewById<TextView>(
                R.id.tvBack
            )

        val tvEditHabit =
            findViewById<TextView>(
                R.id.tvEditHabit
            )

        tvHabitTitle =
            findViewById(
                R.id.tvHabitTitle
            )

        tvHabitCategory =
            findViewById(
                R.id.tvHabitCategory
            )

        tvReminder =
            findViewById(
                R.id.tvReminder
            )

        tvGoal =
            findViewById(
                R.id.tvGoal
            )

        tvCompletionPercentage =
            findViewById(
                R.id.tvCompletionPercentage
            )

        progressHabit =
            findViewById(
                R.id.progressHabit
            )

        tvCurrentStreak =
            findViewById(
                R.id.tvCurrentStreak
            )

        tvBestStreak =
            findViewById(
                R.id.tvBestStreak
            )

        tvTotalDays =
            findViewById(
                R.id.tvTotalDays
            )

        tvMonday =
            findViewById(
                R.id.tvMonday
            )

        tvTuesday =
            findViewById(
                R.id.tvTuesday
            )

        tvWednesday =
            findViewById(
                R.id.tvWednesday
            )

        tvThursday =
            findViewById(
                R.id.tvThursday
            )

        tvFriday =
            findViewById(
                R.id.tvFriday
            )

        tvSaturday =
            findViewById(
                R.id.tvSaturday
            )

        tvSunday =
            findViewById(
                R.id.tvSunday
            )

        pastSevenDaysContainer =
            findViewById(
                R.id.pastSevenDaysContainer
            )

        tvInsight =
            findViewById(
                R.id.tvInsight
            )

        btnCompleteToday =
            findViewById(
                R.id.btnCompleteToday
            )

        // =====================================================
        // HABIT ID
        // =====================================================

        habitId =
            intent.getStringExtra(
                "HABIT_ID"
            ) ?: ""

        if (
            habitId.isBlank()
        ) {

            Toast.makeText(
                this,
                "Habit could not be loaded.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        // =====================================================
        // BACK
        // =====================================================

        tvBack.setOnClickListener {

            finish()
        }

        // =====================================================
        // EDIT
        // =====================================================

        tvEditHabit.setOnClickListener {

            val editIntent =
                Intent(
                    this,
                    CreateHabitActivity::class.java
                )

            editIntent.putExtra(
                "HABIT_ID",
                habitId
            )

            startActivity(
                editIntent
            )
        }

        // =====================================================
        // COMPLETE / UNDO
        // =====================================================

        btnCompleteToday.setOnClickListener {

            val habit =
                currentHabit
                    ?: return@setOnClickListener

            if (
                habit.completed
            ) {

                undoCompletion()

            } else {

                markCompleted()
            }
        }

        loadHabit()
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (
            ::firestore.isInitialized &&
            habitId.isNotBlank()
        ) {

            loadHabit()
        }
    }

    // =========================================================
    // LOAD HABIT
    // =========================================================

    private fun loadHabit() {

        val user =
            firebaseAuth.currentUser

        if (
            user == null
        ) {

            Toast.makeText(
                this,
                "Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .document(habitId)
            .get()
            .addOnSuccessListener { document ->

                if (
                    !document.exists()
                ) {

                    Toast.makeText(
                        this,
                        "Habit no longer exists.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                    return@addOnSuccessListener
                }

                val habit =
                    document.toObject(
                        Habit::class.java
                    )

                if (
                    habit == null
                ) {

                    Toast.makeText(
                        this,
                        "Unable to read habit.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                habit.id =
                    document.id

                currentHabit =
                    habit

                // =================================================
                // TODAY'S REAL COMPLETION STATE
                // =================================================

                loadTodayCompletionState(
                    habit
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Could not load habit: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // TODAY COMPLETION STATE
    //
    // Do not trust the permanent completed Boolean.
    // Check today's dated completion document.
    // =========================================================

    private fun loadTodayCompletionState(
        habit: Habit
    ) {

        val user =
            firebaseAuth.currentUser
                ?: return

        val today =
            dateFormat.format(
                Date()
            )

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .document(habit.id)
            .collection("completions")
            .document(today)
            .get()
            .addOnSuccessListener { document ->

                habit.completed =
                    document.exists()

                currentHabit =
                    habit

                displayHabit(
                    habit
                )

                loadCompletionHistory(
                    habit
                )
            }
            .addOnFailureListener {

                displayHabit(
                    habit
                )

                loadCompletionHistory(
                    habit
                )
            }
    }

    // =========================================================
    // DISPLAY HABIT
    // =========================================================

    private fun displayHabit(
        habit: Habit
    ) {

        tvHabitTitle.text =
            habit.name

        tvHabitCategory.text =
            habit.category

        tvReminder.text =
            if (
                habit.reminderTime.isBlank()
            ) {

                "Reminder: None"

            } else {

                "Reminder: ${habit.reminderTime}"
            }

        val goalDisplay =
            "${habit.goal} ${habit.unit}"
                .trim()

        tvGoal.text =
            if (
                goalDisplay.isBlank()
            ) {

                "Goal: None"

            } else {

                "Goal: $goalDisplay"
            }

        btnCompleteToday.text =
            if (
                habit.completed
            ) {

                "Completed Today ✓"

            } else {

                "Complete Today"
            }
    }

    // =========================================================
    // LOAD COMPLETION HISTORY
    // =========================================================

    private fun loadCompletionHistory(
        habit: Habit
    ) {

        val user =
            firebaseAuth.currentUser
                ?: return

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .document(habitId)
            .collection("completions")
            .get()
            .addOnSuccessListener { snapshot ->

                val completedDates =
                    snapshot.documents
                        .mapNotNull { document ->

                            document.getString(
                                "completionDate"
                            )
                        }
                        .toSet()

                updateRealStatistics(
                    habit,
                    completedDates
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Could not load progress: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_SHORT
                ).show()

                updateRealStatistics(
                    habit,
                    emptySet()
                )
            }
    }

    // =========================================================
    // REAL STATISTICS
    // =========================================================

    private fun updateRealStatistics(
        habit: Habit,
        completedDates: Set<String>
    ) {

        // =====================================================
        // ONLY RELEVANT SCHEDULED COMPLETIONS
        // =====================================================

        val relevantCompletedDates =
            getRelevantCompletedDates(
                habit,
                completedDates
            )

        val totalCompletedDays =
            relevantCompletedDates.size

        tvTotalDays.text =
            totalCompletedDays.toString()

        // =====================================================
        // FREQUENCY-AWARE CURRENT STREAK
        // =====================================================

        val currentStreak =
            calculateCurrentStreak(
                habit,
                relevantCompletedDates
            )

        tvCurrentStreak.text =
            currentStreak.toString()

        // =====================================================
        // FREQUENCY-AWARE BEST STREAK
        // =====================================================

        val bestStreak =
            calculateBestStreak(
                habit,
                relevantCompletedDates
            )

        tvBestStreak.text =
            bestStreak.toString()

        // =====================================================
        // COMPLETION RATE
        // =====================================================

        val expectedDays =
            calculateExpectedDays(
                habit
            )

        val relevantCompletedDays =
            relevantCompletedDates.size

        val completionPercentage =
            if (
                expectedDays <= 0
            ) {

                0

            } else {

                (
                        relevantCompletedDays
                            .toDouble() /
                                expectedDays *
                                100
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        100
                    )
            }

        tvCompletionPercentage.text =
            "$completionPercentage%"

        progressHabit.progress =
            completionPercentage

        // =====================================================
        // WEEKLY TRACKING
        // =====================================================

        updateWeeklyTracking(
            habit,
            completedDates
        )

        // =====================================================
        // PAST SEVEN DAYS
        // =====================================================

        updatePastSevenDays(
            habit,
            completedDates
        )

        // =====================================================
        // INSIGHT
        // =====================================================

        tvInsight.text =
            when {

                totalCompletedDays == 0 ->

                    "Complete this habit on its next scheduled day to start building your streak."

                currentStreak >= 7 ->

                    "Excellent consistency. You've completed $currentStreak scheduled sessions in a row."

                currentStreak >= 3 ->

                    "You're building momentum with $currentStreak scheduled completions in a row."

                currentStreak == 1 ->

                    "Your streak has started. Complete the habit again on its next scheduled day to extend it."

                else ->

                    "You have completed this habit $totalCompletedDays times. Keep following your schedule."
            }
    }

    // =========================================================
    // GET RELEVANT COMPLETION DATES
    //
    // Removes:
    // - completions before creation
    // - completions on non-scheduled days
    // - future completion documents
    // =========================================================

    private fun getRelevantCompletedDates(
        habit: Habit,
        completedDates: Set<String>
    ): Set<String> {

        if (
            habit.createdAt <= 0L
        ) {

            return emptySet()
        }

        val creationDay =
            dateFormat.format(
                Date(habit.createdAt)
            )

        val today =
            dateFormat.format(
                Date()
            )

        return completedDates
            .filter { dateString ->

                try {

                    val parsedDate =
                        dateFormat.parse(
                            dateString
                        ) ?: return@filter false

                    val calendar =
                        Calendar.getInstance().apply {

                            time =
                                parsedDate
                        }

                    dateString >= creationDay &&
                            dateString <= today &&
                            isScheduledDay(
                                habit,
                                calendar
                            )

                } catch (_: Exception) {

                    false
                }
            }
            .toSet()
    }

    // =========================================================
    // CURRENT STREAK
    //
    // IMPORTANT:
    // A streak now follows SCHEDULED days, not calendar days.
    //
    // Example:
    // Custom = Monday, Thursday, Saturday
    //
    // Completing Saturday and then Monday can continue the
    // streak because Sunday was never scheduled.
    // =========================================================

    private fun calculateCurrentStreak(
        habit: Habit,
        completedDates: Set<String>
    ): Int {

        if (
            completedDates.isEmpty()
        ) {

            return 0
        }

        val scheduledDates =
            getScheduledDatesFromCreation(
                habit
            )

        if (
            scheduledDates.isEmpty()
        ) {

            return 0
        }

        val today =
            getTodayCalendar()

        val todayString =
            dateFormat.format(
                today.time
            )

        val todayScheduled =
            isScheduledDay(
                habit,
                today
            )

        var startIndex =
            scheduledDates.lastIndex

        // =====================================================
        // If today is scheduled but not completed yet,
        // preserve the streak from the previous scheduled day.
        // =====================================================

        if (
            todayScheduled &&
            !completedDates.contains(
                todayString
            )
        ) {

            startIndex--
        }

        if (
            startIndex < 0
        ) {

            return 0
        }

        var streak =
            0

        for (
        index in startIndex downTo 0
        ) {

            val scheduledDate =
                scheduledDates[index]

            if (
                completedDates.contains(
                    scheduledDate
                )
            ) {

                streak++

            } else {

                break
            }
        }

        return streak
    }

    // =========================================================
    // BEST STREAK
    //
    // Consecutive scheduled sessions.
    // =========================================================

    private fun calculateBestStreak(
        habit: Habit,
        completedDates: Set<String>
    ): Int {

        if (
            completedDates.isEmpty()
        ) {

            return 0
        }

        val scheduledDates =
            getScheduledDatesFromCreation(
                habit
            )

        if (
            scheduledDates.isEmpty()
        ) {

            return 0
        }

        var best =
            0

        var current =
            0

        scheduledDates.forEach { scheduledDate ->

            if (
                completedDates.contains(
                    scheduledDate
                )
            ) {

                current++

                if (
                    current > best
                ) {

                    best =
                        current
                }

            } else {

                current =
                    0
            }
        }

        return best
    }

    // =========================================================
    // ALL SCHEDULED DATES FROM CREATION THROUGH TODAY
    // =========================================================

    private fun getScheduledDatesFromCreation(
        habit: Habit
    ): List<String> {

        if (
            habit.createdAt <= 0L
        ) {

            return emptyList()
        }

        val start =
            getCreationCalendar(
                habit
            )

        val today =
            getTodayCalendar()

        val dates =
            mutableListOf<String>()

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

                dates.add(
                    dateFormat.format(
                        cursor.time
                    )
                )
            }

            cursor.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return dates
    }

    // =========================================================
    // EXPECTED DAYS
    // =========================================================

    private fun calculateExpectedDays(
        habit: Habit
    ): Int {

        return getScheduledDatesFromCreation(
            habit
        ).size
    }

    // =========================================================
    // FREQUENCY CHECK
    //
    // DAILY    = every day
    // WEEKDAYS = Monday-Friday
    // CUSTOM   = selected customDays only
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
            // OLD / UNKNOWN DATA
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
    // CREATION DATE AT MIDNIGHT
    // =========================================================

    private fun getCreationCalendar(
        habit: Habit
    ): Calendar {

        return Calendar.getInstance().apply {

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
    }

    // =========================================================
    // TODAY AT MIDNIGHT
    // =========================================================

    private fun getTodayCalendar(): Calendar {

        return Calendar.getInstance().apply {

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
    }

    // =========================================================
    // WEEKLY TRACKING
    // =========================================================

    private fun updateWeeklyTracking(
        habit: Habit,
        completedDates: Set<String>
    ) {

        val dayViews =
            listOf(
                tvMonday,
                tvTuesday,
                tvWednesday,
                tvThursday,
                tvFriday,
                tvSaturday,
                tvSunday
            )

        val dayLabels =
            listOf(
                "M",
                "T",
                "W",
                "T",
                "F",
                "S",
                "S"
            )

        val monday =
            getTodayCalendar()

        monday.firstDayOfWeek =
            Calendar.MONDAY

        val currentDay =
            monday.get(
                Calendar.DAY_OF_WEEK
            )

        val daysFromMonday =
            if (
                currentDay ==
                Calendar.SUNDAY
            ) {

                6

            } else {

                currentDay -
                        Calendar.MONDAY
            }

        monday.add(
            Calendar.DAY_OF_YEAR,
            -daysFromMonday
        )

        val today =
            getTodayCalendar()

        val creation =
            getCreationCalendar(
                habit
            )

        for (
        index in 0..6
        ) {

            val calendar =
                monday.clone()
                        as Calendar

            calendar.add(
                Calendar.DAY_OF_YEAR,
                index
            )

            val date =
                dateFormat.format(
                    calendar.time
                )

            val completed =
                completedDates.contains(
                    date
                )

            val scheduled =
                isScheduledDay(
                    habit,
                    calendar
                )

            val beforeCreation =
                calendar.before(
                    creation
                )

            val future =
                calendar.after(
                    today
                )

            val symbol =
                when {

                    beforeCreation ->
                        "–"

                    !scheduled ->
                        "–"

                    completed ->
                        "✓"

                    future ->
                        "○"

                    else ->
                        "○"
                }

            dayViews[index].text =
                "${dayLabels[index]}\n$symbol"

            dayViews[index]
                .setTextColor(

                    when {

                        beforeCreation ->
                            Color.parseColor(
                                "#D0D1D8"
                            )

                        !scheduled ->
                            Color.parseColor(
                                "#D0D1D8"
                            )

                        completed ->
                            Color.parseColor(
                                "#21A78D"
                            )

                        else ->
                            Color.parseColor(
                                "#B2B4BE"
                            )
                    }
                )
        }
    }

    // =========================================================
    // PAST SEVEN DAYS
    // =========================================================

    private fun updatePastSevenDays(
        habit: Habit,
        completedDates: Set<String>
    ) {

        pastSevenDaysContainer
            .removeAllViews()

        val today =
            getTodayCalendar()

        val creation =
            getCreationCalendar(
                habit
            )

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

            val date =
                dateFormat.format(
                    day.time
                )

            val completed =
                completedDates.contains(
                    date
                )

            val scheduled =
                isScheduledDay(
                    habit,
                    day
                )

            val beforeCreation =
                day.before(
                    creation
                )

            // =================================================
            // COLUMN
            // =================================================

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

            // =================================================
            // BAR
            // =================================================

            val bar =
                View(this)

            val barHeight =
                when {

                    beforeCreation ->
                        dp(8)

                    !scheduled ->
                        dp(8)

                    completed ->
                        dp(110)

                    else ->
                        dp(25)
                }

            val barParams =
                LinearLayout.LayoutParams(
                    dp(22),
                    barHeight
                )

            barParams.setMargins(
                dp(3),
                0,
                dp(3),
                dp(7)
            )

            bar.layoutParams =
                barParams

            bar.setBackgroundColor(

                when {

                    beforeCreation ->
                        Color.parseColor(
                            "#E5E4EC"
                        )

                    !scheduled ->
                        Color.parseColor(
                            "#E5E4EC"
                        )

                    completed ->
                        Color.parseColor(
                            "#5B4CF0"
                        )

                    else ->
                        Color.parseColor(
                            "#C8C4FA"
                        )
                }
            )

            // =================================================
            // DAY LABEL
            // =================================================

            val dayLabel =
                TextView(this)

            dayLabel.text =
                SimpleDateFormat(
                    "EEE",
                    Locale.getDefault()
                )
                    .format(
                        day.time
                    )
                    .take(1)

            dayLabel.textSize =
                10f

            dayLabel.gravity =
                Gravity.CENTER

            dayLabel.setTextColor(
                Color.parseColor(
                    "#858998"
                )
            )

            column.addView(
                bar
            )

            column.addView(
                dayLabel
            )

            pastSevenDaysContainer
                .addView(
                    column
                )
        }
    }

    // =========================================================
    // COMPLETE TODAY
    // =========================================================

    private fun markCompleted() {

        val user =
            firebaseAuth.currentUser
                ?: return

        val habit =
            currentHabit
                ?: return

        // =====================================================
        // DON'T COMPLETE A CUSTOM/WEEKDAY HABIT ON AN
        // UNSCHEDULED DAY
        // =====================================================

        val todayCalendar =
            getTodayCalendar()

        if (
            !isScheduledDay(
                habit,
                todayCalendar
            )
        ) {

            Toast.makeText(
                this,
                "This habit is not scheduled for today.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        btnCompleteToday.isEnabled =
            false

        val today =
            dateFormat.format(
                Date()
            )

        val habitRef =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habitId)

        val completionRef =
            habitRef
                .collection("completions")
                .document(today)

        val completion =
            HabitCompletion(
                id = today,
                habitId = habit.id,
                habitName = habit.name,
                category = habit.category,
                completionDate = today,
                completedAt =
                    System.currentTimeMillis()
            )

        val batch =
            firestore.batch()

        batch.set(
            completionRef,
            completion
        )

        batch.update(
            habitRef,
            mapOf(
                "completed" to true,
                "paused" to false
            )
        )

        batch
            .commit()
            .addOnSuccessListener {

                btnCompleteToday.isEnabled =
                    true

                currentHabit?.completed =
                    true

                currentHabit?.paused =
                    false

                Toast.makeText(
                    this,
                    "Habit completed for today! ✓",
                    Toast.LENGTH_SHORT
                ).show()

                loadHabit()
            }
            .addOnFailureListener { exception ->

                btnCompleteToday.isEnabled =
                    true

                Toast.makeText(
                    this,
                    "Could not complete habit: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // UNDO TODAY
    // =========================================================

    private fun undoCompletion() {

        val user =
            firebaseAuth.currentUser
                ?: return

        btnCompleteToday.isEnabled =
            false

        val today =
            dateFormat.format(
                Date()
            )

        val habitRef =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habitId)

        val completionRef =
            habitRef
                .collection("completions")
                .document(today)

        val batch =
            firestore.batch()

        batch.delete(
            completionRef
        )

        batch.update(
            habitRef,
            mapOf(
                "completed" to false,
                "paused" to false
            )
        )

        batch
            .commit()
            .addOnSuccessListener {

                btnCompleteToday.isEnabled =
                    true

                currentHabit?.completed =
                    false

                Toast.makeText(
                    this,
                    "Completion undone.",
                    Toast.LENGTH_SHORT
                ).show()

                loadHabit()
            }
            .addOnFailureListener { exception ->

                btnCompleteToday.isEnabled =
                    true

                Toast.makeText(
                    this,
                    "Could not undo completion: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
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