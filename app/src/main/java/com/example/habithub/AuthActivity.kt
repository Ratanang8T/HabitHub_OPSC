package com.example.habithub

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {

    private lateinit var tabLogin: TextView
    private lateinit var tabSignUp: TextView

    private lateinit var fullNameContainer: LinearLayout
    private lateinit var confirmPasswordContainer: LinearLayout

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var btnAuth: Button
    private lateinit var tvAuthMessage: TextView
    private lateinit var tvForgotPassword: TextView

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var currentMode =
        "LOGIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_auth
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

        tabLogin =
            findViewById(
                R.id.tabLogin
            )

        tabSignUp =
            findViewById(
                R.id.tabSignUp
            )

        fullNameContainer =
            findViewById(
                R.id.fullNameContainer
            )

        confirmPasswordContainer =
            findViewById(
                R.id.confirmPasswordContainer
            )

        etFullName =
            findViewById(
                R.id.etFullName
            )

        etEmail =
            findViewById(
                R.id.etEmail
            )

        etPassword =
            findViewById(
                R.id.etPassword
            )

        etConfirmPassword =
            findViewById(
                R.id.etConfirmPassword
            )

        btnAuth =
            findViewById(
                R.id.btnAuth
            )

        tvAuthMessage =
            findViewById(
                R.id.tvAuthMessage
            )

        tvForgotPassword =
            findViewById(
                R.id.tvForgotPassword
            )

        val btnGoogle =
            findViewById<Button>(
                R.id.btnGoogle
            )

        val btnApple =
            findViewById<Button>(
                R.id.btnApple
            )

        // =====================================================
        // LOGIN / SIGN-UP MODE
        // =====================================================

        val mode =
            intent.getStringExtra(
                "MODE"
            )

        if (mode == "SIGNUP") {

            showSignUpMode()

        } else {

            showLoginMode()
        }

        // =====================================================
        // LOGIN TAB
        // =====================================================

        tabLogin.setOnClickListener {

            showLoginMode()
        }

        // =====================================================
        // SIGN-UP TAB
        // =====================================================

        tabSignUp.setOnClickListener {

            showSignUpMode()
        }

        // =====================================================
        // MAIN AUTH BUTTON
        // =====================================================

        btnAuth.setOnClickListener {

            tvAuthMessage.text =
                ""

            if (currentMode == "LOGIN") {

                validateLogin()

            } else {

                validateSignUp()
            }
        }

        // =====================================================
        // FORGOT PASSWORD
        // =====================================================

        tvForgotPassword.setOnClickListener {

            sendPasswordReset()
        }

        // =====================================================
        // GOOGLE LOGIN PLACEHOLDER
        // =====================================================

        btnGoogle.setOnClickListener {

            Toast.makeText(
                this,
                "Google sign in is not enabled yet.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // APPLE LOGIN PLACEHOLDER
        // =====================================================

        btnApple.setOnClickListener {

            Toast.makeText(
                this,
                "Apple sign in is not enabled yet.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // LOGIN MODE
    // =========================================================

    private fun showLoginMode() {

        currentMode =
            "LOGIN"

        tvAuthMessage.text =
            ""

        fullNameContainer.visibility =
            View.GONE

        confirmPasswordContainer.visibility =
            View.GONE

        tvForgotPassword.visibility =
            View.VISIBLE

        btnAuth.text =
            "Sign In to HabitHub"

        tabLogin.setBackgroundColor(
            Color.WHITE
        )

        tabLogin.setTextColor(
            Color.parseColor("#5142E5")
        )

        tabSignUp.setBackgroundColor(
            Color.TRANSPARENT
        )

        tabSignUp.setTextColor(
            Color.WHITE
        )
    }

    // =========================================================
    // SIGN-UP MODE
    // =========================================================

    private fun showSignUpMode() {

        currentMode =
            "SIGNUP"

        tvAuthMessage.text =
            ""

        fullNameContainer.visibility =
            View.VISIBLE

        confirmPasswordContainer.visibility =
            View.VISIBLE

        tvForgotPassword.visibility =
            View.GONE

        btnAuth.text =
            "Create My Account"

        tabSignUp.setBackgroundColor(
            Color.WHITE
        )

        tabSignUp.setTextColor(
            Color.parseColor("#5142E5")
        )

        tabLogin.setBackgroundColor(
            Color.TRANSPARENT
        )

        tabLogin.setTextColor(
            Color.WHITE
        )
    }

    // =========================================================
    // VALIDATE LOGIN
    // =========================================================

    private fun validateLogin() {

        val email =
            etEmail.text
                .toString()
                .trim()

        val password =
            etPassword.text
                .toString()

        if (email.isEmpty()) {

            etEmail.error =
                "Email is required"

            etEmail.requestFocus()

            return
        }

        if (
            !Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            etEmail.error =
                "Enter a valid email"

            etEmail.requestFocus()

            return
        }

        if (password.isEmpty()) {

            etPassword.error =
                "Password is required"

            etPassword.requestFocus()

            return
        }

        if (password.length < 6) {

            etPassword.error =
                "Password must contain at least 6 characters"

            etPassword.requestFocus()

            return
        }

        loginUser(
            email,
            password
        )
    }

    // =========================================================
    // VALIDATE SIGN-UP
    // =========================================================

    private fun validateSignUp() {

        val fullName =
            etFullName.text
                .toString()
                .trim()

        val email =
            etEmail.text
                .toString()
                .trim()

        val password =
            etPassword.text
                .toString()

        val confirmPassword =
            etConfirmPassword.text
                .toString()

        if (fullName.isEmpty()) {

            etFullName.error =
                "Full name is required"

            etFullName.requestFocus()

            return
        }

        if (email.isEmpty()) {

            etEmail.error =
                "Email is required"

            etEmail.requestFocus()

            return
        }

        if (
            !Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            etEmail.error =
                "Enter a valid email"

            etEmail.requestFocus()

            return
        }

        if (password.isEmpty()) {

            etPassword.error =
                "Password is required"

            etPassword.requestFocus()

            return
        }

        if (password.length < 6) {

            etPassword.error =
                "Password must contain at least 6 characters"

            etPassword.requestFocus()

            return
        }

        if (confirmPassword.isEmpty()) {

            etConfirmPassword.error =
                "Please confirm your password"

            etConfirmPassword.requestFocus()

            return
        }

        if (password != confirmPassword) {

            etConfirmPassword.error =
                "Passwords do not match"

            etConfirmPassword.requestFocus()

            return
        }

        registerUser(
            fullName,
            email,
            password
        )
    }

    // =========================================================
    // FIREBASE REGISTER
    // =========================================================

    private fun registerUser(
        fullName: String,
        email: String,
        password: String
    ) {

        setLoading(true)

        firebaseAuth
            .createUserWithEmailAndPassword(
                email,
                password
            )
            .addOnSuccessListener {

                val firebaseUser =
                    firebaseAuth.currentUser

                if (firebaseUser == null) {

                    setLoading(false)

                    tvAuthMessage.text =
                        "Account could not be created."

                    return@addOnSuccessListener
                }

                val uid =
                    firebaseUser.uid

                // Password is NOT stored in Firestore.
                // Firebase Authentication securely manages it.

                val userData =
                    hashMapOf(
                        "uid" to uid,
                        "fullName" to fullName,
                        "email" to email,
                        "createdAt" to
                                FieldValue.serverTimestamp()
                    )

                firestore
                    .collection("users")
                    .document(uid)
                    .set(userData)
                    .addOnSuccessListener {

                        setLoading(false)

                        saveUserSession(
                            uid,
                            fullName,
                            email
                        )

                        Toast.makeText(
                            this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        openHome(
                            fullName
                        )
                    }
                    .addOnFailureListener { exception ->

                        setLoading(false)

                        tvAuthMessage.text =
                            "Account created, but profile setup failed: " +
                                    (
                                            exception.localizedMessage
                                                ?: "Unknown Firestore error"
                                            )
                    }
            }
            .addOnFailureListener { exception ->

                setLoading(false)

                tvAuthMessage.text =
                    firebaseErrorMessage(
                        exception.localizedMessage
                    )
            }
    }

    // =========================================================
    // FIREBASE LOGIN
    // =========================================================

    private fun loginUser(
        email: String,
        password: String
    ) {

        setLoading(true)

        firebaseAuth
            .signInWithEmailAndPassword(
                email,
                password
            )
            .addOnSuccessListener {

                val firebaseUser =
                    firebaseAuth.currentUser

                if (firebaseUser == null) {

                    setLoading(false)

                    tvAuthMessage.text =
                        "Unable to load your account."

                    return@addOnSuccessListener
                }

                val uid =
                    firebaseUser.uid

                // =================================================
                // LOAD PROFILE FROM FIRESTORE
                // =================================================

                firestore
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        setLoading(false)

                        val fullName =
                            document.getString(
                                "fullName"
                            ) ?: "Student"

                        val savedEmail =
                            document.getString(
                                "email"
                            )
                                ?: firebaseUser.email
                                ?: email

                        saveUserSession(
                            uid,
                            fullName,
                            savedEmail
                        )

                        Toast.makeText(
                            this,
                            "Welcome back, $fullName",
                            Toast.LENGTH_SHORT
                        ).show()

                        openHome(
                            fullName
                        )
                    }
                    .addOnFailureListener { exception ->

                        setLoading(false)

                        tvAuthMessage.text =
                            "Signed in, but profile could not be loaded: " +
                                    (
                                            exception.localizedMessage
                                                ?: "Unknown Firestore error"
                                            )

                        Toast.makeText(
                            this,
                            exception.localizedMessage
                                ?: "Firestore profile error.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            // =====================================================
            // IMPORTANT:
            // SHOW THE REAL FIREBASE LOGIN ERROR
            // =====================================================

            .addOnFailureListener { exception ->

                setLoading(false)

                val errorMessage =
                    exception.localizedMessage
                        ?: "Unknown Firebase login error."

                tvAuthMessage.text =
                    "LOGIN ERROR: $errorMessage"

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // PASSWORD RESET
    // =========================================================

    private fun sendPasswordReset() {

        tvAuthMessage.text =
            ""

        val email =
            etEmail.text
                .toString()
                .trim()

        if (email.isEmpty()) {

            etEmail.error =
                "Enter your email first"

            etEmail.requestFocus()

            return
        }

        if (
            !Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            etEmail.error =
                "Enter a valid email"

            etEmail.requestFocus()

            return
        }

        firebaseAuth
            .sendPasswordResetEmail(
                email
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Password reset email sent.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { exception ->

                tvAuthMessage.text =
                    exception.localizedMessage
                        ?: "Unable to send password reset email."
            }
    }

    // =========================================================
    // SAVE LOCAL SESSION DETAILS
    // =========================================================

    private fun saveUserSession(
        userId: String,
        fullName: String,
        email: String
    ) {

        val preferences =
            getSharedPreferences(
                "UserAccount",
                MODE_PRIVATE
            )

        preferences
            .edit()
            .putString(
                "USER_ID",
                userId
            )
            .putString(
                "FULL_NAME",
                fullName
            )
            .putString(
                "EMAIL",
                email
            )
            .apply()
    }

    // =========================================================
    // FRIENDLY FIREBASE REGISTRATION ERRORS
    // =========================================================

    private fun firebaseErrorMessage(
        firebaseMessage: String?
    ): String {

        val message =
            firebaseMessage
                ?: return "Registration failed."

        return when {

            message.contains(
                "email address is already in use",
                ignoreCase = true
            ) ->

                "An account with this email already exists."

            message.contains(
                "badly formatted",
                ignoreCase = true
            ) ->

                "Enter a valid email address."

            message.contains(
                "password",
                ignoreCase = true
            ) ->

                "Please choose a stronger password."

            message.contains(
                "network",
                ignoreCase = true
            ) ->

                "Unable to connect. Check your internet connection."

            else ->

                message
        }
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun setLoading(
        loading: Boolean
    ) {

        btnAuth.isEnabled =
            !loading

        tabLogin.isEnabled =
            !loading

        tabSignUp.isEnabled =
            !loading

        if (loading) {

            btnAuth.text =
                if (currentMode == "LOGIN") {

                    "Signing In..."

                } else {

                    "Creating Account..."
                }

        } else {

            btnAuth.text =
                if (currentMode == "LOGIN") {

                    "Sign In to HabitHub"

                } else {

                    "Create My Account"
                }
        }
    }

    // =========================================================
    // OPEN HOME
    // =========================================================

    private fun openHome(
        fullName: String
    ) {

        val intent =
            Intent(
                this,
                HomeActivity::class.java
            )

        intent.putExtra(
            "FULL_NAME",
            fullName
        )

        startActivity(
            intent
        )

        finish()
    }
}