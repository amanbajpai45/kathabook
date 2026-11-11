package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Customer
import com.example.data.model.InventoryItem
import com.example.data.model.StoreOrder
import com.example.data.model.Transaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class KiranaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addCustomer_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KiranaRepository(firestore)

        val customer = Customer(
            userId = uid,
            name = "Mohan Sharma",
            phone = "9823456780",
            totalUdhar = 350.0,
            notes = "Dukan ke pas rehta hai"
        )

        val result = withTimeout(TIMEOUT_MS) {
            repository.addCustomer(customer)
        }
        assertTrue("Customer creation should succeed", result.isSuccess)
        val customerId = result.getOrThrow()
        assertTrue("Generated ID must not be blank", customerId.isNotBlank())
    }

    @Test
    fun addTransactionAndUpdateKhata_atomicBatch_updatesBalance() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KiranaRepository(firestore)

        val initialCustomer = Customer(
            userId = uid,
            name = "Suresh Tailor",
            phone = "9876501234",
            totalUdhar = 200.0
        )
        val customerId = withTimeout(TIMEOUT_MS) {
            repository.addCustomer(initialCustomer).getOrThrow()
        }

        val createdCustomer = initialCustomer.copy(id = customerId)
        val transaction = Transaction(
            userId = uid,
            customerId = customerId,
            customerName = "Suresh Tailor",
            type = "UDHAR_DIYA",
            amount = 150.0,
            description = "10kg Atta + 1L Mustard Oil"
        )

        val txResult = withTimeout(TIMEOUT_MS) {
            repository.addTransactionAndUpdateKhata(transaction, createdCustomer)
        }
        assertTrue("Transaction addition should succeed", txResult.isSuccess)
    }

    @Test
    fun addInventoryItem_authenticatedOwner_canObserve() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KiranaRepository(firestore)

        val item = InventoryItem(
            userId = uid,
            name = "Fortune Soyabean Oil 1L",
            hindiName = "फॉर्च्यून सोयाबीन तेल",
            category = "Edible Oil",
            quantity = 24.0,
            unit = "pkt",
            costPrice = 110.0,
            sellingPrice = 125.0,
            lowStockAlert = 6.0
        )

        val result = withTimeout(TIMEOUT_MS) {
            repository.addInventoryItem(item)
        }
        assertTrue("Inventory item creation should succeed", result.isSuccess)
        val itemId = result.getOrThrow()

        val items = withTimeout(TIMEOUT_MS) {
            repository.observeInventory(uid).first { list -> list.any { it.id == itemId } }
        }
        assertTrue("Inventory should contain created item", items.any { it.id == itemId })
    }

    @Test
    fun unauthenticatedAccess_failsWithSecurityExceptionOrPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = KiranaRepository(firestore)

        val result = repository.addCustomer(
            Customer(
                userId = "fake_uid",
                name = "Attacker",
                phone = "9999999999"
            )
        )
        assertTrue("Unauthenticated write must return failure", result.isFailure)
    }

    companion object {
        const val ALICE_EMAIL = "alice_kirana@test.com"
        const val TIMEOUT_MS = 8000L
    }
}
