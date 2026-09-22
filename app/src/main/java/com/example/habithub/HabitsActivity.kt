package com.example.habithub

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitsActivity : AppCompatActivity() {

    private lateinit var habitsContainer: LinearLayout

    private lateinit var btnAll: Button
    private lateinit var btnActive: Button
    private lateinit var btnCompleted: Button
    private lateinit var btnPaused: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // =========================================================
    // PROGRESS / STREAK REPOSITORY
    // =========================================================

    private val progressRepository =
        HabitProgressRepository()

    private var currentFilter =
        "ALL"

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_habits
        )

        // =====================================================
        // FIREBASE
        // =====================================================

        firebaseAuth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        // =====================================================
        // XML VIEWS
        // =====================================================

        val tvBack =
            findViewById<TextView>(
                R.id.tvBack
            )

        val btnCreateHabit =
            findViewById<Button>(
                R.id.btnCreateHabit
            )

        habitsContainer =
            findViewById(
                R.id.habitsContainer
            )

        btnAll =
            findViewById(
                R.id.btnAll
            )

        btnActive =
            findViewById(
                R.id.btnActive
            )

        btnCompleted =
            findViewById(
                R.id.btnCompleted
            )

        btnPaused =
            findViewById(
                R.id.btnPaused
            )

        // =====================================================
        // BACK
        // =====================================================

        tvBack.setOnClickListener {

            finish()
        }

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
        // FILTER BUTTONS
        // =====================================================

        btnAll.setOnClickListener {

            currentFilter =
                "ALL"

            updateFilterButtons()

            loadHabits()
        }

        btnActive.setOnClickListener {

            currentFilter =
                "ACTIVE"

            updateFilterButtons()

            loadHabits()
        }

        btnCompleted.setOnClickListener {

            currentFilter =
                "COMPLETED"

            updateFilterButtons()

            loadHabits()
        }

        btnPaused.setOnClickListener {

            currentFilter =
                "PAUSED"

            updateFilterButtons()

            loadHabits()
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        BottomNavHelper.setup(
            this,
            "HABITS"
        )

        updateFilterButtons()
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (
            ::habitsContainer.isInitialized &&
            ::firebaseAuth.isInitialized &&
            ::firestore.isInitialized
        ) {

            loadHabits()
        }
    }

    // =========================================================
    // LOAD HABITS
    //
    // Today's dated completion document is the source of truth.
    // =========================================================

    private fun loadHabits() {

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

            return
        }

        habitsContainer.removeAllViews()

        // =====================================================
        // LOADING MESSAGE
        // =====================================================

        val loadingText =
            TextView(this)

        loadingText.text =
            "Loading habits..."

        loadingText.gravity =
            Gravity.CENTER

        loadingText.textSize =
            14f

        loadingText.setTextColor(
            Color.parseColor(
                "#858998"
            )
        )

        loadingText.setPadding(
            dp(10),
            dp(45),
            dp(10),
            dp(45)
        )

        habitsContainer.addView(
            loadingText
        )

        val today =
            getTodayDate()

        // =====================================================
        // LOAD HABITS FROM FIRESTORE
        // =====================================================

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .get()
            .addOnSuccessListener { snapshot ->

                val allHabits =
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
                        .sortedByDescending {

                            it.createdAt
                        }

                if (
                    allHabits.isEmpty()
                ) {

                    habitsContainer.removeAllViews()

                    showEmptyMessage()

                    return@addOnSuccessListener
                }

                // =================================================
                // CHECK TODAY'S COMPLETION FOR EACH HABIT
                // =================================================

                var finishedChecks =
                    0

                allHabits.forEach { habit ->

                    firestore
                        .collection("users")
                        .document(user.uid)
                        .collection("habits")
                        .document(habit.id)
                        .collection("completions")
                        .document(today)
                        .get()
                        .addOnSuccessListener { completionDocument ->

                            habit.completed =
                                completionDocument.exists()

                            finishedChecks++

                            if (
                                finishedChecks ==
                                allHabits.size
                            ) {

                                displayHabits(
                                    allHabits
                                )
                            }
                        }
                        .addOnFailureListener {

                            habit.completed =
                                false

                            finishedChecks++

                            if (
                                finishedChecks ==
                                allHabits.size
                            ) {

                                displayHabits(
                                    allHabits
                                )
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->

                habitsContainer.removeAllViews()

                Toast.makeText(
                    this,
                    "Could not load habits: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()

                showEmptyMessage()
            }
    }

    // =========================================================
    // DISPLAY HABITS
    // =========================================================

    private fun displayHabits(
        allHabits: List<Habit>
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {

            return
        }

        habitsContainer.removeAllViews()

        val filteredHabits =
            when (
                currentFilter
            ) {

                "ACTIVE" ->

                    allHabits.filter {

                        !it.completed &&
                                !it.paused
                    }

                "COMPLETED" ->

                    allHabits.filter {

                        it.completed
                    }

                "PAUSED" ->

                    allHabits.filter {

                        it.paused
                    }

                else ->

                    allHabits
            }

        if (
            filteredHabits.isEmpty()
        ) {

            showEmptyMessage()

            return
        }

        filteredHabits.forEach { habit ->

            createHabitCard(
                habit
            )
        }
    }

    // =========================================================
    // CREATE HABIT CARD
    // =========================================================

    private fun createHabitCard(
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

        card.setBackgroundResource(
            R.drawable.bg_card
        )

        val cardParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        cardParams.setMargins(
            0,
            dp(8),
            0,
            dp(8)
        )

        card.layoutParams =
            cardParams

        // =====================================================
        // TOP ROW
        // =====================================================

        val topRow =
            LinearLayout(this)

        topRow.orientation =
            LinearLayout.HORIZONTAL

        topRow.gravity =
            Gravity.CENTER_VERTICAL

        val titleArea =
            LinearLayout(this)

        titleArea.orientation =
            LinearLayout.VERTICAL

        titleArea.layoutParams =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )

        // =====================================================
        // HABIT NAME
        // =====================================================

        val title =
            TextView(this)

        title.text =
            habit.name

        title.textSize =
            17f

        title.setTextColor(
            Color.parseColor(
                "#15182A"
            )
        )

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        // =====================================================
        // CATEGORY
        // =====================================================

        val categoryText =
            TextView(this)

        categoryText.text =
            habit.category

        categoryText.textSize =
            12f

        categoryText.setTextColor(
            Color.parseColor(
                "#5B4CF0"
            )
        )

        categoryText.setPadding(
            0,
            dp(5),
            0,
            0
        )

        titleArea.addView(
            title
        )

        titleArea.addView(
            categoryText
        )

        // =====================================================
        // STATUS
        // =====================================================

        val status =
            TextView(this)

        status.textSize =
            11f

        status.setTypeface(
            null,
            Typeface.BOLD
        )

        status.setPadding(
            dp(10),
            dp(6),
            dp(10),
            dp(6)
        )

        when {

            habit.paused -> {

                status.text =
                    "Paused"

                status.setTextColor(
                    Color.parseColor(
                        "#E28A24"
                    )
                )

                status.setBackgroundColor(
                    Color.parseColor(
                        "#FFF2DD"
                    )
                )
            }

            habit.completed -> {

                status.text =
                    "Completed"

                status.setTextColor(
                    Color.parseColor(
                        "#1B8C76"
                    )
                )

                status.setBackgroundColor(
                    Color.parseColor(
                        "#E5F7F1"
                    )
                )
            }

            else -> {

                status.text =
                    "Active"

                status.setTextColor(
                    Color.parseColor(
                        "#5B4CF0"
                    )
                )

                status.setBackgroundColor(
                    Color.parseColor(
                        "#EEEAFE"
                    )
                )
            }
        }

        topRow.addView(
            titleArea
        )

        topRow.addView(
            status
        )

        card.addView(
            topRow
        )

        // =====================================================
        // REMINDER + FREQUENCY
        // =====================================================

        val reminderText =
            if (
                habit.reminderTime.isBlank()
            ) {

                "No reminder"

            } else {

                "Reminder ${habit.reminderTime}"
            }

        val frequencyText =
            if (
                habit.frequency.equals(
                    "Custom",
                    ignoreCase = true
                ) &&
                habit.customDays.isNotEmpty()
            ) {

                habit.customDays.joinToString(
                    ", "
                )

            } else {

                habit.frequency
            }

        val details =
            TextView(this)

        details.text =
            "$reminderText • $frequencyText"

        details.textSize =
            12f

        details.setTextColor(
            Color.parseColor(
                "#858998"
            )
        )

        details.setPadding(
            0,
            dp(12),
            0,
            0
        )

        card.addView(
            details
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

        card.addView(
            goalText
        )

        // =====================================================
        // REAL STREAK
        // =====================================================

        val statistics =
            TextView(this)

        statistics.text =
            "Calculating streak..."

        statistics.textSize =
            12f

        statistics.setTextColor(
            Color.parseColor(
                "#555A6B"
            )
        )

        statistics.setPadding(
            0,
            dp(10),
            0,
            0
        )

        card.addView(
            statistics
        )

        progressRepository
            .getCurrentStreak(

                habitId =
                    habit.id,

                onSuccess = { streak ->

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {

                        return@getCurrentStreak
                    }

                    val dayText =
                        if (
                            streak == 1
                        ) {

                            "day"

                        } else {

                            "days"
                        }

                    statistics.text =
                        when {

                            habit.paused ->

                                "🔥 $streak $dayText streak • Habit paused"

                            habit.completed ->

                                "🔥 $streak $dayText streak • Completed today ✓"

                            else ->

                                "🔥 $streak $dayText streak • Ready for today"
                        }
                },

                onError = {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {

                        return@getCurrentStreak
                    }

                    statistics.text =
                        when {

                            habit.paused ->

                                "Habit currently paused"

                            habit.completed ->

                                "Completed today ✓"

                            else ->

                                "Ready for today"
                        }
                }
            )

        // =====================================================
        // ACTION ROW
        // =====================================================

        val actionRow =
            LinearLayout(this)

        actionRow.orientation =
            LinearLayout.HORIZONTAL

        actionRow.setPadding(
            0,
            dp(15),
            0,
            0
        )

        // =====================================================
        // COMPLETE BUTTON
        // =====================================================

        val btnComplete =
            Button(this)

        btnComplete.layoutParams =
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            )

        btnComplete.isAllCaps =
            false

        btnComplete.text =
            if (
                habit.completed
            ) {

                "Undo"

            } else {

                "Complete"
            }

        btnComplete.setTextColor(
            Color.WHITE
        )

        btnComplete.setBackgroundColor(

            if (
                habit.completed
            ) {

                Color.parseColor(
                    "#858998"
                )

            } else {

                Color.parseColor(
                    "#5B4CF0"
                )
            }
        )

        // =====================================================
        // PAUSE BUTTON
        // =====================================================

        val btnPause =
            Button(this)

        val pauseParams =
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            )

        pauseParams.setMargins(
            dp(8),
            0,
            0,
            0
        )

        btnPause.layoutParams =
            pauseParams

        btnPause.isAllCaps =
            false

        btnPause.text =
            if (
                habit.paused
            ) {

                "Resume"

            } else {

                "Pause"
            }

        btnPause.setTextColor(
            Color.parseColor(
                "#5B4CF0"
            )
        )

        actionRow.addView(
            btnComplete
        )

        actionRow.addView(
            btnPause
        )

        card.addView(
            actionRow
        )

        // =====================================================
        // SECONDARY ROW
        // =====================================================

        val secondaryRow =
            LinearLayout(this)

        secondaryRow.orientation =
            LinearLayout.HORIZONTAL

        secondaryRow.setPadding(
            0,
            dp(8),
            0,
            0
        )

        val btnEdit =
            Button(this)

        btnEdit.layoutParams =
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )

        btnEdit.text =
            "Edit"

        btnEdit.isAllCaps =
            false

        val btnDelete =
            Button(this)

        val deleteParams =
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )

        deleteParams.setMargins(
            dp(8),
            0,
            0,
            0
        )

        btnDelete.layoutParams =
            deleteParams

        btnDelete.text =
            "Delete"

        btnDelete.isAllCaps =
            false

        btnDelete.setTextColor(
            Color.parseColor(
                "#D14343"
            )
        )

        secondaryRow.addView(
            btnEdit
        )

        secondaryRow.addView(
            btnDelete
        )

        card.addView(
            secondaryRow
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

        // =====================================================
        // COMPLETE / UNDO
        // =====================================================

        btnComplete.setOnClickListener {

            updateHabitCompletion(
                habit
            )
        }

        // =====================================================
        // PAUSE / RESUME
        // =====================================================

        btnPause.setOnClickListener {

            updatePauseStatus(
                habitId =
                    habit.id,

                paused =
                    !habit.paused
            )
        }

        // =====================================================
        // EDIT
        // =====================================================

        btnEdit.setOnClickListener {

            val intent =
                Intent(
                    this,
                    CreateHabitActivity::class.java
                )

            intent.putExtra(
                "HABIT_ID",
                habit.id
            )

            startActivity(
                intent
            )
        }

        // =====================================================
        // DELETE
        // =====================================================

        btnDelete.setOnClickListener {

            AlertDialog
                .Builder(this)
                .setTitle(
                    "Delete Habit"
                )
                .setMessage(
                    "Delete \"${habit.name}\"?"
                )
                .setPositiveButton(
                    "Delete"
                ) { _, _ ->

                    deleteHabit(
                        habit.id
                    )
                }
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .show()
        }

        habitsContainer.addView(
            card
        )
    }

    // =========================================================
    // COMPLETE / UNDO
    // =========================================================

    private fun updateHabitCompletion(
        habit: Habit
    ) {

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

            return
        }

        val today =
            getTodayDate()

        val habitRef =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habit.id)

        val completionRef =
            habitRef
                .collection("completions")
                .document(today)

        val batch =
            firestore.batch()

        // =====================================================
        // UNDO TODAY
        // =====================================================

        if (
            habit.completed
        ) {

            batch.delete(
                completionRef
            )

            batch.update(
                habitRef,
                "completed",
                false
            )

            batch
                .commit()
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Completion undone",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHabits()
                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        this,
                        "Could not undo completion: ${
                            exception.localizedMessage
                                ?: "Unknown error"
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }

        } else {

            // =================================================
            // COMPLETE TODAY
            // =================================================

            val completion =
                HabitCompletion(
                    id =
                        today,

                    habitId =
                        habit.id,

                    habitName =
                        habit.name,

                    category =
                        habit.category,

                    completionDate =
                        today,

                    completedAt =
                        System.currentTimeMillis()
                )

            batch.set(
                completionRef,
                completion
            )

            // =================================================
            // IMPORTANT:
            //
            // Completing a habit should NOT automatically
            // unpause it. Pausing is a separate user choice.
            // =================================================

            batch.update(
                habitRef,
                "completed",
                true
            )

            batch
                .commit()
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Habit completed ✓",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHabits()
                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        this,
                        "Could not save completion: ${
                            exception.localizedMessage
                                ?: "Unknown error"
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    // =========================================================
    // PAUSE / RESUME
    //
    // PAUSE:
    // Cancel this habit's reminder alarms.
    //
    // RESUME:
    // Check master reminder setting before restoring alarms.
    // =========================================================

    private fun updatePauseStatus(
        habitId: String,
        paused: Boolean
    ) {

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

            return
        }

        val habitRef =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habitId)

        habitRef
            .update(
                "paused",
                paused
            )
            .addOnSuccessListener {

                val scheduler =
                    HabitReminderScheduler(
                        this
                    )

                // =================================================
                // PAUSED
                // =================================================

                if (
                    paused
                ) {

                    scheduler.cancelReminder(
                        habitId
                    )

                    Toast.makeText(
                        this,
                        "Habit paused",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHabits()

                    return@addOnSuccessListener
                }

                // =================================================
                // RESUMED
                //
                // Check the master reminder setting.
                // =================================================

                firestore
                    .collection("users")
                    .document(user.uid)
                    .collection("settings")
                    .document("preferences")
                    .get()
                    .addOnSuccessListener { settingsDocument ->

                        val remindersEnabled =
                            settingsDocument.getBoolean(
                                "remindersEnabled"
                            ) ?: true

                        // =========================================
                        // MASTER REMINDERS OFF
                        // =========================================

                        if (
                            !remindersEnabled
                        ) {

                            Toast.makeText(
                                this,
                                "Habit resumed",
                                Toast.LENGTH_SHORT
                            ).show()

                            loadHabits()

                            return@addOnSuccessListener
                        }

                        // =========================================
                        // MASTER REMINDERS ON
                        // =========================================

                        habitRef
                            .get()
                            .addOnSuccessListener { habitDocument ->

                                val habit =
                                    habitDocument.toObject(
                                        Habit::class.java
                                    )

                                if (
                                    habit != null &&
                                    habit.reminderTime.isNotBlank()
                                ) {

                                    habit.id =
                                        habitDocument.id

                                    scheduler.scheduleReminder(
                                        habitId =
                                            habit.id,

                                        habitName =
                                            habit.name,

                                        reminderTime =
                                            habit.reminderTime,

                                        frequency =
                                            habit.frequency,

                                        customDays =
                                            habit.customDays
                                    )
                                }

                                Toast.makeText(
                                    this,
                                    "Habit resumed",
                                    Toast.LENGTH_SHORT
                                ).show()

                                loadHabits()
                            }
                            .addOnFailureListener {

                                Toast.makeText(
                                    this,
                                    "Habit resumed, but its reminder could not be restored.",
                                    Toast.LENGTH_LONG
                                ).show()

                                loadHabits()
                            }
                    }
                    .addOnFailureListener {

                        // Safer behavior:
                        // do not schedule if the master setting
                        // could not be checked.

                        Toast.makeText(
                            this,
                            "Habit resumed, but reminder settings could not be checked.",
                            Toast.LENGTH_LONG
                        ).show()

                        loadHabits()
                    }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Could not update habit: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // DELETE HABIT
    // =========================================================

    private fun deleteHabit(
        habitId: String
    ) {

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

            return
        }

        // =====================================================
        // CANCEL REMINDER
        // =====================================================

        HabitReminderScheduler(
            this
        ).cancelReminder(
            habitId
        )

        val habitRef =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habitId)

        // =====================================================
        // DELETE COMPLETION HISTORY
        // =====================================================

        habitRef
            .collection("completions")
            .get()
            .addOnSuccessListener { completionSnapshot ->

                val batch =
                    firestore.batch()

                completionSnapshot.documents
                    .forEach { completionDocument ->

                        batch.delete(
                            completionDocument.reference
                        )
                    }

                // =================================================
                // DELETE HABIT DOCUMENT
                // =================================================

                batch.delete(
                    habitRef
                )

                batch
                    .commit()
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Habit deleted",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadHabits()
                    }
                    .addOnFailureListener { exception ->

                        Toast.makeText(
                            this,
                            "Could not delete habit: ${
                                exception.localizedMessage
                                    ?: "Unknown error"
                            }",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Could not delete habit history: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // EMPTY MESSAGE
    // =========================================================

    private fun showEmptyMessage() {

        val message =
            TextView(this)

        message.text =
            when (
                currentFilter
            ) {

                "ACTIVE" ->

                    "You don't have any active habits."

                "COMPLETED" ->

                    "You haven't completed any habits today."

                "PAUSED" ->

                    "You don't have any paused habits."

                else ->

                    "No habits yet. Create your first habit!"
            }

        message.gravity =
            Gravity.CENTER

        message.textSize =
            14f

        message.setTextColor(
            Color.parseColor(
                "#858998"
            )
        )

        message.setPadding(
            dp(10),
            dp(45),
            dp(10),
            dp(45)
        )

        habitsContainer.addView(
            message
        )
    }

    // =========================================================
    // FILTER BUTTON STYLING
    // =========================================================

    private fun updateFilterButtons() {

        styleFilterButton(
            btnAll,
            currentFilter == "ALL"
        )

        styleFilterButton(
            btnActive,
            currentFilter == "ACTIVE"
        )

        styleFilterButton(
            btnCompleted,
            currentFilter == "COMPLETED"
        )

        styleFilterButton(
            btnPaused,
            currentFilter == "PAUSED"
        )
    }

    private fun styleFilterButton(
        button: Button,
        selected: Boolean
    ) {

        if (
            selected
        ) {

            button.setBackgroundColor(
                Color.parseColor(
                    "#5B4CF0"
                )
            )

            button.setTextColor(
                Color.WHITE
            )

        } else {

            button.setBackgroundColor(
                Color.parseColor(
                    "#F0F1F6"
                )
            )

            button.setTextColor(
                Color.parseColor(
                    "#555A6B"
                )
            )
        }
    }

    // =========================================================
    // TODAY
    // =========================================================

    private fun getTodayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(
            Date()
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