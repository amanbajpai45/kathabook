package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object CommunicationUtil {

    fun launchCall(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "फोन नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "कॉल करने में असमर्थ", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchSms(context: Context, phoneNumber: String, message: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanNumber")
            putExtra("sms_body", message)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "SMS ऐप खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchWhatsApp(context: Context, phoneNumber: String, message: String) {
        var cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        // If 10-digit Indian number without country code, prefix with 91
        if (cleanNumber.length == 10) {
            cleanNumber = "91$cleanNumber"
        }
        val encodedMessage = Uri.encode(message)
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            try {
                context.startActivity(Intent.createChooser(shareIntent, "Share Reminder"))
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
