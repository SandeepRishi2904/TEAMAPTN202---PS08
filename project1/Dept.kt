package com.example.project1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

// -------------------- Data Class --------------------

data class Memo1(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "Pending",
    val assignedTo: String = "",
    val taggedDepartments: List<String> = emptyList(),
    val raisedBy: String = "",
    val escalationLevel: Int = 0,
    val natureOfComplaint: String = ""
)

// -------------------- ViewModel --------------------

class DeptViewModel(private val departmentName: String = "Civil") : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _memos = mutableStateListOf<Memo1>()
    val memos: List<Memo1> = _memos

    init {
        loadMemosForDepartment()
    }

    private fun loadMemosForDepartment() {
        db.collection("memos")
            .whereEqualTo("assignedTo", departmentName)
            .addSnapshotListener { snapshots, _ ->
                _memos.clear()
                snapshots?.forEach { doc ->
                    _memos.add(doc.toObject(Memo1::class.java).copy(id = doc.id))
                }
            }
    }

    fun markAsCompleted(memoId: String) {
        db.collection("memos").document(memoId)
            .update("status", "Completed")
    }

    fun withholdMemo(memoId: String, reason: String) {
        db.collection("memos").document(memoId)
            .update(mapOf("status" to "Withheld", "notes" to reason))
    }

    fun tagAnotherDepartment(memoId: String, newDept: String) {
        val memoRef = db.collection("memos").document(memoId)
        memoRef.update("taggedDepartments", FieldValue.arrayUnion(newDept))
    }
}

// -------------------- UI Composable --------------------

@Composable
fun Dept(navController: NavHostController,viewModel: DeptViewModel = viewModel()) {
    val memos by remember { derivedStateOf { viewModel.memos } }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        Text(
            "Assigned Memos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (memos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No memos assigned.")
            }
        } else {
            LazyColumn {
                items(memos) { memo ->
                    MemoCard(
                        memo = memo,
                        onComplete = { viewModel.markAsCompleted(memo.id) },
                        onWithhold = { viewModel.withholdMemo(memo.id, "Waiting for materials") },
                        onTagDept = { deptName -> viewModel.tagAnotherDepartment(memo.id, deptName) }
                    )
                }
            }
        }
    }
}

// -------------------- Memo Card --------------------

@Composable
fun MemoCard(
    memo: Memo1,
    onComplete: () -> Unit,
    onWithhold: () -> Unit,
    onTagDept: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Title: ${memo.title}", fontWeight = FontWeight.Bold)
            Text("Description: ${memo.description}")
            Text("Nature of Complaint: ${memo.natureOfComplaint}")
            Text("Status: ${memo.status}")
            Text("Raised By: ${memo.raisedBy}")
            Text("Tagged Departments: ${memo.taggedDepartments.joinToString()}")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onComplete) {
                    Text("Mark Complete")
                }
                Button(onClick = onWithhold) {
                    Text("Withhold")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text("Tag Department (e.g. Plumbing)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (tagInput.isNotBlank()) {
                        onTagDept(tagInput.trim())
                        tagInput = ""
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Text("Tag Dept")
            }
        }
    }
}
