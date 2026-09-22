package com.example.habithub

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object BottomNavHelper {

    fun setup(
        activity: Activity,
        selectedPage: String
    ) {

        // Give the page enough space so content
        // does not disappear behind the navigation bar.
        val root =
            activity.findViewById<View>(
                android.R.id.content
            )

        root.setPadding(
            root.paddingLeft,
            root.paddingTop,
            root.paddingRight,
            dp(activity, 82)
        )

        // MAIN BOTTOM BAR
        val navigation =
            LinearLayout(activity)

        navigation.orientation =
            LinearLayout.HORIZONTAL

        navigation.gravity =
            Gravity.CENTER

        navigation.setPadding(
            dp(activity, 8),
            dp(activity, 8),
            dp(activity, 8),
            dp(activity, 8)
        )

        navigation.background =
            createNavBackground()

        val navParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 74)
            )

        // HOME
        val home =
            createNavItem(
                activity,
                "⌂",
                "Home",
                selectedPage == "HOME"
            )

        // HABITS
        val habits =
            createNavItem(
                activity,
                "✓",
                "Habits",
                selectedPage == "HABITS"
            )

        // CENTER ADD BUTTON
        val add =
            createAddButton(
                activity
            )

        // PROGRESS
        val progress =
            createNavItem(
                activity,
                "▥",
                "Progress",
                selectedPage == "PROGRESS"
            )

        // PROFILE
        val profile =
            createNavItem(
                activity,
                "●",
                "Profile",
                selectedPage == "PROFILE"
            )

        navigation.addView(home)
        navigation.addView(habits)
        navigation.addView(add)
        navigation.addView(progress)
        navigation.addView(profile)

        // -----------------------------------
        // HOME
        // -----------------------------------

        home.setOnClickListener {

            if (selectedPage != "HOME") {

                val intent =
                    Intent(
                        activity,
                        HomeActivity::class.java
                    )

                activity.startActivity(intent)
            }
        }

        // -----------------------------------
        // HABITS
        // -----------------------------------

        habits.setOnClickListener {

            if (selectedPage != "HABITS") {

                val intent =
                    Intent(
                        activity,
                        HabitsActivity::class.java
                    )

                activity.startActivity(intent)
            }
        }

        // -----------------------------------
        // CREATE
        // -----------------------------------

        add.setOnClickListener {

            val intent =
                Intent(
                    activity,
                    CreateHabitActivity::class.java
                )

            activity.startActivity(intent)
        }

        // -----------------------------------
        // PROGRESS
        // -----------------------------------

        progress.setOnClickListener {

            if (selectedPage != "PROGRESS") {

                val intent =
                    Intent(
                        activity,
                        ProgressActivity::class.java
                    )

                activity.startActivity(intent)
            }
        }

        // -----------------------------------
        // PROFILE
        // -----------------------------------

        profile.setOnClickListener {

            if (selectedPage != "PROFILE") {

                val intent =
                    Intent(
                        activity,
                        ProfileActivity::class.java
                    )

                activity.startActivity(intent)
            }
        }

        // Add the bar over the bottom of the screen.
        activity.addContentView(
            navigation,
            navParams
        )

        val navigationLayoutParams =
            navigation.layoutParams

        if (
            navigationLayoutParams
                    is android.widget.FrameLayout.LayoutParams
        ) {

            navigationLayoutParams.gravity =
                Gravity.BOTTOM

            navigation.layoutParams =
                navigationLayoutParams
        }
    }

    // ==================================================
    // NORMAL NAV ITEM
    // ==================================================

    private fun createNavItem(
        activity: Activity,
        icon: String,
        label: String,
        selected: Boolean
    ): LinearLayout {

        val item =
            LinearLayout(activity)

        item.orientation =
            LinearLayout.VERTICAL

        item.gravity =
            Gravity.CENTER

        item.layoutParams =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )

        val iconView =
            TextView(activity)

        iconView.text =
            icon

        iconView.gravity =
            Gravity.CENTER

        iconView.textSize =
            22f

        iconView.setTextColor(
            if (selected) {
                Color.parseColor("#5B4CF0")
            } else {
                Color.parseColor("#9A9DAB")
            }
        )

        if (selected) {

            iconView.setTypeface(
                null,
                Typeface.BOLD
            )
        }

        val labelView =
            TextView(activity)

        labelView.text =
            label

        labelView.gravity =
            Gravity.CENTER

        labelView.textSize =
            10f

        labelView.setTextColor(
            if (selected) {
                Color.parseColor("#5B4CF0")
            } else {
                Color.parseColor("#9A9DAB")
            }
        )

        labelView.setPadding(
            0,
            dp(activity, 2),
            0,
            0
        )

        if (selected) {

            labelView.setTypeface(
                null,
                Typeface.BOLD
            )
        }

        item.addView(
            iconView
        )

        item.addView(
            labelView
        )

        return item
    }

    // ==================================================
    // CENTER PLUS BUTTON
    // ==================================================

    private fun createAddButton(
        activity: Activity
    ): LinearLayout {

        val holder =
            LinearLayout(activity)

        holder.gravity =
            Gravity.CENTER

        holder.layoutParams =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )

        val plus =
            TextView(activity)

        plus.text =
            "+"

        plus.gravity =
            Gravity.CENTER

        plus.textSize =
            32f

        plus.setTextColor(
            Color.WHITE
        )

        plus.setTypeface(
            null,
            Typeface.NORMAL
        )

        val plusParams =
            LinearLayout.LayoutParams(
                dp(activity, 56),
                dp(activity, 56)
            )

        plus.layoutParams =
            plusParams

        val circle =
            GradientDrawable()

        circle.shape =
            GradientDrawable.OVAL

        circle.setColor(
            Color.parseColor("#5B4CF0")
        )

        plus.background =
            circle

        holder.addView(
            plus
        )

        return holder
    }

    // ==================================================
    // NAV BACKGROUND
    // ==================================================

    private fun createNavBackground():
            GradientDrawable {

        val background =
            GradientDrawable()

        background.shape =
            GradientDrawable.RECTANGLE

        background.setColor(
            Color.WHITE
        )

        background.setStroke(
            1,
            Color.parseColor("#ECECF2")
        )

        return background
    }

    // ==================================================
    // DP
    // ==================================================

    private fun dp(
        activity: Activity,
        value: Int
    ): Int {

        return (
                value *
                        activity.resources
                            .displayMetrics
                            .density
                ).toInt()
    }
}