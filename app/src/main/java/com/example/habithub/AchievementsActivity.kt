package com.example.habithub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AchievementsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_achievements
        )

        val tvBack =
            findViewById<TextView>(
                R.id.tvBack
            )

        val btnProfile =
            findViewById<Button>(
                R.id.btnProfile
            )

        tvBack.setOnClickListener {
            finish()
        }

        btnProfile.setOnClickListener {

            val intent =
                Intent(
                    this,
                    ProfileActivity::class.java
                )

            startActivity(intent)
        }
    }
}