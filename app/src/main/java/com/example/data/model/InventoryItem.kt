package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class InventoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val hindiName: String? = null,
    val category: String = "General",
    val quantity: Double = 0.0,
    val unit: String = "kg", // kg, gm, ltr, pkt, pc
    val costPrice: Double? = null,
    val sellingPrice: Double = 0.0,
    val lowStockAlert: Double? = 5.0,
    val isAvailableForOnlineOrder: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
