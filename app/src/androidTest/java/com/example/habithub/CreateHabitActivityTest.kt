package com.example.habithub

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateHabitActivityTest {

    @Test
    fun createHabitScreen_displaysCorrectFields() {
        ActivityScenario.launch(CreateHabitActivity::class.java)

        onView(withId(R.id.etHabitName))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etGoal))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etReminderTime))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnSaveHabit))
            .check(matches(withText("Save Habit")))
    }

    @Test
    fun customFrequency_displaysDaySelection() {
        ActivityScenario.launch(CreateHabitActivity::class.java)

        onView(withId(R.id.rbCustom))
            .perform(click())

        onView(withId(R.id.cbMonday))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cbTuesday))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cbWednesday))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cbThursday))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cbFriday))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cbSaturday))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cbSunday))
            .check(matches(isDisplayed()))
    }

    @Test
    fun customFrequency_canSelectDays() {
        ActivityScenario.launch(CreateHabitActivity::class.java)

        onView(withId(R.id.rbCustom))
            .perform(click())

        onView(withId(R.id.cbMonday))
            .perform(click())

        onView(withId(R.id.cbFriday))
            .perform(click())

        onView(withId(R.id.cbMonday))
            .check(matches(androidx.test.espresso.matcher.ViewMatchers.isChecked()))

        onView(withId(R.id.cbFriday))
            .check(matches(androidx.test.espresso.matcher.ViewMatchers.isChecked()))
    }
}