package com.example.habithub.api

data class AuthResponse(
    val message: String,
    val user: UserResponse?
)