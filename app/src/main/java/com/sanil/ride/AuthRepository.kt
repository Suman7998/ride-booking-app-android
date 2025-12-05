package com.sanil.ride

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun normalize(username: String): String = username.trim().lowercase()

    // Registration with unique username claim
    fun register(
        username: String,
        email: String,
        password: String,
        callback: (Result<FirebaseUser>) -> Unit
    ) {
        val usernameLower = normalize(username)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { createTask ->
                if (!createTask.isSuccessful) {
                    callback(Result.failure(createTask.exception ?: Exception("Registration failed")))
                    return@addOnCompleteListener
                }
                val user = createTask.result?.user
                if (user == null) {
                    callback(Result.failure(Exception("User is null after create")))
                    return@addOnCompleteListener
                }

                val usernamesRef = db.collection("usernames").document(usernameLower)
                val data = mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                // Create the username claim doc (create-only by rules)
                db.runTransaction { txn ->
                    val snap = txn.get(usernamesRef)
                    if (snap.exists()) {
                        throw IllegalStateException("USERNAME_TAKEN")
                    }
                    txn.set(usernamesRef, data)
                }.addOnSuccessListener {
                    // Create user profile
                    val userDoc = db.collection("users").document(user.uid)
                    val profile = mapOf(
                        "username" to username,
                        "usernameLower" to usernameLower,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    userDoc.set(profile)
                        .addOnSuccessListener {
                            callback(Result.success(user))
                        }
                        .addOnFailureListener { e ->
                            // Roll back auth if profile fails (optional)
                            user.delete()
                            callback(Result.failure(e))
                        }
                }.addOnFailureListener { e ->
                    // If username already exists, delete the auth user we just created
                    if ((e as? IllegalStateException)?.message == "USERNAME_TAKEN") {
                        user.delete()
                        callback(Result.failure(Exception("Username already taken")))
                    } else {
                        user.delete()
                        callback(Result.failure(e))
                    }
                }
            }
    }

    // Sign in using username + password by resolving to email
    fun signInWithUsername(
        username: String,
        password: String,
        callback: (Result<FirebaseUser>) -> Unit
    ) {
        val usernameLower = normalize(username)
        val usernamesRef = db.collection("usernames").document(usernameLower)
        usernamesRef.get()
            .addOnSuccessListener { doc ->
                val email = doc.getString("email")
                if (email.isNullOrBlank()) {
                    callback(Result.failure(Exception("Username not found")))
                    return@addOnSuccessListener
                }
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user == null) {
                            callback(Result.failure(Exception("Sign-in failed")))
                            return@addOnSuccessListener
                        }
                        // Update lastLogin and optionally add login event
                        val userDoc = db.collection("users").document(user.uid)
                        userDoc.update("lastLogin", FieldValue.serverTimestamp())
                        db.collection("loginEvents").add(
                            mapOf("uid" to user.uid, "at" to FieldValue.serverTimestamp())
                        )
                        callback(Result.success(user))
                    }
                    .addOnFailureListener { e ->
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}
