package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StoreOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "COD", // "COD", "UDHAR", "UPI"
    val status: String = "PLACED", // "PLACED", "ACCEPTED", "PACKING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    val itemsSummary: String = "",
    val riderNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
