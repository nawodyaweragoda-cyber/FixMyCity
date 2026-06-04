package com.example.fixmycity

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MoneyActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_money)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val btnBack = findViewById<TextView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etWithdraw = findViewById<EditText>(R.id.etWithdraw)
        val tvBalance = findViewById<TextView>(R.id.tvBalance)

        val btnAdd = findViewById<Button>(R.id.btnAddMoney)
        val btnWithdraw = findViewById<Button>(R.id.btnWithdraw)
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        btnHistory.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        // ✅ CHECK LOGIN USER
        val user = auth.currentUser

        if (user == null) {

            Toast.makeText(
                this,
                "Please login first",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(
                Intent(this, LoginActivity::class.java)
            )

            finish()
            return
        }

        val userId = user.uid

        val walletRef = db
            .collection("wallet")
            .document(userId)

        // ✅ SHOW BALANCE LIVE
        walletRef.addSnapshotListener { doc, _ ->

            val balance =
                doc?.getDouble("balance") ?: 0.0

            tvBalance.text = "Rs. $balance"
        }

        // ✅ ADD MONEY
        btnAdd.setOnClickListener {

            val text = etAmount.text.toString()

            if (text.isEmpty()) {

                etAmount.error = "Enter amount"
                return@setOnClickListener
            }

            val amount = text.toDouble()

            walletRef.get()
                .addOnSuccessListener { doc ->

                    val current =
                        doc.getDouble("balance") ?: 0.0

                    val newBalance =
                        current + amount

                    walletRef.set(
                        mapOf(
                            "balance" to newBalance
                        )
                    )

                    // ✅ SAVE TRANSACTION
                    val transaction = hashMapOf(
                        "type" to "Added",
                        "amount" to amount,
                        "date" to System.currentTimeMillis().toString()
                    )

                    walletRef.collection("transactions")
                        .add(transaction)

                    Toast.makeText(
                        this,
                        "Money Added",
                        Toast.LENGTH_SHORT
                    ).show()

                    etAmount.text.clear()
                }
        }

        // ✅ WITHDRAW MONEY
        btnWithdraw.setOnClickListener {

            val text = etWithdraw.text.toString()

            if (text.isEmpty()) {

                etWithdraw.error = "Enter amount"
                return@setOnClickListener
            }

            val amount = text.toDouble()

            walletRef.get()
                .addOnSuccessListener { doc ->

                    val current =
                        doc.getDouble("balance") ?: 0.0

                    if (current >= amount) {

                        val newBalance =
                            current - amount

                        walletRef.set(
                            mapOf(
                                "balance" to newBalance
                            )
                        )

                        // ✅ SAVE TRANSACTION
                        val transaction = hashMapOf(
                            "type" to "Withdraw",
                            "amount" to amount,
                            "date" to System.currentTimeMillis().toString()
                        )

                        walletRef.collection("transactions")
                            .add(transaction)

                        Toast.makeText(
                            this,
                            "Withdraw Success",
                            Toast.LENGTH_SHORT
                        ).show()

                        etWithdraw.text.clear()

                    } else {

                        Toast.makeText(
                            this,
                            "Not enough balance",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // ✅ OPEN TRANSACTION HISTORY
        btnHistory.setOnClickListener {

            val intent = Intent(
                this,
                TransactionHistoryActivity::class.java
            )

            startActivity(intent)
        }
    }
}