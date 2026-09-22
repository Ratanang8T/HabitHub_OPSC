package com.example.habithub

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etDisplayName: EditText
    private lateinit var etEmail: EditText

    private lateinit var switchReminders: Switch
    private lateinit var switchMotivation: Switch
    private lateinit var switchWeeklyReport: Switch

    private lateinit var spinnerWeekStart: Spinner
    private lateinit var spinnerDefaultCategory: Spinner

    private lateinit var btnSaveSettings: Button

    private val weekOptions =
        arrayOf(
            "Monday",
            "Sunday"
        )

    private val categories =
        arrayOf(
            "Academic",
            "Fitness",
            "Health",
            "Wellness",
            "Career",
            "Personal Development"
        )

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_settings
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

        etDisplayName =
            findViewById(
                R.id.etDisplayName
            )

        etEmail =
            findViewById(
                R.id.etEmail
            )

        switchReminders =
            findViewById(
                R.id.switchReminders
            )

        switchMotivation =
            findViewById(
                R.id.switchMotivation
            )

        switchWeeklyReport =
            findViewById(
                R.id.switchWeeklyReport
            )

        spinnerWeekStart =
            findViewById(
                R.id.spinnerWeekStart
            )

        spinnerDefaultCategory =
            findViewById(
                R.id.spinnerDefaultCategory
            )

        btnSaveSettings =
            findViewById(
                R.id.btnSaveSettings
            )

        // =====================================================
        // SPINNER DATA
        // =====================================================

        val weekAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                weekOptions
            )

        weekAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerWeekStart.adapter =
            weekAdapter

        val categoryAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                categories
            )

        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerDefaultCategory.adapter =
            categoryAdapter

        // =====================================================
        // EMAIL
        // =====================================================

        etEmail.isEnabled =
            false

        etEmail.setText(
            firebaseAuth.currentUser?.email ?: ""
        )

        // =====================================================
        // LOAD SETTINGS
        // =====================================================

        loadSettings()

        // =====================================================
        // BACK
        // =====================================================

        tvBack.setOnClickListener {

            finish()
        }

        // =====================================================
        // SAVE
        // =====================================================

        btnSaveSettings.setOnClickListener {

            validateAndSaveSettings()
        }
    }

    // =========================================================
    // LOAD SETTINGS
    // =========================================================

    private fun loadSettings() {

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

        // =====================================================
        // DEFAULT VALUES
        // =====================================================

        switchReminders.isChecked =
            true

        switchMotivation.isChecked =
            true

        switchWeeklyReport.isChecked =
            true

        spinnerWeekStart.setSelection(
            0
        )

        spinnerDefaultCategory.setSelection(
            0
        )

        // =====================================================
        // LOAD USER PROFILE
        // =====================================================

        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                val fullName =
                    document.getString(
                        "fullName"
                    ) ?: "Student"

                etDisplayName.setText(
                    fullName
                )

                val email =
                    user.email
                        ?: document.getString(
                            "email"
                        )
                        ?: ""

                etEmail.setText(
                    email
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Could not load profile: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }

        // =====================================================
        // LOAD APP SETTINGS
        // =====================================================

        firestore
            .collection("users")
            .document(user.uid)
            .collection("settings")
            .document("preferences")
            .get()
            .addOnSuccessListener { document ->

                if (
                    !document.exists()
                ) {

                    return@addOnSuccessListener
                }

                val remindersEnabled =
                    document.getBoolean(
                        "remindersEnabled"
                    ) ?: true

                val motivationEnabled =
                    document.getBoolean(
                        "motivationEnabled"
                    ) ?: true

                val weeklyReportEnabled =
                    document.getBoolean(
                        "weeklyReportEnabled"
                    ) ?: true

                val weekStart =
                    document.getString(
                        "weekStart"
                    ) ?: "Monday"

                val defaultCategory =
                    document.getString(
                        "defaultCategory"
                    ) ?: "Academic"

                switchReminders.isChecked =
                    remindersEnabled

                switchMotivation.isChecked =
                    motivationEnabled

                switchWeeklyReport.isChecked =
                    weeklyReportEnabled

                val weekPosition =
                    weekOptions.indexOf(
                        weekStart
                    )

                if (
                    weekPosition >= 0
                ) {

                    spinnerWeekStart.setSelection(
                        weekPosition
                    )
                }

                val categoryPosition =
                    categories.indexOf(
                        defaultCategory
                    )

                if (
                    categoryPosition >= 0
                ) {

                    spinnerDefaultCategory.setSelection(
                        categoryPosition
                    )
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Could not load settings: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // VALIDATE
    // =========================================================

    private fun validateAndSaveSettings() {

        val newName =
            etDisplayName.text
                .toString()
                .trim()

        if (
            newName.isEmpty()
        ) {

            etDisplayName.error =
                "Display name is required"

            etDisplayName.requestFocus()

            return
        }

        saveSettings(
            newName
        )
    }

    // =========================================================
    // SAVE SETTINGS
    // =========================================================

    private fun saveSettings(
        newName: String
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

        btnSaveSettings.isEnabled =
            false

        btnSaveSettings.text =
            "Saving..."

        val remindersEnabled =
            switchReminders.isChecked

        // =====================================================
        // SETTINGS DATA
        // =====================================================

        val settingsData =
            hashMapOf<String, Any>(

                "remindersEnabled" to
                        remindersEnabled,

                "motivationEnabled" to
                        switchMotivation.isChecked,

                "weeklyReportEnabled" to
                        switchWeeklyReport.isChecked,

                "weekStart" to
                        spinnerWeekStart
                            .selectedItem
                            .toString(),

                "defaultCategory" to
                        spinnerDefaultCategory
                            .selectedItem
                            .toString()
            )

        // =====================================================
        // UPDATE DISPLAY NAME
        // =====================================================

        firestore
            .collection("users")
            .document(user.uid)
            .update(
                "fullName",
                newName
            )
            .addOnSuccessListener {

                // =================================================
                // SAVE SETTINGS DOCUMENT
                // =================================================

                firestore
                    .collection("users")
                    .document(user.uid)
                    .collection("settings")
                    .document("preferences")
                    .set(
                        settingsData
                    )
                    .addOnSuccessListener {

                        // =========================================
                        // LOCAL PROFILE
                        // =========================================

                        getSharedPreferences(
                            "UserAccount",
                            MODE_PRIVATE
                        )
                            .edit()
                            .putString(
                                "FULL_NAME",
                                newName
                            )
                            .putString(
                                "EMAIL",
                                user.email ?: ""
                            )
                            .apply()

                        // =========================================
                        // APPLY MASTER REMINDER SETTING
                        // =========================================

                        updateHabitReminders(
                            remindersEnabled
                        )
                    }
                    .addOnFailureListener { exception ->

                        restoreSaveButton()

                        Toast.makeText(
                            this,
                            "Could not save settings: ${
                                exception.localizedMessage
                                    ?: "Unknown error"
                            }",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->

                restoreSaveButton()

                Toast.makeText(
                    this,
                    "Could not update profile: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // APPLY MASTER REMINDER SETTING
    //
    // OFF
    // → cancel all habit alarms
    //
    // ON
    // → reload habits and schedule their reminders
    // =========================================================

    private fun updateHabitReminders(
        remindersEnabled: Boolean
    ) {

        val user =
            firebaseAuth.currentUser

        if (
            user == null
        ) {

            restoreSaveButton()

            return
        }

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .get()
            .addOnSuccessListener { snapshot ->

                val scheduler =
                    HabitReminderScheduler(
                        this
                    )

                snapshot.documents.forEach { document ->

                    val habit =
                        document.toObject(
                            Habit::class.java
                        )

                    if (
                        habit != null
                    ) {

                        habit.id =
                            document.id

                        // =====================================
                        // ALWAYS REMOVE OLD ALARMS FIRST
                        // =====================================

                        scheduler.cancelReminder(
                            habit.id
                        )

                        // =====================================
                        // RESCHEDULE ONLY WHEN MASTER SWITCH
                        // IS ON, HABIT IS ACTIVE AND IT HAS
                        // A REMINDER TIME.
                        // =====================================

                        if (
                            remindersEnabled &&
                            !habit.paused &&
                            habit.reminderTime.isNotBlank()
                        ) {

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
                    }
                }

                finishSavingSettings(
                    remindersEnabled
                )
            }
            .addOnFailureListener { exception ->

                restoreSaveButton()

                Toast.makeText(
                    this,
                    "Settings were saved, but reminders could not be updated: ${
                        exception.localizedMessage
                            ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // FINISH SAVE
    // =========================================================

    private fun finishSavingSettings(
        remindersEnabled: Boolean
    ) {

        restoreSaveButton()

        val message =
            if (
                remindersEnabled
            ) {

                "Settings saved. Habit reminders are enabled 🔔"

            } else {

                "Settings saved. Habit reminders are disabled."
            }

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    // =========================================================
    // BUTTON RESET
    // =========================================================

    private fun restoreSaveButton() {

        btnSaveSettings.isEnabled =
            true

        btnSaveSettings.text =
            "Save Settings"
    }
}