package com.example.fixmycity

import android.Manifest
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

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etCity: EditText
    private lateinit var imgProfile: ImageView

    private var cameraImageUri: Uri? = null
    private var selectedImageUri: Uri? = null

    // Gallery picker
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            imgProfile.setImageURI(it)
            imgProfile.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = cameraImageUri
            imgProfile.setImageURI(cameraImageUri)
            imgProfile.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    // Camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else showError("Camera permission denied!")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etCity = findViewById(R.id.etCity)
        imgProfile = findViewById(R.id.imgProfile)

        // Click profile image to change
        imgProfile.setOnClickListener {
            showImagePickerDialog()
        }

        // Load user data from Firestore
        loadUserData()

        // Save button
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveUserData()
        }

        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Logout
        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(android.content.Intent(this, CitizenHomeActivity::class.java))
            finish()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("📷  Take Photo", "🖼️  Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Change Profile Photo")
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
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> openCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val photoFile = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", photoFile)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etName.setText(document.getString("fullName") ?: "")
                    etEmail.setText(document.getString("email") ?: "")
                    etCity.setText(document.getString("city") ?: "")

                    // Load saved local photo path
                    val localPath = document.getString("localPhotoPath")
                    if (!localPath.isNullOrEmpty()) {
                        val file = File(localPath)
                        if (file.exists()) {
                            imgProfile.setImageURI(Uri.fromFile(file))
                            imgProfile.scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    }
                }
            }
            .addOnFailureListener {
                showError("Failed to load profile!")
            }
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid ?: return

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val city = etCity.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || city.isEmpty()) {
            showError("Please fill all fields!")
            return
        }

        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val updates = hashMapOf<String, Any>(
            "fullName" to name,
            "email" to email,
            "city" to city
        )

        // Save local photo path to Firestore
        selectedImageUri?.let { uri ->
            val savedPath = saveImageLocally(uri)
            if (savedPath != null) {
                updates["localPhotoPath"] = savedPath
            }
        }

        db.collection("users").document(userId)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                btnSave.isEnabled = true
                btnSave.text = "Save"
                showSuccess("Profile updated successfully! ✅")
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                btnSave.text = "Save"
                showError("Failed to update: ${e.message}")
            }
    }

    private fun saveImageLocally(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_photo.jpg")
            val outputStream = file.outputStream()
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showError(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message, Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(android.graphics.Color.parseColor("#D32F2F"))
        snackbar.setTextColor(android.graphics.Color.WHITE)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    private fun showSuccess(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message, Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(android.graphics.Color.parseColor("#1E6F43"))
        snackbar.setTextColor(android.graphics.Color.WHITE)
        snackbar.show()
    }
}