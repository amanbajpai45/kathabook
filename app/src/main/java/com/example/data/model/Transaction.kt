package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val type: String = "UDHAR_DIYA", // "UDHAR_DIYA" (Debit / सामान ले गया) or "JAMA_KIYA" (Credit / पैसे दिए)
    val amount: Double = 0.0,
    val description: String? = null,
    val paymentMode: String? = "Cash", // Cash, UPI, Google Pay, PhonePe, Paytm, Khata
    @ServerTimestamp val createdAt: Timestamp? = null
)
