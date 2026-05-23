package com.example.fixmycity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class SignupActivity : AppCompatActivity() {

    private lateinit var imgPhoto: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var cameraImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imgPhoto.setImageURI(it)
            imgPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imgPhoto.setImageURI(cameraImageUri)
            imgPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else showError("Camera permission denied!")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val roles = listOf("Select Role", "User", "Admin")
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        imgPhoto = findViewById(R.id.imgPhoto)
        imgPhoto.setOnClickListener { showImagePickerDialog() }

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            registerUser()
        }

        findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val fullName = findViewById<EditText>(R.id.etFullName).text.toString().trim()
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPassword).text.toString().trim()
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword).text.toString().trim()
        val city = findViewById<EditText>(R.id.etCity).text.toString().trim()
        val role = findViewById<Spinner>(R.id.spinnerRole).selectedItem.toString()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || city.isEmpty()) {
            showError("Please fill all fields!")
            return
        }
        if (password != confirmPassword) {
            showError("Passwords do not match!")
            return
        }
        if (role == "Select Role") {
            showError("Please select a role!")
            return
        }
        if (password.length < 6) {
            showError("Password must be at least 6 characters!")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val user = hashMapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "city" to city,
                        "role" to role,
                        "uid" to userId
                    )
                    db.collection("users").document(userId!!)
                        .set(user)
                        .addOnSuccessListener {
                            showSuccess("Account created successfully! 🎉")
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showError("Error: ${e.message}")
                        }
                } else {
                    showError("Signup failed: ${task.exception?.message}")
                }
            }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("📷  Take Photo", "🖼️  Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Add Profile Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> galleryLauncher.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }.show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> openCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val photoFile = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", photoFile)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun showError(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(android.graphics.Color.parseColor("#D32F2F"))
        snackbar.setTextColor(android.graphics.Color.WHITE)
        snackbar.setActionTextColor(android.graphics.Color.WHITE)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    private fun showSuccess(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(android.graphics.Color.parseColor("#1E6F43"))
        snackbar.setTextColor(android.graphics.Color.WHITE)
        snackbar.show()
    }
}