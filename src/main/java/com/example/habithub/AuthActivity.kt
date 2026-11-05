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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
    private lateinit var btnGoogle: Button
    private lateinit var tvAuthMessage: TextView
    private lateinit var tvForgotPassword: TextView

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var currentMode = "LOGIN"

    // =========================================================
    // GOOGLE SIGN-IN RESULT
    // =========================================================

    private val googleSignInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val task =
                GoogleSignIn.getSignedInAccountFromIntent(
                    result.data
                )

            try {

                val account =
                    task.getResult(
                        com.google.android.gms.common.api.ApiException::class.java
                    )

                val idToken =
                    account.idToken

                if (idToken.isNullOrBlank()) {

                    setLoading(false)

                    tvAuthMessage.text =
                        getString(R.string.google_token_failed)

                    return@registerForActivityResult
                }

                firebaseAuthWithGoogle(
                    idToken
                )

            } catch (exception: Exception) {

                setLoading(false)

                tvAuthMessage.text =
                    getString(
                        R.string.google_signin_failed,
                        exception.localizedMessage ?: "Unknown error"
                    )

                Toast.makeText(
                    this,
                    getString(R.string.google_signin_not_completed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

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

        btnGoogle =
            findViewById(
                R.id.btnGoogle
            )

        tvAuthMessage =
            findViewById(
                R.id.tvAuthMessage
            )

        tvForgotPassword =
            findViewById(
                R.id.tvForgotPassword
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

        if (
            mode == "SIGNUP"
        ) {

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

            tvAuthMessage.text = ""

            if (
                currentMode == "LOGIN"
            ) {

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
        // GOOGLE SSO
        // =====================================================

        btnGoogle.setOnClickListener {

            startGoogleSignIn()
        }

        // =====================================================
        // APPLE PLACEHOLDER
        // =====================================================

        btnApple.setOnClickListener {

            Toast.makeText(
                this,
                getString(R.string.apple_signin_not_enabled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // GOOGLE SIGN-IN
    // =========================================================

    private fun startGoogleSignIn() {

        tvAuthMessage.text = ""

        setLoading(true)

        val googleSignInOptions =
            GoogleSignInOptions
                .Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                .requestIdToken(
                    getString(
                        R.string.default_web_client_id
                    )
                )
                .requestEmail()
                .build()

        val googleSignInClient =
            GoogleSignIn.getClient(
                this,
                googleSignInOptions
            )

        /*
         * Signing out here makes the account chooser appear,
         * which is useful for HabitHub because different students
         * may use the same test device during demonstration.
         */
        googleSignInClient
            .signOut()
            .addOnCompleteListener {

                googleSignInLauncher.launch(
                    googleSignInClient.signInIntent
                )
            }
    }

    // =========================================================
    // FIREBASE AUTHENTICATION WITH GOOGLE
    // =========================================================

    private fun firebaseAuthWithGoogle(
        idToken: String
    ) {

        val credential =
            GoogleAuthProvider.getCredential(
                idToken,
                null
            )

        firebaseAuth
            .signInWithCredential(
                credential
            )
            .addOnSuccessListener {

                val firebaseUser =
                    firebaseAuth.currentUser

                if (
                    firebaseUser == null
                ) {

                    setLoading(false)

                    tvAuthMessage.text =
                        getString(R.string.google_account_not_loaded)

                    return@addOnSuccessListener
                }

                createOrLoadGoogleProfile()
            }
            .addOnFailureListener { exception ->

                setLoading(false)

                tvAuthMessage.text =
                    getString(
                        R.string.google_auth_failed,
                        exception.localizedMessage ?: "Unknown error"
                    )

                Toast.makeText(
                    this,
                    getString(R.string.unable_google_signin),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // CREATE OR LOAD GOOGLE USER PROFILE
    // =========================================================

    private fun createOrLoadGoogleProfile() {

        val firebaseUser =
            firebaseAuth.currentUser

        if (
            firebaseUser == null
        ) {

            setLoading(false)

            tvAuthMessage.text =
                getString(R.string.unable_load_google_account)

            return
        }

        val uid =
            firebaseUser.uid

        val email =
            firebaseUser.email
                ?: ""

        val googleName =
            firebaseUser.displayName
                ?.trim()
                .orEmpty()

        val fallbackName =
            if (
                googleName.isNotBlank()
            ) {

                googleName

            } else {

                getString(R.string.student)
            }

        val userReference =
            firestore
                .collection("users")
                .document(uid)

        userReference
            .get()
            .addOnSuccessListener { document ->

                if (
                    document.exists()
                ) {

                    // =========================================
                    // EXISTING HABITHUB USER
                    // =========================================

                    val fullName =
                        document.getString(
                            "fullName"
                        )
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: fallbackName

                    val savedEmail =
                        document.getString(
                            "email"
                        )
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: email

                    completeGoogleLogin(
                        uid = uid,
                        fullName = fullName,
                        email = savedEmail
                    )

                } else {

                    // =========================================
                    // FIRST GOOGLE LOGIN
                    // CREATE FIRESTORE PROFILE
                    // =========================================

                    val userData =
                        hashMapOf(
                            "uid" to uid,
                            "fullName" to fallbackName,
                            "email" to email,
                            "authProvider" to "google",
                            "createdAt" to
                                    FieldValue.serverTimestamp()
                        )

                    userReference
                        .set(
                            userData
                        )
                        .addOnSuccessListener {

                            completeGoogleLogin(
                                uid = uid,
                                fullName = fallbackName,
                                email = email
                            )
                        }
                        .addOnFailureListener { exception ->

                            setLoading(false)

                            tvAuthMessage.text =
                                getString(
                                    R.string.google_profile_create_failed,
                                    exception.localizedMessage
                                        ?: "Unknown Firestore error"
                                )
                        }
                }
            }
            .addOnFailureListener { exception ->

                setLoading(false)

                tvAuthMessage.text =
                    getString(
                        R.string.google_profile_load_failed,
                        exception.localizedMessage
                            ?: "Unknown Firestore error"
                    )
            }
    }

    // =========================================================
    // FINISH GOOGLE LOGIN
    // =========================================================

    private fun completeGoogleLogin(
        uid: String,
        fullName: String,
        email: String
    ) {

        setLoading(false)

        saveUserSession(
            uid,
            fullName,
            email
        )

        Toast.makeText(
            this,
            getString(
                R.string.welcome_user,
                fullName
            ),
            Toast.LENGTH_SHORT
        ).show()

        openHome(
            fullName
        )
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
            getString(R.string.sign_in_habithub)

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
            getString(R.string.create_my_account)

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

        if (
            email.isEmpty()
        ) {

            etEmail.error =
                getString(R.string.email_required)

            etEmail.requestFocus()

            return
        }

        if (
            !Patterns.EMAIL_ADDRESS
                .matcher(
                    email
                )
                .matches()
        ) {

            etEmail.error =
                getString(R.string.valid_email)

            etEmail.requestFocus()

            return
        }

        if (
            password.isEmpty()
        ) {

            etPassword.error =
                getString(R.string.password_required)

            etPassword.requestFocus()

            return
        }

        if (
            password.length < 6
        ) {

            etPassword.error =
                getString(R.string.password_minimum)

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

        if (
            fullName.isEmpty()
        ) {

            etFullName.error =
                getString(R.string.full_name_required)

            etFullName.requestFocus()

            return
        }

        if (
            email.isEmpty()
        ) {

            etEmail.error =
                getString(R.string.email_required)

            etEmail.requestFocus()

            return
        }

        if (
            !Patterns.EMAIL_ADDRESS
                .matcher(
                    email
                )
                .matches()
        ) {

            etEmail.error =
                getString(R.string.valid_email)

            etEmail.requestFocus()

            return
        }

        if (
            password.isEmpty()
        ) {

            etPassword.error =
                getString(R.string.password_required)

            etPassword.requestFocus()

            return
        }

        if (
            password.length < 6
        ) {

            etPassword.error =
                getString(R.string.password_minimum)

            etPassword.requestFocus()

            return
        }

        if (
            confirmPassword.isEmpty()
        ) {

            etConfirmPassword.error =
                getString(R.string.confirm_password_required)

            etConfirmPassword.requestFocus()

            return
        }

        if (
            password != confirmPassword
        ) {

            etConfirmPassword.error =
                getString(R.string.passwords_not_match)

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

        setLoading(
            true
        )

        firebaseAuth
            .createUserWithEmailAndPassword(
                email,
                password
            )
            .addOnSuccessListener {

                val firebaseUser =
                    firebaseAuth.currentUser

                if (
                    firebaseUser == null
                ) {

                    setLoading(
                        false
                    )

                    tvAuthMessage.text =
                        getString(R.string.account_not_created)

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
                        "authProvider" to "password",
                        "createdAt" to
                                FieldValue.serverTimestamp()
                    )

                firestore
                    .collection(
                        "users"
                    )
                    .document(
                        uid
                    )
                    .set(
                        userData
                    )
                    .addOnSuccessListener {

                        setLoading(
                            false
                        )

                        saveUserSession(
                            uid,
                            fullName,
                            email
                        )

                        Toast.makeText(
                            this,
                            getString(
                                R.string.account_created_successfully
                            ),
                            Toast.LENGTH_SHORT
                        ).show()

                        openHome(
                            fullName
                        )
                    }
                    .addOnFailureListener { exception ->

                        setLoading(
                            false
                        )

                        tvAuthMessage.text =
                            getString(
                                R.string.profile_setup_failed,
                                exception.localizedMessage
                                    ?: "Unknown Firestore error"
                            )
                    }
            }
            .addOnFailureListener { exception ->

                setLoading(
                    false
                )

                tvAuthMessage.text =
                    firebaseErrorMessage(
                        exception.localizedMessage
                    )
            }
    }

    // =========================================================
    // FIREBASE EMAIL/PASSWORD LOGIN
    // =========================================================

    private fun loginUser(
        email: String,
        password: String
    ) {

        setLoading(
            true
        )

        firebaseAuth
            .signInWithEmailAndPassword(
                email,
                password
            )
            .addOnSuccessListener {

                val firebaseUser =
                    firebaseAuth.currentUser

                if (
                    firebaseUser == null
                ) {

                    setLoading(
                        false
                    )

                    tvAuthMessage.text =
                        getString(R.string.unable_load_account)

                    return@addOnSuccessListener
                }

                val uid =
                    firebaseUser.uid

                firestore
                    .collection(
                        "users"
                    )
                    .document(
                        uid
                    )
                    .get()
                    .addOnSuccessListener { document ->

                        setLoading(
                            false
                        )

                        val fullName =
                            document.getString(
                                "fullName"
                            ) ?: getString(R.string.student)

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
                            getString(
                                R.string.welcome_back_user,
                                fullName
                            ),
                            Toast.LENGTH_SHORT
                        ).show()

                        openHome(
                            fullName
                        )
                    }
                    .addOnFailureListener { exception ->

                        setLoading(
                            false
                        )

                        tvAuthMessage.text =
                            getString(
                                R.string.signed_in_profile_failed,
                                exception.localizedMessage
                                    ?: "Unknown Firestore error"
                            )
                    }
            }
            .addOnFailureListener { exception ->

                setLoading(
                    false
                )

                tvAuthMessage.text =
                    firebaseErrorMessage(
                        exception.localizedMessage
                    )

                Toast.makeText(
                    this,
                    firebaseErrorMessage(
                        exception.localizedMessage
                    ),
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

        if (
            email.isEmpty()
        ) {

            etEmail.error =
                getString(R.string.enter_email_first)

            etEmail.requestFocus()

            return
        }

        if (
            !Patterns.EMAIL_ADDRESS
                .matcher(
                    email
                )
                .matches()
        ) {

            etEmail.error =
                getString(R.string.valid_email)

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
                    getString(
                        R.string.password_reset_sent
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { exception ->

                tvAuthMessage.text =
                    firebaseErrorMessage(
                        exception.localizedMessage
                    )
            }
    }

    // =========================================================
    // SAVE LOCAL SESSION
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
    // FRIENDLY FIREBASE ERRORS
    // =========================================================

    private fun firebaseErrorMessage(
        firebaseMessage: String?
    ): String {

        val message =
            firebaseMessage
                ?: return getString(
                    R.string.authentication_failed
                )

        return when {

            message.contains(
                "email address is already in use",
                ignoreCase = true
            ) ->

                getString(
                    R.string.email_already_exists
                )

            message.contains(
                "badly formatted",
                ignoreCase = true
            ) ->

                getString(
                    R.string.valid_email_address
                )

            message.contains(
                "password",
                ignoreCase = true
            ) ->

                getString(
                    R.string.check_password
                )

            message.contains(
                "credential",
                ignoreCase = true
            ) ->

                getString(
                    R.string.incorrect_email_password
                )

            message.contains(
                "network",
                ignoreCase = true
            ) ->

                getString(
                    R.string.unable_connect
                )

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

        btnGoogle.isEnabled =
            !loading

        tabLogin.isEnabled =
            !loading

        tabSignUp.isEnabled =
            !loading

        if (
            loading
        ) {

            btnAuth.text =
                if (
                    currentMode == "LOGIN"
                ) {

                    getString(
                        R.string.signing_in
                    )

                } else {

                    getString(
                        R.string.creating_account
                    )
                }

        } else {

            btnAuth.text =
                if (
                    currentMode == "LOGIN"
                ) {

                    getString(
                        R.string.sign_in_habithub
                    )

                } else {

                    getString(
                        R.string.create_my_account
                    )
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