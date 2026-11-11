package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.InventoryItem
import com.example.data.model.StoreOrder
import com.example.data.model.Transaction
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KiranaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(expectedUid: String? = null): String {
        val currentUid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Kirana store data.")
        if (expectedUid != null && currentUid != expectedUid) {
            throw SecurityException("UID mismatch: $currentUid != $expectedUid")
        }
        return currentUid
    }

    // ----------------- CUSTOMER MANAGEMENT -----------------

    fun observeCustomers(uid: String): Flow<List<Customer>> = flow {
        val verifiedUid = requireUserId(uid)
        val path = "users/$verifiedUid/customers"
        val query = db.collection("users").document(verifiedUid).collection("customers")
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(Customer::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == verifiedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addCustomer(customer: Customer): Result<String> {
        return try {
            val uid = requireUserId(customer.userId)
            val docRef = db.collection("users").document(uid).collection("customers").document()
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "name" to customer.name.trim(),
                "phone" to customer.phone.trim(),
                "totalUdhar" to customer.totalUdhar,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!customer.address.isNullOrBlank()) payload["address"] = customer.address.trim()
            if (!customer.notes.isNullOrBlank()) payload["notes"] = customer.notes.trim()

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${customer.userId}/customers")
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Unit> {
        return try {
            val uid = requireUserId(customer.userId)
            require(customer.id.isNotBlank()) { "Customer ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("customers").document(customer.id)
            val updatePayload = mutableMapOf<String, Any?>(
                "name" to customer.name.trim(),
                "phone" to customer.phone.trim(),
                "totalUdhar" to customer.totalUdhar,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (customer.address != null) updatePayload["address"] = customer.address.trim()
            if (customer.notes != null) updatePayload["notes"] = customer.notes.trim()

            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${customer.userId}/customers/${customer.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Result<Unit> {
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/customers/$customerId")
            Result.failure(e)
        }
    }

    // ----------------- TRANSACTIONS / KHATA ENTRIES -----------------

    fun observeTransactions(uid: String, customerId: String? = null): Flow<List<Transaction>> = flow {
        val verifiedUid = requireUserId(uid)
        val path = "users/$verifiedUid/transactions"
        var query: Query = db.collection("users").document(verifiedUid).collection("transactions")
        if (!customerId.isNullOrBlank()) {
            query = query.whereEqualTo("customerId", customerId)
        }
        query = query.orderBy("createdAt", Query.Direction.DESCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(Transaction::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == verifiedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTransactionAndUpdateKhata(transaction: Transaction, currentCustomer: Customer): Result<String> {
        return try {
            val uid = requireUserId(transaction.userId)
            val txDocRef = db.collection("users").document(uid).collection("transactions").document()
            val customerDocRef = db.collection("users").document(uid).collection("customers").document(transaction.customerId)

            // Calculate new total udhar
            val newTotalUdhar = if (transaction.type == "UDHAR_DIYA") {
                currentCustomer.totalUdhar + transaction.amount
            } else {
                (currentCustomer.totalUdhar - transaction.amount).coerceAtLeast(0.0)
            }

            val txPayload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "customerId" to transaction.customerId,
                "customerName" to transaction.customerName,
                "type" to transaction.type,
                "amount" to transaction.amount,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!transaction.description.isNullOrBlank()) txPayload["description"] = transaction.description.trim()
            if (!transaction.paymentMode.isNullOrBlank()) txPayload["paymentMode"] = transaction.paymentMode.trim()

            // Run atomic batch
            val batch = db.batch()
            batch.set(txDocRef, txPayload)
            batch.update(customerDocRef, mapOf(
                "totalUdhar" to newTotalUdhar,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.commit().await()

            Result.success(txDocRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${transaction.userId}/transactions")
            Result.failure(e)
        }
    }

    // ----------------- INVENTORY MANAGEMENT -----------------

    fun observeInventory(uid: String): Flow<List<InventoryItem>> = flow {
        val verifiedUid = requireUserId(uid)
        val path = "users/$verifiedUid/inventory"
        val query = db.collection("users").document(verifiedUid).collection("inventory")
            .orderBy("name", Query.Direction.ASCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(InventoryItem::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == verifiedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addInventoryItem(item: InventoryItem): Result<String> {
        return try {
            val uid = requireUserId(item.userId)
            val docRef = db.collection("users").document(uid).collection("inventory").document()
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "name" to item.name.trim(),
                "category" to item.category.trim(),
                "quantity" to item.quantity,
                "unit" to item.unit.trim(),
                "sellingPrice" to item.sellingPrice,
                "isAvailableForOnlineOrder" to item.isAvailableForOnlineOrder,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!item.hindiName.isNullOrBlank()) payload["hindiName"] = item.hindiName.trim()
            if (item.costPrice != null) payload["costPrice"] = item.costPrice
            if (item.lowStockAlert != null) payload["lowStockAlert"] = item.lowStockAlert

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${item.userId}/inventory")
            Result.failure(e)
        }
    }

    suspend fun updateInventoryItem(item: InventoryItem): Result<Unit> {
        return try {
            val uid = requireUserId(item.userId)
            require(item.id.isNotBlank()) { "Item ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("inventory").document(item.id)
            val updatePayload = mutableMapOf<String, Any?>(
                "name" to item.name.trim(),
                "category" to item.category.trim(),
                "quantity" to item.quantity,
                "unit" to item.unit.trim(),
                "sellingPrice" to item.sellingPrice,
                "isAvailableForOnlineOrder" to item.isAvailableForOnlineOrder,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (item.hindiName != null) updatePayload["hindiName"] = item.hindiName.trim()
            if (item.costPrice != null) updatePayload["costPrice"] = item.costPrice
            if (item.lowStockAlert != null) updatePayload["lowStockAlert"] = item.lowStockAlert

            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${item.userId}/inventory/${item.id}")
            Result.failure(e)
        }
    }

    suspend fun adjustStock(userId: String, itemId: String, delta: Double): Result<Unit> {
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
            docRef.update(
                mapOf(
                    "quantity" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/inventory/$itemId")
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryItem(userId: String, itemId: String): Result<Unit> {
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/inventory/$itemId")
            Result.failure(e)
        }
    }

    // ----------------- STORE ORDERS & REAL-TIME TRACKING -----------------

    fun observeOrders(uid: String): Flow<List<StoreOrder>> = flow {
        val verifiedUid = requireUserId(uid)
        val path = "users/$verifiedUid/orders"
        val query = db.collection("users").document(verifiedUid).collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(StoreOrder::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == verifiedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun placeOrder(order: StoreOrder): Result<String> {
        return try {
            val uid = requireUserId(order.userId)
            val docRef = db.collection("users").document(uid).collection("orders").document()
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "customerName" to order.customerName.trim(),
                "customerPhone" to order.customerPhone.trim(),
                "deliveryAddress" to order.deliveryAddress.trim(),
                "totalAmount" to order.totalAmount,
                "paymentMethod" to order.paymentMethod,
                "status" to "PLACED",
                "itemsSummary" to order.itemsSummary.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!order.riderNote.isNullOrBlank()) payload["riderNote"] = order.riderNote.trim()

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${order.userId}/orders")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(userId: String, orderId: String, newStatus: String, riderNote: String? = null): Result<Unit> {
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
            val updatePayload = mutableMapOf<String, Any?>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (riderNote != null) updatePayload["riderNote"] = riderNote.trim()

            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/orders/$orderId")
            Result.failure(e)
        }
    }
}

// Extension helper for Flow emitAll
private suspend inline fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
    flow.collect { value -> emit(value) }
}
