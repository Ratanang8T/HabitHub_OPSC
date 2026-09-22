package com.example.habithub

import com.example.habithub.api.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FirestoreRestRepository {

    private val firebaseAuth =
        FirebaseAuth.getInstance()

    // =========================================================
    // GET HABITS USING RETROFIT + FIRESTORE REST API
    // =========================================================

    fun getHabits(
        onSuccess: (List<Habit>) -> Unit,
        onError: (String) -> Unit
    ) {

        val user =
            firebaseAuth.currentUser

        if (user == null) {

            onError(
                "Your session has ended. Please log in again."
            )

            return
        }

        // =====================================================
        // FIRST ATTEMPT
        //
        // Firebase can use its normal cached token here.
        // If Firestore says the token is unauthorized,
        // we force-refresh it and retry once.
        // =====================================================

        getTokenAndRequest(
            user = user,
            forceRefresh = false,
            allowRetry = true,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    // =========================================================
    // GET FIREBASE TOKEN
    // =========================================================

    private fun getTokenAndRequest(
        user: FirebaseUser,
        forceRefresh: Boolean,
        allowRetry: Boolean,
        onSuccess: (List<Habit>) -> Unit,
        onError: (String) -> Unit
    ) {

        user
            .getIdToken(
                forceRefresh
            )
            .addOnSuccessListener { tokenResult ->

                val token =
                    tokenResult.token

                if (
                    token.isNullOrBlank()
                ) {

                    onError(
                        "Could not get authentication token."
                    )

                    return@addOnSuccessListener
                }

                makeRestRequest(
                    user = user,
                    token = token,
                    allowRetry = allowRetry,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
            .addOnFailureListener { exception ->

                // =================================================
                // If normal token retrieval failed, try once more
                // with a forced refresh.
                // =================================================

                if (
                    !forceRefresh &&
                    allowRetry
                ) {

                    getTokenAndRequest(
                        user = user,
                        forceRefresh = true,
                        allowRetry = false,
                        onSuccess = onSuccess,
                        onError = onError
                    )

                } else {

                    onError(
                        exception.localizedMessage
                            ?: "Could not refresh authentication."
                    )
                }
            }
    }

    // =========================================================
    // RETROFIT REQUEST
    // =========================================================

    private fun makeRestRequest(
        user: FirebaseUser,
        token: String,
        allowRetry: Boolean,
        onSuccess: (List<Habit>) -> Unit,
        onError: (String) -> Unit
    ) {

        val authorization =
            "Bearer $token"

        ApiClient.apiService
            .getHabitsViaRest(
                user.uid,
                authorization
            )
            .enqueue(

                object :
                    Callback<JsonObject> {

                    override fun onResponse(
                        call: Call<JsonObject>,
                        response: Response<JsonObject>
                    ) {

                        // =========================================
                        // TOKEN EXPIRED / UNAUTHORIZED
                        //
                        // Force Firebase to obtain a fresh token,
                        // then retry the REST request once.
                        // =========================================

                        if (
                            response.code() == 401 &&
                            allowRetry
                        ) {

                            getTokenAndRequest(
                                user = user,
                                forceRefresh = true,
                                allowRetry = false,
                                onSuccess = onSuccess,
                                onError = onError
                            )

                            return
                        }

                        // =========================================
                        // OTHER REST ERROR
                        // =========================================

                        if (
                            !response.isSuccessful
                        ) {

                            val errorMessage =
                                response
                                    .errorBody()
                                    ?.string()
                                    ?: "Unknown REST API error."

                            onError(
                                "HTTP ${response.code()}: $errorMessage"
                            )

                            return
                        }

                        // =========================================
                        // SUCCESS
                        // =========================================

                        val body =
                            response.body()

                        if (
                            body == null
                        ) {

                            onSuccess(
                                emptyList()
                            )

                            return
                        }

                        try {

                            val habits =
                                parseHabits(
                                    body
                                )

                            onSuccess(
                                habits
                            )

                        } catch (
                            exception: Exception
                        ) {

                            onError(
                                "Could not read REST response: ${
                                    exception.localizedMessage
                                        ?: "Unknown parsing error"
                                }"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<JsonObject>,
                        throwable: Throwable
                    ) {

                        onError(
                            throwable.localizedMessage
                                ?: "REST API request failed."
                        )
                    }
                }
            )
    }

    // =========================================================
    // CONVERT FIRESTORE REST JSON INTO HABIT OBJECTS
    // =========================================================

    private fun parseHabits(
        response: JsonObject
    ): List<Habit> {

        val habits =
            mutableListOf<Habit>()

        val documents =
            response.getAsJsonArray(
                "documents"
            )
                ?: return emptyList()

        for (
        element in documents
        ) {

            val document =
                element.asJsonObject

            val documentName =
                document
                    .get(
                        "name"
                    )
                    ?.asString
                    ?: ""

            // Firestore returns:
            //
            // projects/.../documents/users/UID/habits/HABIT_ID
            //
            // We only need HABIT_ID.

            val habitId =
                documentName
                    .substringAfterLast(
                        "/"
                    )

            val fields =
                document
                    .getAsJsonObject(
                        "fields"
                    )
                    ?: continue

            val habit =
                Habit(

                    id =
                        habitId,

                    name =
                        getString(
                            fields,
                            "name"
                        ),

                    category =
                        getString(
                            fields,
                            "category"
                        ),

                    reminderTime =
                        getString(
                            fields,
                            "reminderTime"
                        ),

                    goal =
                        getString(
                            fields,
                            "goal"
                        ),

                    unit =
                        getString(
                            fields,
                            "unit"
                        ),

                    notes =
                        getString(
                            fields,
                            "notes"
                        ),

                    frequency =
                        getString(
                            fields,
                            "frequency"
                        )
                            .ifBlank {
                                "Daily"
                            },

                    completed =
                        getBoolean(
                            fields,
                            "completed"
                        ),

                    paused =
                        getBoolean(
                            fields,
                            "paused"
                        ),

                    createdAt =
                        getLong(
                            fields,
                            "createdAt"
                        )
                )

            habits.add(
                habit
            )
        }

        return habits
            .sortedByDescending {
                it.createdAt
            }
    }

    // =========================================================
    // FIRESTORE REST STRING
    // =========================================================

    private fun getString(
        fields: JsonObject,
        fieldName: String
    ): String {

        val field =
            fields
                .getAsJsonObject(
                    fieldName
                )
                ?: return ""

        return field
            .get(
                "stringValue"
            )
            ?.asString
            ?: ""
    }

    // =========================================================
    // FIRESTORE REST BOOLEAN
    // =========================================================

    private fun getBoolean(
        fields: JsonObject,
        fieldName: String
    ): Boolean {

        val field =
            fields
                .getAsJsonObject(
                    fieldName
                )
                ?: return false

        return field
            .get(
                "booleanValue"
            )
            ?.asBoolean
            ?: false
    }

    // =========================================================
    // FIRESTORE REST INTEGER
    // =========================================================

    private fun getLong(
        fields: JsonObject,
        fieldName: String
    ): Long {

        val field =
            fields
                .getAsJsonObject(
                    fieldName
                )
                ?: return 0L

        val value =
            field.get(
                "integerValue"
            )
                ?: return 0L

        return try {

            value
                .asString
                .toLong()

        } catch (
            exception: Exception
        ) {

            0L
        }
    }
}