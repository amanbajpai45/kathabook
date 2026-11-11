package com.example.data.repository

import android.util.Log
import com.example.data.model.Customer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiReminderRepository"

class AiReminderRepository {

    // Lazy initialization of Gemini 3.8 Flash model as mandated by Firebase AI Logic skill
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.9f
            }
        )
    }

    /**
     * Generates an intelligent, personalized weekend payment reminder in Hindi / Hinglish.
     * Tones: "POLITE" (विनम्र), "WEEKEND" (वीकेंड तकादा), "FIRM" (आवश्यक), "DISCOUNT" (छूट ऑफर)
     */
    suspend fun generatePaymentReminder(
        customerName: String,
        amount: Double,
        tone: String,
        storeName: String = "किराना स्टोर"
    ): Result<String> {
        return try {
            val toneInstruction = when (tone) {
                "POLITE" -> "Very polite and respectful, maintaining a good relationship. (विनम्र और आदरणीय)"
                "WEEKEND" -> "Weekend payment reminder asking to settle this weekend so stock can be replenished on Monday. (वीकेंड तकादा)"
                "FIRM" -> "Firm and urgent due to long overdue payment, clearly requesting payment today. (अति आवश्यक तकादा)"
                "DISCOUNT" -> "Offering a quick settlement incentive or thanking them in advance for clearing the balance today. (तुरंत भुगतान पर आभार)"
                else -> "Friendly and professional Kirana store shopkeeper message."
            }

            val prompt = """
                You are an intelligent billing and payment recovery assistant for an Indian Kirana General Store ($storeName).
                Generate a ready-to-send WhatsApp / SMS reminder message for the customer.
                
                Customer Details:
                - Name: $customerName
                - Outstanding Udhar Amount: ₹${String.format("%.0f", amount)}
                - Reminder Style / Tone: $toneInstruction
                
                Rules:
                1. Write the message in friendly, natural Hindi with Hinglish/Devanagari, easily understood by Indian customers.
                2. Mention the store name ($storeName) and exact amount (₹${String.format("%.0f", amount)}).
                3. Keep it concise, courteous, and ready to send directly on WhatsApp or SMS.
                4. Include polite greeting (नमस्ते / प्रणाम $customerName जी) and friendly closing (धन्यवाद, $storeName).
                5. Do NOT include placeholder tokens like [Your Name] or [Link]. Give the final ready text only.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                Result.failure(IllegalStateException("AI model returned an empty reminder."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI reminder generation timed out", e)
            Result.failure(Exception("AI तकादा जनरेट करने में समय अधिक लगा। कृपया पुनः प्रयास करें।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI reminder generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a weekly financial health insight and udhar recovery strategy for the store owner.
     */
    suspend fun generateShopFinancialAdvice(
        totalUdhar: Double,
        pendingCustomerCount: Int,
        totalCustomers: Int,
        inventoryCount: Int
    ): Result<String> {
        return try {
            val prompt = """
                You are a smart Kirana Business Advisor. Analyze these weekly store statistics and provide 3-4 actionable Hindi tips for the shopkeeper:
                - Total Udhar Pending: ₹${String.format("%.0f", totalUdhar)}
                - Customers with Pending Udhar: $pendingCustomerCount out of $totalCustomers total customers
                - Active Inventory Items: $inventoryCount
                
                Give 3 bullet points with practical advice on:
                1. Weekend recovery strategy.
                2. Inventory cashflow management.
                3. Customer credit limit safety.
                Keep it very practical, encouraging, and in easy Hindi/Hinglish.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                Result.failure(IllegalStateException("No advice generated."))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("AI सलाह जनरेट करने में समय समाप्त हो गया।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
