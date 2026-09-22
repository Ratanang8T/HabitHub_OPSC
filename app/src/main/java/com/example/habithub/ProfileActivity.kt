package com.example.habithub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView

    private lateinit var tvTracked: TextView
    private lateinit var tvProfileStreak: TextView
    private lateinit var tvProfileCompleted: TextView

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
            R.layout.activity_profile
        )

        firebaseAuth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        // =====================================================
        // PROFILE VIEWS
        // =====================================================

        tvAvatar =
            findViewById(
                R.id.tvAvatar
            )

        tvProfileName =
            findViewById(
                R.id.tvProfileName
            )

        tvProfileEmail =
            findViewById(
                R.id.tvProfileEmail
            )

        tvTracked =
            findViewById(
                R.id.tvTracked
            )

        tvProfileStreak =
            findViewById(
                R.id.tvProfileStreak
            )

        tvProfileCompleted =
            findViewById(
                R.id.tvProfileCompleted
            )

        // =====================================================
        // BUTTONS
        // =====================================================

        val btnEditProfile =
            findViewById<TextView>(
                R.id.btnEditProfile
            )

        val btnNotifications =
            findViewById<TextView>(
                R.id.btnNotifications
            )

        val btnPrivacy =
            findViewById<TextView>(
                R.id.btnPrivacy
            )

        val btnReminders =
            findViewById<TextView>(
                R.id.btnReminders
            )

        val btnHelpCenter =
            findViewById<TextView>(
                R.id.btnHelpCenter
            )

        val btnRateApp =
            findViewById<TextView>(
                R.id.btnRateApp
            )

        val btnAbout =
            findViewById<TextView>(
                R.id.btnAbout
            )

        val btnAchievements =
            findViewById<Button>(
                R.id.btnAchievements
            )

        val btnLogout =
            findViewById<Button>(
                R.id.btnLogout
            )

        // =====================================================
        // LOAD PROFILE
        // =====================================================

        loadProfile()

        loadProfileStatistics()

        // =====================================================
        // EDIT PROFILE
        // =====================================================

        btnEditProfile.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }

        // =====================================================
        // NOTIFICATIONS
        // =====================================================

        btnNotifications.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }

        // =====================================================
        // REMINDERS
        // =====================================================

        btnReminders.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }

        // =====================================================
        // PRIVACY
        // =====================================================

        btnPrivacy.setOnClickListener {

            Toast.makeText(
                this,
                "Privacy & Security will be connected later.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // HELP
        // =====================================================

        btnHelpCenter.setOnClickListener {

            Toast.makeText(
                this,
                "HabitHub Help Center",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // RATE
        // =====================================================

        btnRateApp.setOnClickListener {

            Toast.makeText(
                this,
                "Thank you for supporting HabitHub!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // ABOUT
        // =====================================================

        btnAbout.setOnClickListener {

            AlertDialog
                .Builder(this)
                .setTitle(
                    "About HabitHub"
                )
                .setMessage(
                    "HabitHub helps students build consistent habits, track progress and achieve academic, personal and wellness goals."
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .show()
        }

        // =====================================================
        // ACHIEVEMENTS
        // =====================================================

        btnAchievements.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AchievementsActivity::class.java
                )
            )
        }

        // =====================================================
        // LOGOUT
        // =====================================================

        btnLogout.setOnClickListener {

            AlertDialog
                .Builder(this)
                .setTitle(
                    "Log Out"
                )
                .setMessage(
                    "Are you sure you want to log out?"
                )
                .setPositiveButton(
                    "Log Out"
                ) { _, _ ->

                    performLogout()
                }
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .show()
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        BottomNavHelper.setup(
            this,
            "PROFILE"
        )
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (
            ::firebaseAuth.isInitialized &&
            ::tvProfileName.isInitialized
        ) {

            loadProfile()

            loadProfileStatistics()
        }
    }

    // =========================================================
    // LOAD REAL PROFILE INFORMATION
    // =========================================================

    private fun loadProfile() {

        val user =
            firebaseAuth.currentUser

        if (
            user == null
        ) {

            tvProfileName.text =
                "Student"

            tvProfileEmail.text =
                "Not signed in"

            tvAvatar.text =
                "S"

            return
        }

        // =====================================================
        // EMAIL FROM FIREBASE AUTH
        // =====================================================

        val firebaseEmail =
            user.email ?: ""

        tvProfileEmail.text =
            if (
                firebaseEmail.isNotBlank()
            ) {

                firebaseEmail

            } else {

                "No email available"
            }

        // =====================================================
        // NAME FROM FIRESTORE
        // =====================================================

        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                var fullName =
                    document.getString(
                        "fullName"
                    ) ?: ""

                // =============================================
                // FALLBACK TO LOCAL PROFILE
                // =============================================

                if (
                    fullName.isBlank()
                ) {

                    val preferences =
                        getSharedPreferences(
                            "UserAccount",
                            MODE_PRIVATE
                        )

                    fullName =
                        preferences.getString(
                            "FULL_NAME",
                            "Student"
                        ) ?: "Student"
                }

                tvProfileName.text =
                    fullName

                updateAvatar(
                    fullName
                )
            }
            .addOnFailureListener {

                val preferences =
                    getSharedPreferences(
                        "UserAccount",
                        MODE_PRIVATE
                    )

                val fullName =
                    preferences.getString(
                        "FULL_NAME",
                        "Student"
                    ) ?: "Student"

                tvProfileName.text =
                    fullName

                updateAvatar(
                    fullName
                )
            }
    }

    // =========================================================
    // AVATAR
    // =========================================================

    private fun updateAvatar(
        fullName: String
    ) {

        val cleanName =
            fullName.trim()

        tvAvatar.text =
            if (
                cleanName.isBlank()
            ) {

                "S"

            } else {

                cleanName
                    .first()
                    .uppercase()
            }
    }

    // =========================================================
    // LOAD PROFILE STATISTICS
    // =========================================================

    private fun loadProfileStatistics() {

        val user =
            firebaseAuth.currentUser

        if (
            user == null
        ) {

            resetStatistics()

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

                            if (
                                habit != null
                            ) {

                                habit.id =
                                    document.id
                            }

                            habit
                        }

                tvTracked.text =
                    habits.size.toString()

                if (
                    habits.isEmpty()
                ) {

                    tvProfileStreak.text =
                        "0"

                    tvProfileCompleted.text =
                        "0%"

                    return@addOnSuccessListener
                }

                loadCompletionHistories(
                    habits
                )
            }
            .addOnFailureListener { exception ->

                resetStatistics()

                Toast.makeText(
                    this,
                    "Could not load profile statistics: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =========================================================
    // LOAD COMPLETION HISTORY
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

                        calculateProfileStatistics(
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

                        calculateProfileStatistics(
                            habits,
                            completionDatesByHabit
                        )
                    }
                }
        }
    }

    // =========================================================
    // CALCULATE PROFILE STATISTICS
    // =========================================================

    private fun calculateProfileStatistics(
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
        // TRACKED HABITS
        // =====================================================

        tvTracked.text =
            habits.size.toString()

        // =====================================================
        // OVERALL COMPLETION RATE
        // =====================================================

        var totalExpected =
            0

        var totalCompleted =
            0

        habits.forEach { habit ->

            totalExpected +=
                calculateExpectedDays(
                    habit
                )

            totalCompleted +=
                countRelevantCompletions(
                    habit,
                    completionDatesByHabit[
                        habit.id
                    ] ?: emptySet()
                )
        }

        val completionPercentage =
            if (
                totalExpected <= 0
            ) {

                0

            } else {

                (
                        totalCompleted
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

        tvProfileCompleted.text =
            "$completionPercentage%"

        // =====================================================
        // OVERALL PROFILE STREAK
        //
        // A profile day counts when:
        // - at least one habit was scheduled that day
        // - at least one scheduled habit was completed that day
        //
        // Days where NO habit was scheduled are skipped.
        // =====================================================

        val profileStreak =
            calculateProfileStreak(
                habits,
                completionDatesByHabit
            )

        tvProfileStreak.text =
            profileStreak.toString()
    }

    // =========================================================
    // PROFILE STREAK
    //
    // Unlike the old calendar-day streak, this skips days where
    // the user had absolutely no habits scheduled.
    // =========================================================

    private fun calculateProfileStreak(
        habits: List<Habit>,
        completionDatesByHabit:
        Map<String, Set<String>>
    ): Int {

        if (
            habits.isEmpty()
        ) {

            return 0
        }

        val earliestCreatedAt =
            habits
                .mapNotNull { habit ->

                    if (
                        habit.createdAt > 0L
                    ) {

                        habit.createdAt

                    } else {

                        null
                    }
                }
                .minOrNull()
                ?: return 0

        val earliestDate =
            Calendar.getInstance().apply {

                timeInMillis =
                    earliestCreatedAt

                resetToMidnight(
                    this
                )
            }

        val today =
            Calendar.getInstance().apply {

                resetToMidnight(
                    this
                )
            }

        // =====================================================
        // BUILD ALL DAYS WHERE AT LEAST ONE HABIT WAS SCHEDULED
        // =====================================================

        val scheduledProfileDates =
            mutableListOf<String>()

        val cursor =
            earliestDate.clone()
                    as Calendar

        while (
            !cursor.after(
                today
            )
        ) {

            val scheduledHabits =
                habits.filter { habit ->

                    isHabitActiveOnDate(
                        habit,
                        cursor
                    ) &&
                            isScheduledDay(
                                habit,
                                cursor
                            )
                }

            if (
                scheduledHabits.isNotEmpty()
            ) {

                scheduledProfileDates.add(
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

        if (
            scheduledProfileDates.isEmpty()
        ) {

            return 0
        }

        // =====================================================
        // DID USER COMPLETE AT LEAST ONE SCHEDULED HABIT?
        // =====================================================

        fun hasCompletionOnDate(
            dateString: String
        ): Boolean {

            val parsed =
                try {

                    dateFormat.parse(
                        dateString
                    )

                } catch (_: Exception) {

                    null
                } ?: return false

            val calendar =
                Calendar.getInstance().apply {

                    time =
                        parsed

                    resetToMidnight(
                        this
                    )
                }

            return habits.any { habit ->

                if (
                    !isHabitActiveOnDate(
                        habit,
                        calendar
                    )
                ) {

                    return@any false
                }

                if (
                    !isScheduledDay(
                        habit,
                        calendar
                    )
                ) {

                    return@any false
                }

                val dates =
                    completionDatesByHabit[
                        habit.id
                    ] ?: emptySet()

                dates.contains(
                    dateString
                )
            }
        }

        // =====================================================
        // TODAY SHOULD NOT DESTROY A STREAK BEFORE THE USER
        // HAS HAD A CHANCE TO COMPLETE TODAY.
        // =====================================================

        var index =
            scheduledProfileDates.lastIndex

        val todayString =
            dateFormat.format(
                today.time
            )

        if (
            scheduledProfileDates[index] ==
            todayString &&
            !hasCompletionOnDate(
                todayString
            )
        ) {

            index--
        }

        if (
            index < 0
        ) {

            return 0
        }

        var streak =
            0

        while (
            index >= 0
        ) {

            val dateString =
                scheduledProfileDates[index]

            if (
                hasCompletionOnDate(
                    dateString
                )
            ) {

                streak++

                index--

            } else {

                break
            }
        }

        return streak
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

                resetToMidnight(
                    this
                )
            }

        val today =
            Calendar.getInstance().apply {

                resetToMidnight(
                    this
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
    // COUNT REAL RELEVANT COMPLETIONS
    // =========================================================

    private fun countRelevantCompletions(
        habit: Habit,
        completionDates: Set<String>
    ): Int {

        if (
            habit.createdAt <= 0L
        ) {

            return 0
        }

        val createdDate =
            Calendar.getInstance().apply {

                timeInMillis =
                    habit.createdAt

                resetToMidnight(
                    this
                )
            }

        val today =
            Calendar.getInstance().apply {

                resetToMidnight(
                    this
                )
            }

        var count =
            0

        completionDates.forEach { dateString ->

            try {

                val parsedDate =
                    dateFormat.parse(
                        dateString
                    ) ?: return@forEach

                val completionCalendar =
                    Calendar.getInstance().apply {

                        time =
                            parsedDate

                        resetToMidnight(
                            this
                        )
                    }

                // =============================================
                // BEFORE CREATION
                // =============================================

                if (
                    completionCalendar.before(
                        createdDate
                    )
                ) {

                    return@forEach
                }

                // =============================================
                // FUTURE DOCUMENT
                // =============================================

                if (
                    completionCalendar.after(
                        today
                    )
                ) {

                    return@forEach
                }

                // =============================================
                // ONLY SCHEDULED COMPLETIONS
                // =============================================

                if (
                    isScheduledDay(
                        habit,
                        completionCalendar
                    )
                ) {

                    count++
                }

            } catch (_: Exception) {

                // Ignore invalid date.
            }
        }

        return count
    }

    // =========================================================
    // HABIT EXISTED ON DATE?
    // =========================================================

    private fun isHabitActiveOnDate(
        habit: Habit,
        calendar: Calendar
    ): Boolean {

        if (
            habit.createdAt <= 0L
        ) {

            return false
        }

        val creationDate =
            Calendar.getInstance().apply {

                timeInMillis =
                    habit.createdAt

                resetToMidnight(
                    this
                )
            }

        return !calendar.before(
            creationDate
        )
    }

    // =========================================================
    // FREQUENCY
    //
    // DAILY
    // → Every day
    //
    // WEEKDAYS
    // → Monday-Friday
    //
    // CUSTOM
    // → Selected customDays only
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
    // RESET CALENDAR TO MIDNIGHT
    // =========================================================

    private fun resetToMidnight(
        calendar: Calendar
    ) {

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )
    }

    // =========================================================
    // RESET STATISTICS
    // =========================================================

    private fun resetStatistics() {

        if (
            !::tvTracked.isInitialized
        ) {

            return
        }

        tvTracked.text =
            "0"

        tvProfileStreak.text =
            "0"

        tvProfileCompleted.text =
            "0%"
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private fun performLogout() {

        // =====================================================
        // FIREBASE SIGN OUT
        // =====================================================

        firebaseAuth.signOut()

        // =====================================================
        // CLEAR LOCAL SESSION
        // =====================================================

        getSharedPreferences(
            "UserAccount",
            MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()

        // =====================================================
        // RETURN TO LOGIN
        // =====================================================

        val intent =
            Intent(
                this,
                AuthActivity::class.java
            )

        intent.putExtra(
            "MODE",
            "LOGIN"
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(
            intent
        )

        finish()
    }
}