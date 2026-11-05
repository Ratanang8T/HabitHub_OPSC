package com.example.habithub

import android.content.Intent
import android.os.Bundle
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

class AchievementsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvAchievementCount: TextView
    private lateinit var tvAchievementPercent: TextView
    private lateinit var tvAchievementSummary: TextView
    private lateinit var progressAchievements: ProgressBar

    private lateinit var tvFeaturedIcon: TextView
    private lateinit var tvFeaturedTitle: TextView
    private lateinit var tvFeaturedDescription: TextView

    private lateinit var cardFirstStep: LinearLayout
    private lateinit var cardSevenDay: LinearLayout
    private lateinit var cardFourteenDay: LinearLayout
    private lateinit var cardThirtyDay: LinearLayout
    private lateinit var cardAcademicStar: LinearLayout
    private lateinit var cardHealthyStudent: LinearLayout
    private lateinit var cardConsistencyMaster: LinearLayout

    private lateinit var iconFirstStep: TextView
    private lateinit var iconSevenDay: TextView
    private lateinit var iconFourteenDay: TextView
    private lateinit var iconThirtyDay: TextView
    private lateinit var iconAcademicStar: TextView
    private lateinit var iconHealthyStudent: TextView
    private lateinit var iconConsistencyMaster: TextView

    private lateinit var statusFirstStep: TextView
    private lateinit var statusSevenDay: TextView
    private lateinit var statusFourteenDay: TextView
    private lateinit var statusThirtyDay: TextView
    private lateinit var statusAcademicStar: TextView
    private lateinit var statusHealthyStudent: TextView
    private lateinit var statusConsistencyMaster: TextView

    private data class HabitAchievementData(
        val habit: Habit,
        val completedDates: Set<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_achievements)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        bindViews()
        setupNavigation()

        showLoadingState()

        loadAchievementData()
    }

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private fun bindViews() {

        tvAchievementCount =
            findViewById(R.id.tvAchievementCount)

        tvAchievementPercent =
            findViewById(R.id.tvAchievementPercent)

        tvAchievementSummary =
            findViewById(R.id.tvAchievementSummary)

        progressAchievements =
            findViewById(R.id.progressAchievements)

        tvFeaturedIcon =
            findViewById(R.id.tvFeaturedIcon)

        tvFeaturedTitle =
            findViewById(R.id.tvFeaturedTitle)

        tvFeaturedDescription =
            findViewById(R.id.tvFeaturedDescription)

        cardFirstStep =
            findViewById(R.id.cardFirstStep)

        cardSevenDay =
            findViewById(R.id.cardSevenDay)

        cardFourteenDay =
            findViewById(R.id.cardFourteenDay)

        cardThirtyDay =
            findViewById(R.id.cardThirtyDay)

        cardAcademicStar =
            findViewById(R.id.cardAcademicStar)

        cardHealthyStudent =
            findViewById(R.id.cardHealthyStudent)

        cardConsistencyMaster =
            findViewById(R.id.cardConsistencyMaster)

        iconFirstStep =
            findViewById(R.id.iconFirstStep)

        iconSevenDay =
            findViewById(R.id.iconSevenDay)

        iconFourteenDay =
            findViewById(R.id.iconFourteenDay)

        iconThirtyDay =
            findViewById(R.id.iconThirtyDay)

        iconAcademicStar =
            findViewById(R.id.iconAcademicStar)

        iconHealthyStudent =
            findViewById(R.id.iconHealthyStudent)

        iconConsistencyMaster =
            findViewById(R.id.iconConsistencyMaster)

        statusFirstStep =
            findViewById(R.id.statusFirstStep)

        statusSevenDay =
            findViewById(R.id.statusSevenDay)

        statusFourteenDay =
            findViewById(R.id.statusFourteenDay)

        statusThirtyDay =
            findViewById(R.id.statusThirtyDay)

        statusAcademicStar =
            findViewById(R.id.statusAcademicStar)

        statusHealthyStudent =
            findViewById(R.id.statusHealthyStudent)

        statusConsistencyMaster =
            findViewById(R.id.statusConsistencyMaster)
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private fun setupNavigation() {

        val tvBack =
            findViewById<TextView>(R.id.tvBack)

        val btnProfile =
            findViewById<Button>(R.id.btnProfile)

        tvBack.setOnClickListener {
            finish()
        }

        btnProfile.setOnClickListener {

            val intent =
                Intent(
                    this,
                    ProfileActivity::class.java
                )

            startActivity(intent)
        }
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun showLoadingState() {

        tvAchievementCount.text = "0/7"
        tvAchievementPercent.text = "0%"

        tvAchievementSummary.text =
            getString(R.string.achievements_loading)

        progressAchievements.progress = 0

        tvFeaturedIcon.text = "🏆"

        tvFeaturedTitle.text =
            getString(R.string.your_achievements)

        tvFeaturedDescription.text =
            getString(R.string.checking_habithub_progress)
    }

    // =========================================================
    // LOAD HABITS
    // =========================================================

    private fun loadAchievementData() {

        val user = auth.currentUser

        if (user == null) {

            tvAchievementSummary.text =
                getString(R.string.sign_in_view_achievements)

            return
        }

        firestore
            .collection("users")
            .document(user.uid)
            .collection("habits")
            .get()
            .addOnSuccessListener { habitSnapshot ->

                val habits =
                    habitSnapshot.documents.mapNotNull { document ->

                        document
                            .toObject(Habit::class.java)
                            ?.apply {
                                id = document.id
                            }
                    }

                if (habits.isEmpty()) {

                    calculateAchievements(
                        emptyList()
                    )

                    return@addOnSuccessListener
                }

                loadCompletions(
                    user.uid,
                    habits
                )
            }
            .addOnFailureListener {

                tvAchievementSummary.text =
                    getString(R.string.unable_load_achievements)

                Toast.makeText(
                    this,
                    getString(R.string.could_not_load_achievement_data),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =========================================================
    // LOAD COMPLETION HISTORY
    // =========================================================

    private fun loadCompletions(
        userId: String,
        habits: List<Habit>
    ) {

        val achievementData =
            mutableListOf<HabitAchievementData>()

        var completedQueries = 0

        habits.forEach { habit ->

            firestore
                .collection("users")
                .document(userId)
                .collection("habits")
                .document(habit.id)
                .collection("completions")
                .get()
                .addOnSuccessListener { completionSnapshot ->

                    val completedDates =
                        completionSnapshot.documents
                            .mapNotNull { document ->

                                val completion =
                                    document.toObject(
                                        HabitCompletion::class.java
                                    )

                                when {

                                    !completion
                                        ?.completionDate
                                        .isNullOrBlank() -> {

                                        completion?.completionDate
                                    }

                                    document.id.matches(
                                        Regex(
                                            "\\d{4}-\\d{2}-\\d{2}"
                                        )
                                    ) -> {

                                        document.id
                                    }

                                    else -> null
                                }
                            }
                            .filterNotNull()
                            .toSet()

                    achievementData.add(
                        HabitAchievementData(
                            habit = habit,
                            completedDates = completedDates
                        )
                    )

                    completedQueries++

                    if (
                        completedQueries ==
                        habits.size
                    ) {

                        calculateAchievements(
                            achievementData
                        )
                    }
                }
                .addOnFailureListener {

                    achievementData.add(
                        HabitAchievementData(
                            habit = habit,
                            completedDates = emptySet()
                        )
                    )

                    completedQueries++

                    if (
                        completedQueries ==
                        habits.size
                    ) {

                        calculateAchievements(
                            achievementData
                        )
                    }
                }
        }
    }

    // =========================================================
    // CALCULATE ALL ACHIEVEMENTS
    // =========================================================

    private fun calculateAchievements(
        data: List<HabitAchievementData>
    ) {

        val totalCompletions =
            data.sumOf {
                it.completedDates.size
            }

        val firstStepUnlocked =
            totalCompletions >= 1

        var bestStreak = 0

        data.forEach { item ->

            val scheduledDates =
                createScheduledDates(
                    item.habit,
                    item.completedDates
                )

            val habitBestStreak =
                HabitStatsCalculator
                    .calculateBestStreak(
                        scheduledDates,
                        item.completedDates
                    )

            if (
                habitBestStreak >
                bestStreak
            ) {

                bestStreak =
                    habitBestStreak
            }
        }

        val sevenDayUnlocked =
            bestStreak >= 7

        val fourteenDayUnlocked =
            bestStreak >= 14

        val thirtyDayUnlocked =
            bestStreak >= 30

        val academicConsistency =
            calculateCategoryConsistency(
                data,
                listOf(
                    "academic",
                    "study",
                    "education"
                )
            )

        val healthConsistency =
            calculateCategoryConsistency(
                data,
                listOf(
                    "health",
                    "fitness",
                    "exercise",
                    "wellness"
                )
            )

        val overallConsistency =
            calculateOverallConsistency(
                data
            )

        val academicStarUnlocked =
            academicConsistency >= 80

        val healthyStudentUnlocked =
            healthConsistency >= 80

        val consistencyMasterUnlocked =
            overallConsistency >= 90

        val unlockedCount =
            listOf(
                firstStepUnlocked,
                sevenDayUnlocked,
                fourteenDayUnlocked,
                thirtyDayUnlocked,
                academicStarUnlocked,
                healthyStudentUnlocked,
                consistencyMasterUnlocked
            ).count {
                it
            }

        updateAchievementScreen(
            unlockedCount = unlockedCount,

            firstStepUnlocked =
                firstStepUnlocked,

            sevenDayUnlocked =
                sevenDayUnlocked,

            fourteenDayUnlocked =
                fourteenDayUnlocked,

            thirtyDayUnlocked =
                thirtyDayUnlocked,

            academicStarUnlocked =
                academicStarUnlocked,

            healthyStudentUnlocked =
                healthyStudentUnlocked,

            consistencyMasterUnlocked =
                consistencyMasterUnlocked,

            bestStreak =
                bestStreak,

            academicConsistency =
                academicConsistency,

            healthConsistency =
                healthConsistency,

            overallConsistency =
                overallConsistency
        )
    }

    // =========================================================
    // CREATE SCHEDULED DATES
    // =========================================================

    private fun createScheduledDates(
        habit: Habit,
        completedDates: Set<String>
    ): List<String> {

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )

        val calendar =
            Calendar.getInstance()

        val today =
            Calendar.getInstance()

        val startDate =
            findStartDate(
                habit,
                completedDates
            )

        calendar.timeInMillis =
            startDate.timeInMillis

        val scheduledDates =
            mutableListOf<String>()

        while (
            !calendar.after(today)
        ) {

            if (
                HabitStatsCalculator
                    .isHabitExpectedOnDay(
                        habit.frequency,
                        habit.customDays,
                        calendar.get(
                            Calendar.DAY_OF_WEEK
                        )
                    )
            ) {

                scheduledDates.add(
                    dateFormat.format(
                        calendar.time
                    )
                )
            }

            calendar.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return scheduledDates
    }

    // =========================================================
    // FIND HABIT START DATE
    // =========================================================

    private fun findStartDate(
        habit: Habit,
        completedDates: Set<String>
    ): Calendar {

        val result =
            Calendar.getInstance()

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )

        val createdAt =
            habit.createdAt

        if (
            createdAt > 0L &&
            createdAt <= System.currentTimeMillis()
        ) {

            result.timeInMillis =
                createdAt

            clearTime(result)

            return result
        }

        val earliestCompletion =
            completedDates
                .mapNotNull { date ->

                    try {

                        dateFormat.parse(date)

                    } catch (
                        exception: Exception
                    ) {

                        null
                    }
                }
                .minOrNull()

        if (
            earliestCompletion != null
        ) {

            result.time =
                earliestCompletion

            clearTime(result)

            return result
        }

        clearTime(result)

        return result
    }

    // =========================================================
    // CATEGORY CONSISTENCY
    // =========================================================

    private fun calculateCategoryConsistency(
        data: List<HabitAchievementData>,
        categoryKeywords: List<String>
    ): Int {

        val matchingHabits =
            data.filter { item ->

                val category =
                    item.habit.category
                        .lowercase(
                            Locale.getDefault()
                        )

                categoryKeywords.any {
                        keyword ->

                    category.contains(
                        keyword
                    )
                }
            }

        if (
            matchingHabits.isEmpty()
        ) {

            return 0
        }

        var expectedDays = 0
        var completedDays = 0

        matchingHabits.forEach { item ->

            val scheduledDates =
                createScheduledDates(
                    item.habit,
                    item.completedDates
                )

            expectedDays +=
                scheduledDates.size

            completedDays +=
                item.completedDates.count {
                        date ->

                    scheduledDates.contains(
                        date
                    )
                }
        }

        return HabitStatsCalculator
            .calculateCompletionPercentage(
                expectedDays,
                completedDays
            )
    }

    // =========================================================
    // OVERALL CONSISTENCY
    // =========================================================

    private fun calculateOverallConsistency(
        data: List<HabitAchievementData>
    ): Int {

        if (
            data.isEmpty()
        ) {

            return 0
        }

        var expectedDays = 0
        var completedDays = 0

        data.forEach { item ->

            val scheduledDates =
                createScheduledDates(
                    item.habit,
                    item.completedDates
                )

            expectedDays +=
                scheduledDates.size

            completedDays +=
                item.completedDates.count {
                        date ->

                    scheduledDates.contains(
                        date
                    )
                }
        }

        return HabitStatsCalculator
            .calculateCompletionPercentage(
                expectedDays,
                completedDays
            )
    }

    // =========================================================
    // UPDATE SCREEN
    // =========================================================

    private fun updateAchievementScreen(
        unlockedCount: Int,
        firstStepUnlocked: Boolean,
        sevenDayUnlocked: Boolean,
        fourteenDayUnlocked: Boolean,
        thirtyDayUnlocked: Boolean,
        academicStarUnlocked: Boolean,
        healthyStudentUnlocked: Boolean,
        consistencyMasterUnlocked: Boolean,
        bestStreak: Int,
        academicConsistency: Int,
        healthConsistency: Int,
        overallConsistency: Int
    ) {

        val totalAchievements =
            7

        val achievementPercentage =
            (
                    unlockedCount.toDouble() /
                            totalAchievements.toDouble() *
                            100
                    ).toInt()

        tvAchievementCount.text =
            "$unlockedCount/$totalAchievements"

        tvAchievementPercent.text =
            "$achievementPercentage%"

        tvAchievementSummary.text =
            getString(
                R.string.achievements_unlocked,
                unlockedCount,
                totalAchievements
            )

        progressAchievements.progress =
            achievementPercentage

        updateBadge(
            card = cardFirstStep,
            icon = iconFirstStep,
            status = statusFirstStep,
            unlocked = firstStepUnlocked,
            unlockedIcon = "🌟",
            lockedText =
                getString(R.string.first_step_locked)
        )

        updateBadge(
            card = cardSevenDay,
            icon = iconSevenDay,
            status = statusSevenDay,
            unlocked = sevenDayUnlocked,
            unlockedIcon = "🔥",
            lockedText =
                getString(
                    R.string.seven_day_streak_progress,
                    bestStreak
                )
        )

        updateBadge(
            card = cardFourteenDay,
            icon = iconFourteenDay,
            status = statusFourteenDay,
            unlocked = fourteenDayUnlocked,
            unlockedIcon = "🏆",
            lockedText =
                getString(
                    R.string.fourteen_day_streak_progress,
                    bestStreak
                )
        )

        updateBadge(
            card = cardThirtyDay,
            icon = iconThirtyDay,
            status = statusThirtyDay,
            unlocked = thirtyDayUnlocked,
            unlockedIcon = "👑",
            lockedText =
                getString(
                    R.string.thirty_day_streak_progress,
                    bestStreak
                )
        )

        updateBadge(
            card = cardAcademicStar,
            icon = iconAcademicStar,
            status = statusAcademicStar,
            unlocked = academicStarUnlocked,
            unlockedIcon = "🎓",
            lockedText =
                getString(
                    R.string.academic_consistency_progress,
                    academicConsistency
                )
        )

        updateBadge(
            card = cardHealthyStudent,
            icon = iconHealthyStudent,
            status = statusHealthyStudent,
            unlocked = healthyStudentUnlocked,
            unlockedIcon = "💪",
            lockedText =
                getString(
                    R.string.health_consistency_progress,
                    healthConsistency
                )
        )

        updateBadge(
            card = cardConsistencyMaster,
            icon = iconConsistencyMaster,
            status = statusConsistencyMaster,
            unlocked = consistencyMasterUnlocked,
            unlockedIcon = "⭐",
            lockedText =
                getString(
                    R.string.overall_consistency_progress,
                    overallConsistency
                )
        )

        updateFeaturedAchievement(
            firstStepUnlocked,
            sevenDayUnlocked,
            fourteenDayUnlocked,
            thirtyDayUnlocked,
            academicStarUnlocked,
            healthyStudentUnlocked,
            consistencyMasterUnlocked
        )
    }

    // =========================================================
    // UPDATE ONE BADGE
    // =========================================================

    private fun updateBadge(
        card: LinearLayout,
        icon: TextView,
        status: TextView,
        unlocked: Boolean,
        unlockedIcon: String,
        lockedText: String
    ) {

        if (
            unlocked
        ) {

            card.alpha =
                1.0f

            icon.text =
                unlockedIcon
            status.text =
                getString(R.string.achievement_unlocked)

            status.setTextColor(
                android.graphics.Color.parseColor(
                    "#21A78D"
                )
            )

        } else {

            card.alpha =
                0.55f

            icon.text =
                "🔒"

            status.text =
                lockedText

            status.setTextColor(
                android.graphics.Color.parseColor(
                    "#858998"
                )
            )
        }
    }

    // =========================================================
    // FEATURED ACHIEVEMENT
    // =========================================================

    private fun updateFeaturedAchievement(
        firstStep: Boolean,
        sevenDay: Boolean,
        fourteenDay: Boolean,
        thirtyDay: Boolean,
        academicStar: Boolean,
        healthyStudent: Boolean,
        consistencyMaster: Boolean
    ) {

        when {

            consistencyMaster -> {

                tvFeaturedIcon.text = "⭐"

                tvFeaturedTitle.text =
                    getString(R.string.consistency_master_featured)

                tvFeaturedDescription.text =
                    getString(R.string.consistency_master_description)
            }

            thirtyDay -> {

                tvFeaturedIcon.text = "👑"

                tvFeaturedTitle.text =
                    getString(R.string.thirty_day_champion_featured)

                tvFeaturedDescription.text =
                    getString(R.string.thirty_day_champion_description)
            }

            healthyStudent -> {

                tvFeaturedIcon.text = "💪"

                tvFeaturedTitle.text =
                    getString(R.string.healthy_student_featured)

                tvFeaturedDescription.text =
                    getString(R.string.healthy_student_description)
            }

            academicStar -> {

                tvFeaturedIcon.text = "🎓"

                tvFeaturedTitle.text =
                    getString(R.string.academic_star_featured)

                tvFeaturedDescription.text =
                    getString(R.string.academic_star_description)
            }

            fourteenDay -> {

                tvFeaturedIcon.text = "🏆"

                tvFeaturedTitle.text =
                    getString(R.string.fourteen_day_streak_featured)

                tvFeaturedDescription.text =
                    getString(R.string.fourteen_day_streak_description)
            }

            sevenDay -> {

                tvFeaturedIcon.text = "🔥"

                tvFeaturedTitle.text =
                    getString(R.string.seven_day_warrior_featured)

                tvFeaturedDescription.text =
                    getString(R.string.seven_day_warrior_description)
            }

            firstStep -> {

                tvFeaturedIcon.text = "🌟"

                tvFeaturedTitle.text =
                    getString(R.string.first_step_featured)

                tvFeaturedDescription.text =
                    getString(R.string.first_step_description)
            }

            else -> {

                tvFeaturedIcon.text = "🏆"

                tvFeaturedTitle.text =
                    getString(R.string.your_achievements)

                tvFeaturedDescription.text =
                    getString(R.string.unlock_first_badge)
            }
        }
    }
    // =========================================================
    // REMOVE TIME FROM CALENDAR
    // =========================================================

    private fun clearTime(
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
}