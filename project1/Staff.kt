// Staff.kt
package com.example.project1

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment

@Composable
fun Staff(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var dutyTiming by remember { mutableStateOf("") }
    var complaint by remember { mutableStateOf("") }

    val departments = listOf("Civil", "Electrical", "Laundry", "Plumbing")
    var selectedDepartments by remember { mutableStateOf(setOf<String>()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text("Raise a Non-Medical Memo", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = ward, onValueChange = { ward = it }, label = { Text("Ward") })
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text("Floor") })
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = dutyTiming, onValueChange = { dutyTiming = it }, label = { Text("Duty Timing") })
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = complaint,
            onValueChange = { complaint = it },
            label = { Text("Nature of Complaint") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Select Concerned Departments:")
        departments.forEach { dept ->
            val isChecked = selectedDepartments.contains(dept)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        selectedDepartments = if (checked) {
                            selectedDepartments + dept
                        } else {
                            selectedDepartments - dept
                        }
                    }
                )
                Text(dept, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && ward.isNotBlank() && complaint.isNotBlank()) {
                    val memo = hashMapOf(
                        "name" to name,
                        "ward" to ward,
                        "floor" to floor,
                        "dutyTiming" to dutyTiming,
                        "complaint" to complaint,
                        "departments" to selectedDepartments.toList(),
                        "status" to "Pending",
                        "timestamp" to Timestamp.now(),
                        "title" to selectedDepartments.joinToString(),
                        "description" to complaint,
                        "raisedBy" to name
                    )

                    db.collection("memos")
                        .add(memo)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Memo Submitted!", Toast.LENGTH_SHORT).show()
                            name = ""; ward = ""; floor = ""; dutyTiming = ""; complaint = ""
                            selectedDepartments = setOf()
                            navController.navigate("approvers")
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Memo")
        }
    }
}


