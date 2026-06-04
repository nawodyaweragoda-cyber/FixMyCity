package com.example.fixmycity

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import java.text.SimpleDateFormat
import java.util.*

class ReportIssueActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imgIssue: ImageView
    private lateinit var layoutAddPhoto: LinearLayout

    private var cameraImageUri: Uri? = null
    private var selectedImageUri: Uri? = null

    // Gallery Picker
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        uri?.let {

            selectedImageUri = it

            imgIssue.setImageURI(it)

            imgIssue.scaleType =
                ImageView.ScaleType.CENTER_CROP

            layoutAddPhoto.visibility = View.GONE
        }
    }

    // Camera Launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->

        if (success) {

            selectedImageUri = cameraImageUri

            imgIssue.setImageURI(cameraImageUri)

            imgIssue.scaleType =
                ImageView.ScaleType.CENTER_CROP

            layoutAddPhoto.visibility = View.GONE
        }
    }

    // Camera Permission
    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                openCamera()

            } else {

                showError("Camera permission denied!")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_issue)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        imgIssue = findViewById(R.id.imgIssue)
        layoutAddPhoto = findViewById(R.id.layoutAddPhoto)

        // Open image picker
        layoutAddPhoto.setOnClickListener {

            showImagePickerDialog()
        }

        imgIssue.setOnClickListener {

            showImagePickerDialog()
        }

        // Category Spinner
        val spinnerCategory =
            findViewById<Spinner>(R.id.spinnerCategory)

        val categories = listOf(
            "Select Category",
            "Road Issue",
            "Garbage",
            "Street Light",
            "Water Supply",
            "Drainage",
            "Public Property",
            "Other"
        )

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerCategory.adapter = categoryAdapter

        // Priority Spinner
        val spinnerPriority =
            findViewById<Spinner>(R.id.spinnerPriority)

        val priorities = listOf(
            "Select Priority",
            "Low",
            "Medium",
            "High"
        )

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            priorities
        )

        priorityAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerPriority.adapter = priorityAdapter

        // Back Button
        findViewById<TextView>(R.id.btnBack)
            .setOnClickListener {

                finish()
            }

        // Cancel Button
        findViewById<Button>(R.id.btnCancel)
            .setOnClickListener {

                finish()
            }

        // Submit Button
        findViewById<Button>(R.id.btnSubmit)
            .setOnClickListener {

                submitReport()
            }
    }

    private fun submitReport() {

        val category =
            findViewById<Spinner>(R.id.spinnerCategory)
                .selectedItem.toString()

        val location =
            findViewById<EditText>(R.id.etLocation)
                .text.toString().trim()

        val description =
            findViewById<EditText>(R.id.etDescription)
                .text.toString().trim()

        val priority =
            findViewById<Spinner>(R.id.spinnerPriority)
                .selectedItem.toString()

        // Validation
        if (category == "Select Category") {

            showError("Please select category")
            return
        }

        if (location.isEmpty()) {

            showError("Please enter location")
            return
        }

        if (description.isEmpty()) {

            showError("Please enter description")
            return
        }

        if (priority == "Select Priority") {

            showError("Please select priority")
            return
        }

        val btnSubmit =
            findViewById<Button>(R.id.btnSubmit)

        btnSubmit.isEnabled = false
        btnSubmit.text = "Submitting..."

        val userId =
            auth.currentUser?.uid ?: ""

        val timestamp =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        // Save Image Locally
        var localPhotoPath = ""

        selectedImageUri?.let { uri ->

            val path = saveImageLocally(uri)

            if (path != null) {

                localPhotoPath = path
            }
        }

        // Firestore Data
        val issue = hashMapOf(

            "userId" to userId,

            "category" to category,

            "location" to location,

            "description" to description,

            "priority" to priority,

            "status" to "Pending",

            "timestamp" to timestamp,

            "localPhotoPath" to localPhotoPath
        )

        db.collection("issues")
            .add(issue)

            .addOnSuccessListener {

                btnSubmit.isEnabled = true

                btnSubmit.text = "Submit Report"

                showSuccess(
                    "Issue reported successfully!"
                )

                clearForm()
            }

            .addOnFailureListener { e ->

                btnSubmit.isEnabled = true

                btnSubmit.text = "Submit Report"

                showError(
                    "Failed: ${e.message}"
                )
            }
    }

    private fun clearForm() {

        findViewById<EditText>(R.id.etLocation)
            .text.clear()

        findViewById<EditText>(R.id.etDescription)
            .text.clear()

        findViewById<Spinner>(R.id.spinnerCategory)
            .setSelection(0)

        findViewById<Spinner>(R.id.spinnerPriority)
            .setSelection(0)

        imgIssue.setImageDrawable(null)

        layoutAddPhoto.visibility = View.VISIBLE

        selectedImageUri = null
    }

    private fun saveImageLocally(uri: Uri): String? {

        return try {

            val inputStream =
                contentResolver.openInputStream(uri)

            val file = File(
                filesDir,
                "issue_${System.currentTimeMillis()}.jpg"
            )

            val outputStream = file.outputStream()

            inputStream?.copyTo(outputStream)

            inputStream?.close()

            outputStream.close()

            file.absolutePath

        } catch (e: Exception) {

            null
        }
    }

    private fun showImagePickerDialog() {

        val options = arrayOf(
            "📷 Take Photo",
            "🖼️ Choose from Gallery",
            "Cancel"
        )

        AlertDialog.Builder(this)
            .setTitle("Add Issue Photo")

            .setItems(options) { dialog, which ->

                when (which) {

                    0 -> checkCameraPermission()

                    1 -> galleryLauncher.launch("image/*")

                    2 -> dialog.dismiss()
                }
            }

            .show()
    }

    private fun checkCameraPermission() {

        when {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {

                openCamera()
            }

            else -> {

                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun openCamera() {

        val photoFile = File(
            cacheDir,
            "issue_${System.currentTimeMillis()}.jpg"
        )

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        cameraImageUri = uri

        cameraLauncher.launch(uri)
    }

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

        snackbar.show()
    }

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