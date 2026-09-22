package com.example.habithub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val getStartedButton =
            findViewById<Button>(R.id.btnGetStarted)

        val existingAccountText =
            findViewById<TextView>(R.id.tvExistingAccount)

        getStartedButton.setOnClickListener {

            val intent =
                Intent(this, AuthActivity::class.java)

            intent.putExtra("MODE", "SIGNUP")

            startActivity(intent)
        }

        existingAccountText.setOnClickListener {

            val intent =
                Intent(this, AuthActivity::class.java)

            intent.putExtra("MODE", "LOGIN")

            startActivity(intent)
        }
    }
}