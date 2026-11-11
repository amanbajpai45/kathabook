package com.example.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Structured Firestore error logger as mandated by Firebase architecture guidelines.
 */
fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    val code = if (exception is FirebaseFirestoreException) exception.code.name else "UNKNOWN"
    val message = exception.localizedMessage ?: "Unknown error"
    Log.e("FirestoreError", "[$operation] failed at '$path' with code $code: $message", exception)
}
