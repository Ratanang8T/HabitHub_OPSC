package com.example.habithub.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Official Google Cloud Firestore REST API
    private const val BASE_URL =
        "https://firestore.googleapis.com/"

    val apiService: HabitHubApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(
                HabitHubApiService::class.java
            )
    }
}