package com.example.project1

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// -------------------- Data Class --------------------

data class Memo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "Pending",
    val raisedBy: String = "",
    val assignedTo: String = "",
    val escalationLevel: Int = 0,
    val natureOfComplaint: String = ""
)

// -------------------- ViewModel --------------------

class MemoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _memos = mutableStateListOf<Memo>()
    val memos: List<Memo> = _memos

    init {
        loadPendingMemos()
    }

    private fun loadPendingMemos() {
        db.collection("memos")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, _ ->
                _memos.clear()
                snapshots?.forEach { doc ->
                    val memo = doc.toObject(Memo::class.java).copy(id = doc.id)
                    _memos.add(memo)
                }
            }
    }

    fun approveMemo(memo: Memo, onApproved: () -> Unit) {
        db.collection("memos").document(memo.id)
            .update("status", "Approved")
            .addOnSuccessListener {
                // Save to another collection
                db.collection("approved_memos")
                    .document(memo.id)
                    .set(memo.copy(status = "Approved"), SetOptions.merge())
                    .addOnSuccessListener { onApproved() }
            }
    }

    fun escalateMemo(memoId: String, currentLevel: Int) {
        db.collection("memos").document(memoId)
            .update(
                mapOf(
                    "status" to "Escalated",
                    "escalationLevel" to currentLevel + 1
                )
            )
    }
}

// -------------------- UI Composable --------------------

@Composable
fun MemoCard(
    memo: Memo,
    navController: NavHostController,
    onEscalate: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Raised By: ${memo.raisedBy}")
            Text("Title: ${memo.title}")
            Text("Description: ${memo.description}")
            Text("Nature of Complaint: ${memo.natureOfComplaint}")
            Text("Escalation Level: ${memo.escalationLevel}")
            Text("Assigned To: ${memo.assignedTo}")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    val approvedMemo = hashMapOf(
                        "title" to memo.title,
                        "description" to memo.description,
                        "status" to "Approved", // 👈 Dept.kt will read these
                        "raisedBy" to memo.raisedBy,
                        "assignedTo" to memo.assignedTo,
                        "escalationLevel" to memo.escalationLevel,
                        "natureOfComplaint" to memo.natureOfComplaint
                    )

                    db.collection("memos").document(memo.id)
                        .set(approvedMemo) // 👈 overwrite/update the existing memo
                        .addOnSuccessListener {
                            Toast.makeText(context, "Approved and sent to Dept", Toast.LENGTH_SHORT).show()
                            navController.navigate("dept") // 👈 Navigate to Dept.kt
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Approval failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }) {
                    Text("Approve")
                }

                Button(onClick = onEscalate) {
                    Text("Escalate")
                }
            }
        }
    }
}

@Composable
fun Approvers(navController: NavHostController, viewModel: MemoViewModel = viewModel()) {
    val memos = viewModel.memos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Pending Memos for Approval",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (memos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No pending memos.")
            }
        } else {
            LazyColumn {
                items(memos) { memo ->
                    MemoCard(
                        memo = memo,
                        navController = navController,
                        onEscalate = {
                            viewModel.escalateMemo(memo.id, memo.escalationLevel)
                        }
                    )
                }
            }
        }
    }
}

