package com.example.habithub

data class Habit(
    var id: String = "",
    var name: String = "",
    var category: String = "",
    var reminderTime: String = "",
    var goal: String = "",
    var unit: String = "",
    var notes: String = "",

    // Daily / Weekdays / Custom
    var frequency: String = "Daily",

    // Used only when frequency = Custom.
    //
    // Example:
    // ["Monday", "Wednesday", "Friday"]
    //
    // Existing habits that do not have this Firestore field
    // will simply receive an empty list.
    var customDays: List<String> = emptyList(),

    var completed: Boolean = false,
    var paused: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)