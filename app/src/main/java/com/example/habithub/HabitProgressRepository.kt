package com.example.habithub

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitProgressRepository {

    private val firebaseAuth =
        FirebaseAuth.getInstance()

    private val firestore =
        FirebaseFirestore.getInstance()

    // =========================================================
    // GET CURRENT STREAK
    // =========================================================

    fun getCurrentStreak(
        habitId: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {

        val user =
            firebaseAuth.currentUser

        if (user == null) {

            onError(
                "No signed-in user."
            )

            return
        }

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

                val streak =
                    calculateCurrentStreak(
                        completedDates
                    )

                onSuccess(
                    streak
                )
            }
            .addOnFailureListener { exception ->

                onError(
                    exception.localizedMessage
                        ?: "Could not calculate streak."
                )
            }
    }

    // =========================================================
    // CALCULATE CONSECUTIVE DAYS
    // =========================================================

    private fun calculateCurrentStreak(
        completedDates: Set<String>
    ): Int {

        if (completedDates.isEmpty()) {
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

        /*
         * If today's habit has not been completed yet,
         * start checking from yesterday.
         *
         * This prevents a streak from disappearing in the
         * morning before the student has had a chance to
         * complete today's habit.
         */
        if (!completedDates.contains(today)) {

            calendar.add(
                Calendar.DAY_OF_YEAR,
                -1
            )
        }

        var streak =
            0

        while (true) {

            val date =
                dateFormat.format(
                    calendar.time
                )

            if (completedDates.contains(date)) {

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
}