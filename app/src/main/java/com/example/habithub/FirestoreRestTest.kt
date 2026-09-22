package com.example.habithub

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.habithub.api.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FirestoreRestTest(
    private val context: Context
) {

    fun testHabitsRestApi() {

        val user =
            FirebaseAuth.getInstance().currentUser

        if (user == null) {

            Toast.makeText(
                context,
                "REST TEST: No signed-in user",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // Firebase gives us an ID token.
        // We use this token to authenticate the REST request.
        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->

                val idToken =
                    tokenResult.token

                if (idToken.isNullOrBlank()) {

                    Toast.makeText(
                        context,
                        "REST TEST: Could not get Firebase token",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val authorizationHeader =
                    "Bearer $idToken"

                // Actual Retrofit REST request
                ApiClient.apiService
                    .getHabitsViaRest(
                        user.uid,
                        authorizationHeader
                    )
                    .enqueue(
                        object : Callback<JsonObject> {

                            override fun onResponse(
                                call: Call<JsonObject>,
                                response: Response<JsonObject>
                            ) {

                                if (response.isSuccessful) {

                                    val responseBody =
                                        response.body()

                                    Log.d(
                                        "HABITHUB_REST",
                                        "REST SUCCESS: $responseBody"
                                    )

                                    Toast.makeText(
                                        context,
                                        "REST API SUCCESS ✓",
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else {

                                    val error =
                                        response.errorBody()
                                            ?.string()
                                            ?: "Unknown REST error"

                                    Log.e(
                                        "HABITHUB_REST",
                                        "HTTP ${response.code()}: $error"
                                    )

                                    Toast.makeText(
                                        context,
                                        "REST API ERROR: HTTP ${response.code()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            override fun onFailure(
                                call: Call<JsonObject>,
                                throwable: Throwable
                            ) {

                                Log.e(
                                    "HABITHUB_REST",
                                    "REST FAILURE",
                                    throwable
                                )

                                Toast.makeText(
                                    context,
                                    "REST FAILED: ${
                                        throwable.localizedMessage
                                            ?: "Unknown network error"
                                    }",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
            }
            .addOnFailureListener { exception ->

                Log.e(
                    "HABITHUB_REST",
                    "TOKEN FAILURE",
                    exception
                )

                Toast.makeText(
                    context,
                    "Could not authenticate REST request.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}