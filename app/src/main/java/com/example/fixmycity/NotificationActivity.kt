package com.example.fixmycity

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // BACK BUTTON
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // HOME NAVIGATION
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, CitizenHomeActivity::class.java))
            finish()
        }

        // PROFILE NAVIGATION
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}