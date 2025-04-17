package com.example.project1

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

@Composable
fun Admin(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val activity = context as Activity

    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("") }

    val roles = listOf("Nurse", "Electrician", "Plumber", "Dean")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Admin - User Registration", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number (+91...)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!isOtpSent) {
            Button(
                onClick = {
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                auth.signInWithCredential(credential)
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                            override fun onCodeSent(
                                verificationIdParam: String,
                                token: PhoneAuthProvider.ForceResendingToken
                            ) {
                                verificationId = verificationIdParam
                                isOtpSent = true
                                Toast.makeText(context, "OTP Sent!", Toast.LENGTH_SHORT).show()
                            }
                        })
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send OTP")
            }
        } else {
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("Enter OTP") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val verId = verificationId
                    if (verId != null) {
                        val credential = PhoneAuthProvider.getCredential(verId, otp)
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = task.result?.user?.uid ?: return@addOnCompleteListener
                                    val userMap = hashMapOf(
                                        "phone" to phoneNumber,
                                        "role" to selectedRole,
                                        "privileges" to getPrivilegesForRole(selectedRole)
                                    )
                                    db.collection("users").document(userId).set(userMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "User Registered!", Toast.LENGTH_SHORT).show()
                                            phoneNumber = ""
                                            otp = ""
                                            selectedRole = ""
                                            isOtpSent = false
                                        }
                                } else {
                                    Toast.makeText(context, "OTP Verification Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Invalid verification state", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify & Register")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DropdownMenuBox("Select Role", roles, selectedRole) { selectedRole = it }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Buttons
        Text("Navigate to Pages", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { navController.navigate("staff") }, modifier = Modifier.fillMaxWidth()) {
            Text("Go to Staff Page")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.navigate("approve") }, modifier = Modifier.fillMaxWidth()) {
            Text("Go to Approver Page")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.navigate("dept") }, modifier = Modifier.fillMaxWidth()) {
            Text("Go to Department Page")
        }
    }
}

@Composable
fun DropdownMenuBox(
    label: String,
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun getPrivilegesForRole(role: String): List<String> {
    return when (role) {
        "Nurse" -> listOf("create")
        "Plumber" -> listOf("respond")
        "Dean" -> listOf("approve", "escalate", "monitor")
        "Electrician" -> listOf("respond", "escalate")
        else -> listOf()
    }
}
