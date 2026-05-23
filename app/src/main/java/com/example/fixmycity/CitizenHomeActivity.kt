package com.example.fixmycity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CitizenHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var layoutIssues: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_citizen_home)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        layoutIssues = findViewById(R.id.layoutIssues)

        // Report Issue button
        findViewById<Button>(R.id.btnReportIssue).setOnClickListener {
            startActivity(Intent(this, ReportIssueActivity::class.java))
        }

        // View All button
        findViewById<TextView>(R.id.tvViewAll).setOnClickListener {
            startActivity(Intent(this, MyIssuesActivity::class.java))
        }

        // Bottom Nav
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { }
        findViewById<LinearLayout>(R.id.navReport).setOnClickListener {
            startActivity(Intent(this, ReportIssueActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navNotification).setOnClickListener { }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Load recent issues
        loadRecentIssues()

        // Search functionality
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnClearSearch = findViewById<TextView>(R.id.btnClearSearch)

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    btnClearSearch.visibility = View.GONE
                    loadRecentIssues()
                } else {
                    btnClearSearch.visibility = View.VISIBLE
                    searchIssues(query)
                }
            }
        })

        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
            btnClearSearch.visibility = View.GONE
            loadRecentIssues()
        }
    }

    private fun loadRecentIssues() {
        val userId = auth.currentUser?.uid ?: return
        layoutIssues.removeAllViews()

        db.collection("issues")
            .whereEqualTo("userId", userId)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                layoutIssues.removeAllViews()

                if (documents.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text =
                        "No issues reported yet!\nTap '+ Report an Issue' to start."
                    emptyText.textSize = 14f
                    emptyText.gravity = android.view.Gravity.CENTER
                    emptyText.setTextColor(
                        android.graphics.Color.parseColor("#888888"))
                    emptyText.setPadding(16, 48, 16, 16)
                    layoutIssues.addView(emptyText)
                } else {
                    for (document in documents) {
                        val category = document.getString("category") ?: ""
                        val location = document.getString("location") ?: ""
                        val status = document.getString("status") ?: "Pending"
                        val timestamp = document.getString("timestamp") ?: ""
                        addIssueCard(category, location, status, timestamp)
                    }
                }
            }
            .addOnFailureListener { e ->
                val errorText = TextView(this)
                errorText.text = "Failed to load: ${e.message}"
                errorText.textSize = 13f
                errorText.setTextColor(
                    android.graphics.Color.parseColor("#D32F2F"))
                layoutIssues.addView(errorText)
            }
    }

    private fun searchIssues(query: String) {
        val userId = auth.currentUser?.uid ?: return
        layoutIssues.removeAllViews()

        db.collection("issues")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                layoutIssues.removeAllViews()
                var found = false

                for (document in documents) {
                    val category = document.getString("category") ?: ""
                    val location = document.getString("location") ?: ""
                    val status = document.getString("status") ?: "Pending"
                    val timestamp = document.getString("timestamp") ?: ""
                    val description = document.getString("description") ?: ""

                    if (category.contains(query, ignoreCase = true) ||
                        location.contains(query, ignoreCase = true) ||
                        description.contains(query, ignoreCase = true)) {
                        addIssueCard(category, location, status, timestamp)
                        found = true
                    }
                }

                if (!found) {
                    val emptyText = TextView(this)
                    emptyText.text = "No issues found for \"$query\""
                    emptyText.textSize = 14f
                    emptyText.gravity = android.view.Gravity.CENTER
                    emptyText.setTextColor(
                        android.graphics.Color.parseColor("#888888"))
                    emptyText.setPadding(16, 48, 16, 16)
                    layoutIssues.addView(emptyText)
                }
            }
    }

    private fun addIssueCard(
        category: String,
        location: String,
        status: String,
        timestamp: String
    ) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(
            R.layout.item_issue, layoutIssues, false)

        cardView.findViewById<TextView>(R.id.tvCategory).text = category
        cardView.findViewById<TextView>(R.id.tvLocation).text = "📍 $location"
        cardView.findViewById<TextView>(R.id.tvDescription).visibility = View.GONE
        cardView.findViewById<TextView>(R.id.tvTimestamp).text = timestamp

        // Hide edit/delete buttons on home screen
        cardView.findViewById<Button>(R.id.btnEdit).visibility = View.GONE
        cardView.findViewById<Button>(R.id.btnDelete).visibility = View.GONE

        // Status badge color
        val tvStatus = cardView.findViewById<TextView>(R.id.tvStatus)
        tvStatus.text = status
        when (status) {
            "Pending" -> tvStatus.setBackgroundColor(
                android.graphics.Color.parseColor("#F7931E"))
            "In Progress" -> tvStatus.setBackgroundColor(
                android.graphics.Color.parseColor("#3A86FF"))
            "Resolved" -> tvStatus.setBackgroundColor(
                android.graphics.Color.parseColor("#1E6F43"))
        }

        layoutIssues.addView(cardView)
    }
}