package com.example.fixmycity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var layoutHistory: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        db = FirebaseFirestore.getInstance()

        // CONNECT XML IDS
        layoutHistory = findViewById(R.id.layoutHistory)

        val btnBack = findViewById<TextView>(R.id.btnBack)

        // BACK BUTTON
        btnBack.setOnClickListener {
            finish()
        }

        // LOAD TRANSACTIONS
        loadTransactions()
    }

    private fun loadTransactions() {

        val userId = FirebaseAuth.getInstance()
            .currentUser?.uid ?: return

        db.collection("wallet")
            .document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()

            .addOnSuccessListener { documents ->

                // CLEAR OLD DATA
                layoutHistory.removeAllViews()

                // LOOP TRANSACTIONS
                for (doc in documents) {

                    // OPEN ITEM LAYOUT
                    val view: View = layoutInflater.inflate(
                        R.layout.transaction_item,
                        layoutHistory,
                        false
                    )

                    // GET FIREBASE DATA
                    val type = doc.getString("type") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    val date = doc.getString("date") ?: ""

                    // CONNECT ITEM XML
                    val tvType =
                        view.findViewById<TextView>(R.id.tvType)

                    val tvAmount =
                        view.findViewById<TextView>(R.id.tvAmount)

                    val tvDate =
                        view.findViewById<TextView>(R.id.tvDate)

                    val btnDelete =
                        view.findViewById<Button>(R.id.btnDelete)

                    // SET DATA
                    tvType.text = type
                    tvAmount.text = "Rs. $amount"
                    tvDate.text = date

                    // CHANGE COLOR
                    if (type == "Withdraw") {

                        tvType.setTextColor(Color.RED)

                    } else {

                        tvType.setTextColor(
                            Color.parseColor("#1E6F43")
                        )
                    }

                    // DELETE TRANSACTION
                    btnDelete.setOnClickListener {

                        val currentUserId =
                            FirebaseAuth.getInstance()
                                .currentUser?.uid
                                ?: return@setOnClickListener

                        db.collection("wallet")
                            .document(currentUserId)
                            .collection("transactions")
                            .document(doc.id)
                            .delete()

                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Transaction Deleted",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // REFRESH LIST
                                loadTransactions()
                            }

                            .addOnFailureListener {

                                Toast.makeText(
                                    this,
                                    "Delete Failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }

                    // ADD ITEM TO PAGE
                    layoutHistory.addView(view)
                }
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to Load Transactions",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}