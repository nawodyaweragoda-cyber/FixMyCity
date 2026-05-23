package com.example.fixmycity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyIssuesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var layoutIssues: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_issues)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        layoutIssues = findViewById(R.id.layoutIssues)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        scrollView = findViewById(R.id.scrollView)
        progressBar = findViewById(R.id.progressBar)

        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Add new button
        findViewById<TextView>(R.id.btnAddNew).setOnClickListener {
            startActivity(Intent(this, ReportIssueActivity::class.java))
        }

        // Report first button
        findViewById<Button>(R.id.btnReportFirst).setOnClickListener {
            startActivity(Intent(this, ReportIssueActivity::class.java))
        }

        // Load issues
        loadMyIssues()
    }

    private fun loadMyIssues() {
        val userId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        scrollView.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        layoutIssues.removeAllViews()

        db.collection("issues")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    layoutEmpty.visibility = View.VISIBLE
                    scrollView.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    scrollView.visibility = View.VISIBLE

                    for (document in documents) {
                        val issueId = document.id
                        val category = document.getString("category") ?: ""
                        val location = document.getString("location") ?: ""
                        val description = document.getString("description") ?: ""
                        val status = document.getString("status") ?: "Pending"
                        val timestamp = document.getString("timestamp") ?: ""
                        val priority = document.getString("priority") ?: ""

                        addIssueCard(
                            issueId, category, location,
                            description, status, timestamp, priority
                        )
                    }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                showError("Failed to load issues!")
            }
    }

    private fun addIssueCard(
        issueId: String,
        category: String,
        location: String,
        description: String,
        status: String,
        timestamp: String,
        priority: String
    ) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.item_issue, layoutIssues, false)

        cardView.findViewById<TextView>(R.id.tvCategory).text = category
        cardView.findViewById<TextView>(R.id.tvLocation).text = "📍 $location"
        cardView.findViewById<TextView>(R.id.tvDescription).text = description
        cardView.findViewById<TextView>(R.id.tvTimestamp).text = timestamp

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

        // Edit button
        cardView.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            showEditDialog(issueId, category, location, description, priority)
        }

        // Delete button
        cardView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            showDeleteDialog(issueId, cardView)
        }

        layoutIssues.addView(cardView)
    }

    private fun showEditDialog(
        issueId: String,
        category: String,
        location: String,
        description: String,
        priority: String
    ) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val etLocation = EditText(this).apply {
            hint = "Location"
            setText(location)
            textSize = 14f
        }

        val etDescription = EditText(this).apply {
            hint = "Description"
            setText(description)
            textSize = 14f
            minLines = 3
            inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        dialogView.addView(TextView(this).apply {
            text = "Location"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#1E6F43"))
        })
        dialogView.addView(etLocation)

        dialogView.addView(TextView(this).apply {
            text = "Description"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#1E6F43"))
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = 16
        })
        dialogView.addView(etDescription)

        AlertDialog.Builder(this)
            .setTitle("Edit Issue")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newLocation = etLocation.text.toString().trim()
                val newDescription = etDescription.text.toString().trim()

                if (newLocation.isEmpty() || newDescription.isEmpty()) {
                    showError("Please fill all fields!")
                    return@setPositiveButton
                }

                updateIssue(issueId, newLocation, newDescription)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateIssue(
        issueId: String,
        location: String,
        description: String
    ) {
        db.collection("issues").document(issueId)
            .update(
                mapOf(
                    "location" to location,
                    "description" to description
                )
            )
            .addOnSuccessListener {
                showSuccess("Issue updated successfully! ✅")
                loadMyIssues()
            }
            .addOnFailureListener {
                showError("Failed to update issue!")
            }
    }

    private fun showDeleteDialog(issueId: String, cardView: View) {
        AlertDialog.Builder(this)
            .setTitle("Delete Issue")
            .setMessage("Are you sure you want to delete this issue? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteIssue(issueId, cardView)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteIssue(issueId: String, cardView: View) {
        db.collection("issues").document(issueId)
            .delete()
            .addOnSuccessListener {
                layoutIssues.removeView(cardView)
                showSuccess("Issue deleted successfully!")
                if (layoutIssues.childCount == 0) {
                    scrollView.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                showError("Failed to delete issue!")
            }
    }

    private fun showError(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(
            android.graphics.Color.parseColor("#D32F2F"))
        snackbar.setTextColor(android.graphics.Color.WHITE)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    private fun showSuccess(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(
            android.graphics.Color.parseColor("#1E6F43"))
        snackbar.setTextColor(android.graphics.Color.WHITE)
        snackbar.show()
    }
}