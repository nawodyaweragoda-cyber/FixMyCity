package com.example.fixmycity

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Role Spinner
        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)

        val roles = listOf(
            "Select Role",
            "User",
            "Admin"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerRole.adapter = adapter

        // Login Button
        findViewById<Button>(R.id.btnLogin)
            .setOnClickListener {

                loginUser()
            }

        // Sign Up Link
        findViewById<TextView>(R.id.tvSignUp)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        SignupActivity::class.java
                    )
                )
            }
    }

    private fun loginUser() {

        val email = findViewById<EditText>(
            R.id.etEmail
        ).text.toString().trim()

        val password = findViewById<EditText>(
            R.id.etPassword
        ).text.toString().trim()

        val selectedRole = findViewById<Spinner>(
            R.id.spinnerRole
        ).selectedItem.toString()

        // Validation

        if (email.isEmpty() || password.isEmpty()) {

            showError("Please enter email and password!")
            return
        }

        if (selectedRole == "Select Role") {

            showError("Please select a role!")
            return
        }

        // Firebase Login

        auth.signInWithEmailAndPassword(email, password)

            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val userId = auth.currentUser?.uid

                    db.collection("users")
                        .document(userId!!)
                        .get()

                        .addOnSuccessListener { document ->

                            val userRole =
                                document.getString("role")

                            if (userRole == selectedRole) {

                                showSuccess("Welcome back! 👋")

                                // ADMIN LOGIN

                                if (userRole == "Admin") {

                                    startActivity(
                                        Intent(
                                            this,
                                            AdminDashboardActivity::class.java
                                        )
                                    )

                                    finish()

                                }

                                // USER LOGIN

                                else {

                                    startActivity(
                                        Intent(
                                            this,
                                            CitizenHomeActivity::class.java
                                        )
                                    )

                                    finish()
                                }

                            } else {

                                showError(
                                    "Wrong role selected! Please check your role."
                                )

                                auth.signOut()
                            }
                        }

                        .addOnFailureListener {

                            showError(
                                "Error fetching user data. Try again!"
                            )
                        }

                } else {

                    showError(
                        "Login failed: ${task.exception?.message}"
                    )
                }
            }
    }

    // ERROR MESSAGE

    private fun showError(message: String) {

        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )

        snackbar.setBackgroundTint(
            android.graphics.Color.parseColor("#D32F2F")
        )

        snackbar.setTextColor(
            android.graphics.Color.WHITE
        )

        snackbar.setAction("OK") {

            snackbar.dismiss()
        }

        snackbar.show()
    }

    // SUCCESS MESSAGE

    private fun showSuccess(message: String) {

        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )

        snackbar.setBackgroundTint(
            android.graphics.Color.parseColor("#1E6F43")
        )

        snackbar.setTextColor(
            android.graphics.Color.WHITE
        )

        snackbar.show()
    }
}