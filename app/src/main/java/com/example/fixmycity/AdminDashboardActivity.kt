package com.example.fixmycity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var layoutIssues: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        db = FirebaseFirestore.getInstance()

        layoutIssues = findViewById(R.id.layoutIssues)

        // 💳 PAYMENT ICON CLICK
        val navPayment = findViewById<LinearLayout>(R.id.navPayment)

        navPayment.setOnClickListener {

            val intent = Intent(this, MoneyActivity::class.java)

            startActivity(intent)
        }

        // 👤 PROFILE ICON CLICK
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        navProfile.setOnClickListener {

            val intent = Intent(this, ProfileActivity::class.java)

            startActivity(intent)
        }

        // 🏠 HOME CLICK
        val navHome = findViewById<LinearLayout>(R.id.navHome)

        navHome.setOnClickListener {

            Toast.makeText(
                this,
                "Already in Home",
                Toast.LENGTH_SHORT
            ).show()
        }

        loadIssues()
    }

    private fun loadIssues() {

        db.collection("issues")
            .get()
            .addOnSuccessListener { documents ->

                layoutIssues.removeAllViews()

                var pending = 0
                var progress = 0
                var resolved = 0

                for (doc in documents) {

                    val issueId = doc.id

                    val category = doc.getString("category") ?: ""
                    val location = doc.getString("location") ?: ""
                    val status = doc.getString("status") ?: "Pending"

                    when (status) {

                        "Pending" -> pending++

                        "In Progress" -> progress++

                        "Resolved" -> resolved++
                    }

                    val card = LayoutInflater.from(this)
                        .inflate(
                            R.layout.item_admin_issue,
                            layoutIssues,
                            false
                        )

                    card.findViewById<TextView>(R.id.tvCategory).text =
                        category

                    card.findViewById<TextView>(R.id.tvLocation).text =
                        location

                    card.findViewById<TextView>(R.id.tvStatus).text =
                        status

                    card.findViewById<Button>(R.id.btnProgress)
                        .setOnClickListener {

                            updateStatus(
                                issueId,
                                "In Progress"
                            )
                        }

                    card.findViewById<Button>(R.id.btnResolved)
                        .setOnClickListener {

                            updateStatus(
                                issueId,
                                "Resolved"
                            )
                        }
                    card.findViewById<Button>(R.id.btnComment)
                        .setOnClickListener {

                            val response = hashMapOf(
                                "adminMessage" to "Issue will be fixed soon"
                            )

                            db.collection("issues")
                                .document(issueId)
                                .collection("responses")
                                .add(response)

                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Response Added",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    card.findViewById<Button>(R.id.btnDelete)
                        .setOnClickListener {

                            db.collection("issues")
                                .document(issueId)
                                .delete()

                                .addOnSuccessListener {

                                    Toast.makeText(
                                        this,
                                        "Issue Deleted",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    loadIssues()
                                }

                                .addOnFailureListener {

                                    Toast.makeText(
                                        this,
                                        "Delete Failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                    layoutIssues.addView(card)
                }

                findViewById<TextView>(R.id.tvPending).text =
                    pending.toString()

                findViewById<TextView>(R.id.tvProgress).text =
                    progress.toString()

                findViewById<TextView>(R.id.tvResolved).text =
                    resolved.toString()
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to load issues",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateStatus(
        issueId: String,
        status: String
    ) {

        db.collection("issues")
            .document(issueId)
            .update("status", status)

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Status Updated",
                    Toast.LENGTH_SHORT
                ).show()

                loadIssues()
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Update Failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}