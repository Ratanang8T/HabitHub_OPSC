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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class HabitsActivity : AppCompatActivity() {

    private lateinit var habitsContainer: LinearLayout

    private lateinit var btnAll: Button
    private lateinit var btnActive: Button
    private lateinit var btnCompleted: Button
    private lateinit var btnPaused: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tvNetworkStatus: TextView
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                runOnUiThread {
                    showOnlineStatus()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                runOnUiThread {
                    if (!isInternetAvailable()) {
                        showOfflineStatus()
                    }
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(
                    network,
                    networkCapabilities
                )

                runOnUiThread {
                    if (isInternetAvailable()) {
                        showOnlineStatus()
                    } else {
                        showOfflineStatus()
                    }
                }
            }
        }

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

        tvNetworkStatus =
            findViewById(
                R.id.tvNetworkStatus
            )

        connectivityManager =
            getSystemService(
                CONNECTIVITY_SERVICE
            ) as ConnectivityManager

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
        //
        // These internal values deliberately stay in English.
        // They are program logic, not visible translated text.
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

        connectivityManager.registerDefaultNetworkCallback(
            networkCallback
        )

        if (isInternetAvailable()) {
            showOnlineStatus()
        } else {
            showOfflineStatus()
        }

        if (
            ::habitsContainer.isInitialized &&
            ::firebaseAuth.isInitialized &&
            ::firestore.isInitialized
        ) {

            loadHabits()
        }
    }

    override fun onPause() {

        try {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        } catch (_: Exception) {
            // Network callback was already removed.
        }

        super.onPause()
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
                getString(
                    R.string.please_sign_in_again
                ),
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
            getString(
                R.string.loading_habits
            )

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
                    getString(
                        R.string.could_not_load_habits,
                        exception.localizedMessage
                            ?: "Unknown error"
                    ),
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
                    getString(
                        R.string.paused
                    )

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
                    getString(
                        R.string.completed
                    )

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
                    getString(
                        R.string.active
                    )

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

                getString(
                    R.string.no_reminder
                )

            } else {

                getString(
                    R.string.reminder_time,
                    habit.reminderTime
                )
            }

        /*
         * frequency/customDays are saved application data.
         * We leave those stored values unchanged so existing
         * habit scheduling continues to work correctly.
         */
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

                getString(
                    R.string.no_goal_set
                )

            } else {

                "${habit.goal} ${habit.unit}"
                    .trim()
            }

        goalText.text =
            getString(
                R.string.goal_label,
                goalDisplay
            )

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
            getString(
                R.string.calculating_streak
            )

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

                            getString(
                                R.string.day
                            )

                        } else {

                            getString(
                                R.string.days
                            )
                        }

                    statistics.text =
                        when {

                            habit.paused ->

                                getString(
                                    R.string.streak_habit_paused,
                                    streak,
                                    dayText
                                )

                            habit.completed ->

                                getString(
                                    R.string.streak_completed_today,
                                    streak,
                                    dayText
                                )

                            else ->

                                getString(
                                    R.string.streak_ready_today,
                                    streak,
                                    dayText
                                )
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

                                getString(
                                    R.string.habit_currently_paused
                                )

                            habit.completed ->

                                getString(
                                    R.string.completed_today
                                )

                            else ->

                                getString(
                                    R.string.ready_for_today
                                )
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

                getString(
                    R.string.undo
                )

            } else {

                getString(
                    R.string.complete
                )
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

                getString(
                    R.string.resume
                )

            } else {

                getString(
                    R.string.pause
                )
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
            getString(
                R.string.edit
            )

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
            getString(
                R.string.delete
            )

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
                    getString(
                        R.string.could_not_open_habit
                    ),
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
                    getString(
                        R.string.delete_habit
                    )
                )
                .setMessage(
                    getString(
                        R.string.delete_habit_message,
                        habit.name
                    )
                )
                .setPositiveButton(
                    getString(
                        R.string.delete
                    )
                ) { _, _ ->

                    deleteHabit(
                        habit.id
                    )
                }
                .setNegativeButton(
                    getString(
                        R.string.cancel
                    ),
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
                getString(
                    R.string.please_sign_in_again
                ),
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

            // Update the habit locally.
            // Firestore queues the write when offline.
            habit.completed =
                false

            batch
                .commit()
                .addOnSuccessListener {

                    if (
                        isInternetAvailable()
                    ) {

                        Toast.makeText(
                            this,
                            getString(
                                R.string.completion_undone
                            ),
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            getString(
                                R.string.completion_undone_offline
                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        showOfflineStatus()
                    }

                    loadHabits()
                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        this,
                        getString(
                            R.string.could_not_undo_completion,
                            exception.localizedMessage
                                ?: "Unknown error"
                        ),
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
            // Completing a habit must NOT automatically
            // unpause it. Pausing is a separate user choice.
            // =================================================

            batch.update(
                habitRef,
                "completed",
                true
            )

            // Update the habit locally.
            // Firestore queues the write when offline.
            habit.completed =
                true

            batch
                .commit()
                .addOnSuccessListener {

                    if (
                        isInternetAvailable()
                    ) {

                        Toast.makeText(
                            this,
                            getString(
                                R.string.habit_completed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            getString(
                                R.string.habit_completed_offline
                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        showOfflineStatus()
                    }

                    loadHabits()
                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        this,
                        getString(
                            R.string.could_not_save_completion,
                            exception.localizedMessage
                                ?: "Unknown error"
                        ),
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
                getString(
                    R.string.please_sign_in_again
                ),
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
                        getString(
                            R.string.habit_paused
                        ),
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
                                getString(
                                    R.string.habit_resumed
                                ),
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
                                    getString(
                                        R.string.habit_resumed
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()

                                loadHabits()
                            }
                            .addOnFailureListener {

                                Toast.makeText(
                                    this,
                                    getString(
                                        R.string.habit_resumed_reminder_failed
                                    ),
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
                            getString(
                                R.string.habit_resumed_settings_failed
                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        loadHabits()
                    }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    getString(
                        R.string.could_not_update_habit,
                        exception.localizedMessage
                            ?: "Unknown error"
                    ),
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
                getString(
                    R.string.please_sign_in_again
                ),
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
                            getString(
                                R.string.habit_deleted
                            ),
                            Toast.LENGTH_SHORT
                        ).show()

                        loadHabits()
                    }
                    .addOnFailureListener { exception ->

                        Toast.makeText(
                            this,
                            getString(
                                R.string.could_not_delete_habit,
                                exception.localizedMessage
                                    ?: "Unknown error"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    getString(
                        R.string.could_not_delete_habit_history,
                        exception.localizedMessage
                            ?: "Unknown error"
                    ),
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

                    getString(
                        R.string.no_active_habits
                    )

                "COMPLETED" ->

                    getString(
                        R.string.no_completed_habits
                    )

                "PAUSED" ->

                    getString(
                        R.string.no_paused_habits
                    )

                else ->

                    getString(
                        R.string.no_habits_yet
                    )
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
    // NETWORK STATUS
    // =========================================================

    private fun isInternetAvailable(): Boolean {

        val activeNetwork =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(
                activeNetwork
            ) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
    }

    private fun showOfflineStatus() {

        tvNetworkStatus.text =
            getString(
                R.string.offline_sync
            )

        tvNetworkStatus.setTextColor(
            Color.parseColor(
                "#795500"
            )
        )

        tvNetworkStatus.setBackgroundColor(
            Color.parseColor(
                "#FFF3CD"
            )
        )

        tvNetworkStatus.visibility =
            android.view.View.VISIBLE
    }

    private fun showOnlineStatus() {

        tvNetworkStatus.visibility =
            android.view.View.GONE
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