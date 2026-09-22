package com.example.habithub

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // =========================================================
    // NOTIFICATION PERMISSION
    // =========================================================

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                Toast.makeText(
                    this,
                    "Habit reminders enabled 🔔",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Notification permission is needed for habit reminders.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // =========================================================
    // EDIT MODE
    // =========================================================

    private var habitId: String? = null

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var etHabitName: EditText
    private lateinit var spinnerCategory: Spinner

    private lateinit var radioFrequency: RadioGroup
    private lateinit var rbDaily: RadioButton
    private lateinit var rbWeekdays: RadioButton
    private lateinit var rbCustom: RadioButton

    private lateinit var customDaysSection: LinearLayout

    private lateinit var cbMonday: CheckBox
    private lateinit var cbTuesday: CheckBox
    private lateinit var cbWednesday: CheckBox
    private lateinit var cbThursday: CheckBox
    private lateinit var cbFriday: CheckBox
    private lateinit var cbSaturday: CheckBox
    private lateinit var cbSunday: CheckBox

    private lateinit var etReminderTime: EditText
    private lateinit var etGoal: EditText
    private lateinit var spinnerUnit: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSaveHabit: Button

    // =========================================================
    // OPTIONS
    // =========================================================

    private val categories =
        arrayOf(
            "Academic",
            "Fitness",
            "Health",
            "Wellness",
            "Career",
            "Personal Development"
        )

    private val units =
        arrayOf(
            "Minutes",
            "Hours",
            "Pages",
            "Sessions",
            "Times"
        )

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_create_habit
        )

        // =====================================================
        // NOTIFICATION PERMISSION
        // =====================================================

        requestNotificationPermission()

        // =====================================================
        // FIREBASE
        // =====================================================

        firebaseAuth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        // =====================================================
        // CONNECT XML
        // =====================================================

        val tvBack =
            findViewById<TextView>(
                R.id.tvBack
            )

        etHabitName =
            findViewById(
                R.id.etHabitName
            )

        spinnerCategory =
            findViewById(
                R.id.spinnerCategory
            )

        radioFrequency =
            findViewById(
                R.id.radioFrequency
            )

        rbDaily =
            findViewById(
                R.id.rbDaily
            )

        rbWeekdays =
            findViewById(
                R.id.rbWeekdays
            )

        rbCustom =
            findViewById(
                R.id.rbCustom
            )

        customDaysSection =
            findViewById(
                R.id.customDaysSection
            )

        cbMonday =
            findViewById(
                R.id.cbMonday
            )

        cbTuesday =
            findViewById(
                R.id.cbTuesday
            )

        cbWednesday =
            findViewById(
                R.id.cbWednesday
            )

        cbThursday =
            findViewById(
                R.id.cbThursday
            )

        cbFriday =
            findViewById(
                R.id.cbFriday
            )

        cbSaturday =
            findViewById(
                R.id.cbSaturday
            )

        cbSunday =
            findViewById(
                R.id.cbSunday
            )

        etReminderTime =
            findViewById(
                R.id.etReminderTime
            )

        etGoal =
            findViewById(
                R.id.etGoal
            )

        spinnerUnit =
            findViewById(
                R.id.spinnerUnit
            )

        etNotes =
            findViewById(
                R.id.etNotes
            )

        btnSaveHabit =
            findViewById(
                R.id.btnSaveHabit
            )

        // =====================================================
        // CATEGORY SPINNER
        // =====================================================

        val categoryAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                categories
            )

        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerCategory.adapter =
            categoryAdapter

        // =====================================================
        // UNIT SPINNER
        // =====================================================

        val unitAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                units
            )

        unitAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerUnit.adapter =
            unitAdapter

        // =====================================================
        // FREQUENCY LISTENER
        // =====================================================

        radioFrequency.setOnCheckedChangeListener { _, checkedId ->

            if (
                checkedId == R.id.rbCustom
            ) {

                customDaysSection.visibility =
                    View.VISIBLE

            } else {

                customDaysSection.visibility =
                    View.GONE
            }
        }

        // =====================================================
        // EDIT MODE
        // =====================================================

        habitId =
            intent.getStringExtra(
                "HABIT_ID"
            )

        if (
            !habitId.isNullOrBlank()
        ) {

            btnSaveHabit.text =
                "Update Habit"

            loadHabitForEditing()
        }

        // =====================================================
        // TIME PICKER
        // =====================================================

        etReminderTime.setOnClickListener {

            val calendar =
                Calendar.getInstance()

            val hour =
                calendar.get(
                    Calendar.HOUR_OF_DAY
                )

            val minute =
                calendar.get(
                    Calendar.MINUTE
                )

            val timePicker =
                TimePickerDialog(
                    this,
                    { _, selectedHour, selectedMinute ->

                        val time =
                            String.format(
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            )

                        etReminderTime.setText(
                            time
                        )
                    },
                    hour,
                    minute,
                    true
                )

            timePicker.show()
        }

        // =====================================================
        // BACK
        // =====================================================

        tvBack.setOnClickListener {

            finish()
        }

        // =====================================================
        // SAVE / UPDATE
        // =====================================================

        btnSaveHabit.setOnClickListener {

            validateAndSaveHabit()
        }
    }

    // =========================================================
    // NOTIFICATION PERMISSION
    // =========================================================

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    // =========================================================
    // GET SELECTED CUSTOM DAYS
    // =========================================================

    private fun getSelectedCustomDays(): List<String> {

        val selectedDays =
            mutableListOf<String>()

        if (cbMonday.isChecked) {
            selectedDays.add("Monday")
        }

        if (cbTuesday.isChecked) {
            selectedDays.add("Tuesday")
        }

        if (cbWednesday.isChecked) {
            selectedDays.add("Wednesday")
        }

        if (cbThursday.isChecked) {
            selectedDays.add("Thursday")
        }

        if (cbFriday.isChecked) {
            selectedDays.add("Friday")
        }

        if (cbSaturday.isChecked) {
            selectedDays.add("Saturday")
        }

        if (cbSunday.isChecked) {
            selectedDays.add("Sunday")
        }

        return selectedDays
    }

    // =========================================================
    // VALIDATE
    // =========================================================

    private fun validateAndSaveHabit() {

        val habitName =
            etHabitName.text
                .toString()
                .trim()

        val category =
            spinnerCategory
                .selectedItem
                .toString()

        val reminder =
            etReminderTime.text
                .toString()
                .trim()

        val goal =
            etGoal.text
                .toString()
                .trim()

        val unit =
            spinnerUnit
                .selectedItem
                .toString()

        val notes =
            etNotes.text
                .toString()
                .trim()

        // =====================================================
        // NAME VALIDATION
        // =====================================================

        if (
            habitName.isEmpty()
        ) {

            etHabitName.error =
                "Habit name is required"

            etHabitName.requestFocus()

            return
        }

        // =====================================================
        // GOAL VALIDATION
        // =====================================================

        if (
            goal.isEmpty()
        ) {

            etGoal.error =
                "Daily goal is required"

            etGoal.requestFocus()

            return
        }

        // =====================================================
        // FREQUENCY
        // =====================================================

        val selectedFrequency =
            when (
                radioFrequency.checkedRadioButtonId
            ) {

                R.id.rbWeekdays ->
                    "Weekdays"

                R.id.rbCustom ->
                    "Custom"

                else ->
                    "Daily"
            }

        // =====================================================
        // CUSTOM DAYS
        // =====================================================

        val selectedCustomDays =
            if (
                selectedFrequency == "Custom"
            ) {

                getSelectedCustomDays()

            } else {

                emptyList()
            }

        // =====================================================
        // CUSTOM VALIDATION
        // =====================================================

        if (
            selectedFrequency == "Custom" &&
            selectedCustomDays.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please select at least one day.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // =====================================================
        // CREATE OR UPDATE
        // =====================================================

        if (
            habitId.isNullOrBlank()
        ) {

            createHabit(
                name = habitName,
                category = category,
                reminder = reminder,
                goal = goal,
                unit = unit,
                notes = notes,
                frequency = selectedFrequency,
                customDays = selectedCustomDays
            )

        } else {

            updateHabit(
                name = habitName,
                category = category,
                reminder = reminder,
                goal = goal,
                unit = unit,
                notes = notes,
                frequency = selectedFrequency,
                customDays = selectedCustomDays
            )
        }
    }

    // =========================================================
    // CREATE HABIT
    // =========================================================

    private fun createHabit(
        name: String,
        category: String,
        reminder: String,
        goal: String,
        unit: String,
        notes: String,
        frequency: String,
        customDays: List<String>
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

        setLoading(
            true
        )

        val habitDocument =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document()

        val habit =
            Habit(
                id = habitDocument.id,
                name = name,
                category = category,
                reminderTime = reminder,
                goal = goal,
                unit = unit,
                notes = notes,
                frequency = frequency,
                customDays = customDays,
                completed = false,
                paused = false,
                createdAt =
                    System.currentTimeMillis()
            )

        // =====================================================
        // SAVE TO FIRESTORE
        // =====================================================

        habitDocument
            .set(
                habit
            )
            .addOnSuccessListener {

                // =============================================
                // APPLY REMINDER ONLY IF MASTER SETTING IS ON
                // =============================================

                applyReminderAfterSave(
                    habitId = habitDocument.id,
                    habitName = name,
                    reminder = reminder,
                    frequency = frequency,
                    customDays = customDays,
                    paused = false,
                    successMessage = "Habit created successfully"
                )
            }
            .addOnFailureListener { exception ->

                setLoading(
                    false
                )

                Toast.makeText(
                    this,
                    "Could not save habit: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // LOAD HABIT FOR EDITING
    // =========================================================

    private fun loadHabitForEditing() {

        val user =
            firebaseAuth.currentUser

        val currentHabitId =
            habitId

        if (
            user == null ||
            currentHabitId.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "Unable to load habit.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        setLoading(
            true
        )

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .document(currentHabitId)
            .get()
            .addOnSuccessListener { document ->

                setLoading(
                    false
                )

                if (
                    !document.exists()
                ) {

                    Toast.makeText(
                        this,
                        "Habit not found.",
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

                populateForm(
                    habit
                )
            }
            .addOnFailureListener { exception ->

                setLoading(
                    false
                )

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
    // POPULATE EDIT FORM
    // =========================================================

    private fun populateForm(
        habit: Habit
    ) {

        etHabitName.setText(
            habit.name
        )

        etReminderTime.setText(
            habit.reminderTime
        )

        etGoal.setText(
            habit.goal
        )

        etNotes.setText(
            habit.notes
        )

        // =====================================================
        // CATEGORY
        // =====================================================

        val categoryPosition =
            categories.indexOf(
                habit.category
            )

        if (
            categoryPosition >= 0
        ) {

            spinnerCategory.setSelection(
                categoryPosition
            )
        }

        // =====================================================
        // UNIT
        // =====================================================

        val unitPosition =
            units.indexOf(
                habit.unit
            )

        if (
            unitPosition >= 0
        ) {

            spinnerUnit.setSelection(
                unitPosition
            )
        }

        // =====================================================
        // FREQUENCY
        // =====================================================

        when (
            habit.frequency
        ) {

            "Weekdays" -> {

                rbWeekdays.isChecked =
                    true
            }

            "Custom" -> {

                rbCustom.isChecked =
                    true

                customDaysSection.visibility =
                    View.VISIBLE

                restoreCustomDays(
                    habit.customDays
                )
            }

            else -> {

                rbDaily.isChecked =
                    true
            }
        }
    }

    // =========================================================
    // RESTORE CUSTOM DAYS
    // =========================================================

    private fun restoreCustomDays(
        customDays: List<String>
    ) {

        cbMonday.isChecked =
            customDays.contains(
                "Monday"
            )

        cbTuesday.isChecked =
            customDays.contains(
                "Tuesday"
            )

        cbWednesday.isChecked =
            customDays.contains(
                "Wednesday"
            )

        cbThursday.isChecked =
            customDays.contains(
                "Thursday"
            )

        cbFriday.isChecked =
            customDays.contains(
                "Friday"
            )

        cbSaturday.isChecked =
            customDays.contains(
                "Saturday"
            )

        cbSunday.isChecked =
            customDays.contains(
                "Sunday"
            )
    }

    // =========================================================
    // UPDATE HABIT
    // =========================================================

    private fun updateHabit(
        name: String,
        category: String,
        reminder: String,
        goal: String,
        unit: String,
        notes: String,
        frequency: String,
        customDays: List<String>
    ) {

        val user =
            firebaseAuth.currentUser

        val currentHabitId =
            habitId

        if (
            user == null ||
            currentHabitId.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "Unable to update habit.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        setLoading(
            true
        )

        // =====================================================
        // LOAD CURRENT HABIT
        //
        // We keep its paused state. Editing a paused habit
        // must not silently resume its reminders.
        // =====================================================

        val habitReference =
            firestore
                .collection("users")
                .document(user.uid)
                .collection("habits")
                .document(currentHabitId)

        habitReference
            .get()
            .addOnSuccessListener { currentDocument ->

                val currentlyPaused =
                    currentDocument.getBoolean(
                        "paused"
                    ) ?: false

                val updates =
                    hashMapOf<String, Any>(
                        "name" to name,
                        "category" to category,
                        "reminderTime" to reminder,
                        "goal" to goal,
                        "unit" to unit,
                        "notes" to notes,
                        "frequency" to frequency,
                        "customDays" to customDays
                    )

                habitReference
                    .update(
                        updates
                    )
                    .addOnSuccessListener {

                        // =====================================
                        // ALWAYS CANCEL OLD ALARMS FIRST
                        // =====================================

                        HabitReminderScheduler(
                            this
                        ).cancelReminder(
                            currentHabitId
                        )

                        // =====================================
                        // APPLY UPDATED REMINDER
                        // =====================================

                        applyReminderAfterSave(
                            habitId = currentHabitId,
                            habitName = name,
                            reminder = reminder,
                            frequency = frequency,
                            customDays = customDays,
                            paused = currentlyPaused,
                            successMessage = "Habit updated successfully"
                        )
                    }
                    .addOnFailureListener { exception ->

                        setLoading(
                            false
                        )

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
            .addOnFailureListener { exception ->

                setLoading(
                    false
                )

                Toast.makeText(
                    this,
                    "Could not load current habit state: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // APPLY REMINDER AFTER CREATE / EDIT
    //
    // This is the MASTER reminder check.
    //
    // Rules:
    //
    // 1. Old alarms are removed first.
    // 2. No reminder time = no alarm.
    // 3. Paused habit = no alarm.
    // 4. Master reminders OFF = no alarm.
    // 5. Master reminders ON = schedule according to frequency.
    // =========================================================

    private fun applyReminderAfterSave(
        habitId: String,
        habitName: String,
        reminder: String,
        frequency: String,
        customDays: List<String>,
        paused: Boolean,
        successMessage: String
    ) {

        val user =
            firebaseAuth.currentUser

        if (
            user == null
        ) {

            finishHabitSave(
                successMessage
            )

            return
        }

        val scheduler =
            HabitReminderScheduler(
                this
            )

        // =====================================================
        // REMOVE ANY OLD ALARMS
        // =====================================================

        scheduler.cancelReminder(
            habitId
        )

        // =====================================================
        // NO REMINDER OR HABIT PAUSED
        // =====================================================

        if (
            reminder.isBlank() ||
            paused
        ) {

            finishHabitSave(
                successMessage
            )

            return
        }

        // =====================================================
        // CHECK MASTER REMINDER SETTING
        // =====================================================

        firestore
            .collection("users")
            .document(user.uid)
            .collection("settings")
            .document("preferences")
            .get()
            .addOnSuccessListener { settingsDocument ->

                val remindersEnabled =
                    if (
                        settingsDocument.exists()
                    ) {

                        settingsDocument.getBoolean(
                            "remindersEnabled"
                        ) ?: true

                    } else {

                        // New users have reminders enabled
                        // by default, matching SettingsActivity.
                        true
                    }

                // =================================================
                // MASTER REMINDERS ON
                // =================================================

                if (
                    remindersEnabled
                ) {

                    scheduler.scheduleReminder(
                        habitId = habitId,
                        habitName = habitName,
                        reminderTime = reminder,
                        frequency = frequency,
                        customDays = customDays
                    )
                }

                // =================================================
                // MASTER OFF:
                // intentionally do nothing.
                // Habit is still saved successfully.
                // =================================================

                finishHabitSave(
                    successMessage
                )
            }
            .addOnFailureListener {

                // =================================================
                // SAFETY:
                //
                // The habit itself has already been saved.
                // If we cannot verify whether reminders are enabled,
                // we do NOT schedule an alarm.
                // =================================================

                finishHabitSave(
                    successMessage
                )
            }
    }

    // =========================================================
    // FINISH SUCCESSFUL SAVE
    // =========================================================

    private fun finishHabitSave(
        message: String
    ) {

        setLoading(
            false
        )

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun setLoading(
        loading: Boolean
    ) {

        btnSaveHabit.isEnabled =
            !loading

        btnSaveHabit.text =
            if (
                loading
            ) {

                if (
                    habitId.isNullOrBlank()
                ) {

                    "Saving..."

                } else {

                    "Updating..."
                }

            } else {

                if (
                    habitId.isNullOrBlank()
                ) {

                    "Save Habit"

                } else {

                    "Update Habit"
                }
            }
    }
}