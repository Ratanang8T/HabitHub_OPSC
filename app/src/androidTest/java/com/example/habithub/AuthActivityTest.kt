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
class AuthActivityTest {

    @Test
    fun loginMode_displaysCorrectFields() {
        ActivityScenario.launch(AuthActivity::class.java)

        onView(withId(R.id.etEmail))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etPassword))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnAuth))
            .check(matches(withText("Sign In to HabitHub")))

        onView(withId(R.id.tvForgotPassword))
            .check(matches(isDisplayed()))
    }

    @Test
    fun signUpMode_displaysRegistrationFields() {
        val intent = android.content.Intent(
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext,
            AuthActivity::class.java
        )

        intent.putExtra("MODE", "SIGNUP")

        ActivityScenario.launch<AuthActivity>(intent)

        onView(withId(R.id.etFullName))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etConfirmPassword))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnAuth))
            .check(matches(withText("Create My Account")))
    }

    @Test
    fun loginTab_switchesToSignUpMode() {
        ActivityScenario.launch(AuthActivity::class.java)

        onView(withId(R.id.tabSignUp))
            .perform(click())

        onView(withId(R.id.etFullName))
            .check(matches(isDisplayed()))

        onView(withId(R.id.etConfirmPassword))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnAuth))
            .check(matches(withText("Create My Account")))
    }
}