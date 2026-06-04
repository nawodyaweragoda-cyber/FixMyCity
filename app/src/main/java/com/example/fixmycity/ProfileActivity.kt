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

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etCity: EditText
    private lateinit var imgProfile: ImageView

    private var cameraImageUri: Uri? = null
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etCity = findViewById(R.id.etCity)
        imgProfile = findViewById(R.id.imgProfile)

        imgProfile.setOnClickListener {
            showImagePickerDialog()
        }

        loadUserData()

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveUserData()
        }

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        findViewById<Button>(R.id.btnDeleteAccount)
            .setOnClickListener {

                val user = FirebaseAuth.getInstance().currentUser

                AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account?")
                    .setPositiveButton("Yes") { _, _ ->

                        user?.delete()
                            ?.addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Account Deleted",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // go to main screen
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            ?.addOnFailureListener {

                                Toast.makeText(
                                    this,
                                    "Delete Failed (login again required)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

        // NAVIGATION
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, CitizenHomeActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navReport).setOnClickListener {
            startActivity(Intent(this, ReportIssueActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navNotification).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            finish()
        }
        // PROFILE
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {


        }
    }


    // ================= IMAGE PICK =================

    private fun showImagePickerDialog() {
        val options = arrayOf("📷 Take Photo", "🖼️ Gallery", "Cancel")

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

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                imgProfile.setImageURI(it)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    selectedImageUri = uri
                    imgProfile.setImageURI(uri)
                }
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else showError("Camera permission denied")
        }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val file = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        // ✅ FIX (NO CRASH)
        cameraImageUri?.let {
            cameraLauncher.launch(it)
        }
    }

    // ================= FIRESTORE =================

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                etName.setText(doc.getString("fullName"))
                etEmail.setText(doc.getString("email"))
                etCity.setText(doc.getString("city"))
            }
    }

    private fun saveUserData() {
        val uid = auth.currentUser?.uid ?: return

        val data = hashMapOf(
            "fullName" to etName.text.toString(),
            "email" to etEmail.text.toString(),
            "city" to etCity.text.toString()
        )

        db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener {
                showSuccess("Profile saved")
            }
            .addOnFailureListener {
                showError("Save failed")
            }
    }

    // ================= UI =================

    private fun showError(msg: String) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(msg: String) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }
}