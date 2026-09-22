package com.example.habithub

data class HabitCompletion(

    var id: String = "",

    // Firebase ID of the habit that was completed
    var habitId: String = "",

    // Name is stored as well so progress screens
    // are easier to display later
    var habitName: String = "",

    var category: String = "",

    // Example: "2026-09-17"
    // This lets us know exactly which day was completed
    var completionDate: String = "",

    // Exact time the completion was recorded
    var completedAt: Long = System.currentTimeMillis()
)