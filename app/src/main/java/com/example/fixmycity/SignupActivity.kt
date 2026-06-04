package com.example.fixmycity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val etCity = findViewById<EditText>(R.id.etCity)

        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val imgPhoto = findViewById<ImageView>(R.id.imgPhoto)

        // Role Spinner setup
        val roles = listOf("Select Role", "User", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // Login redirect
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Photo click (optional simple)
        imgPhoto.setOnClickListener {
            Toast.makeText(this, "Image upload not added yet", Toast.LENGTH_SHORT).show()
        }

        // Signup button
        btnSubmit.setOnClickListener {

            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val city = etCity.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()

            // Validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password not matching", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "Select Role") {
                Toast.makeText(this, "Select Role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase Auth Signup
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val uid = auth.currentUser?.uid

                        val userMap = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role,
                            "city" to city,
                            "uid" to uid
                        )

                        // Firestore save
                        db.collection("users")
                            .document(uid!!)
                            .set(userMap)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Signup Success",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Go to login
                                startActivity(
                                    Intent(
                                        this,
                                        LoginActivity::class.java
                                    )
                                )

                                finish()
                            }
                            .addOnFailureListener {

                                Toast.makeText(
                                    this,
                                    "Firestore error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                    } else {

                        Toast.makeText(
                            this,
                            task.exception?.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}