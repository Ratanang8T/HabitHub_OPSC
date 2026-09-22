package com.example.habithub.api

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String
)