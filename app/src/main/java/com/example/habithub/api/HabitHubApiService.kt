package com.example.habithub.api

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface HabitHubApiService {

    /*
     * Reads the signed-in user's habits using
     * the official Cloud Firestore REST API.
     *
     * Example:
     *
     * GET
     * v1/projects/habithub-2dd33/databases/(default)/
     * documents/users/{userId}/habits
     */
    @GET(
        "v1/projects/habithub-2dd33/databases/(default)/documents/users/{userId}/habits"
    )
    fun getHabitsViaRest(
        @Path("userId") userId: String,
        @Header("Authorization") authorization: String
    ): Call<JsonObject>
}