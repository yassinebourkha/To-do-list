package com.example.todolistapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.example.todolistapplication.ui.theme.ToDoListApplicationTheme
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

// ✅ Data class pour représenter une tâche
data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false,
    var date: Long = System.currentTimeMillis()
)

// ✅ Classe principale de l'activité
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoListApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TaskManager(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ✅ Composable principal pour gérer les tâches
@Composable
fun TaskManager(modifier: Modifier = Modifier) {
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var filter by remember { mutableStateOf("All") }
    var isAddingTask by remember { mutableStateOf(false) }
    var isEditingTask by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()
    val myRef: DatabaseReference = database.getReference("tasks")

    // ✅ Charger les tâches depuis Firebase
    LaunchedEffect(Unit) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tasks = snapshot.children.mapNotNull { taskSnapshot ->
                    val id = taskSnapshot.key ?: ""
                    val title = taskSnapshot.child("title").getValue(String::class.java) ?: ""
                    val description = taskSnapshot.child("description").getValue(String::class.java) ?: ""
                    val completed = taskSnapshot.child("completed").getValue(Boolean::class.java) ?: false
                    val date = taskSnapshot.child("date").getValue(Long::class.java) ?: System.currentTimeMillis()
                    Task(id, title, description, completed, date)
                }.sortedWith(compareByDescending<Task> { it.completed }.thenByDescending { it.date })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to read tasks.", error.toException())
            }
        })
    }

    val filteredTasks = when (filter) {
        "Completed" -> tasks.filter { it.completed }
        "Not Completed" -> tasks.filter { !it.completed }
        else -> tasks
    }

    // ✅ Contenu principal
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔓 Logout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                    (context as? ComponentActivity)?.finish()
                }
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Logout",
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📝 Titre
        Text(
            text = "To-Do List Application",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ➕ Bouton d'ajout de tâche
        Button(
            onClick = { isAddingTask = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
        ) {
            Text("Add Task", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔍 Filtrage des tâches
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Button(
                onClick = { showFilterMenu = !showFilterMenu },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
            ) {
                Text("Filter: $filter", fontSize = 16.sp, color = Color.White)
            }

            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                listOf("All", "Completed", "Not Completed").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            filter = option
                            showFilterMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📋 Liste des tâches
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            items(filteredTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onDelete = { taskToDelete = it },
                    onEdit = { isEditingTask = it },
                    onToggle = { toggleTaskCompletion(it, myRef) }
                )
            }
        }
    }

    // 🗑️ Boîte de dialogue de suppression
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                Button(onClick = {
                    deleteTask(task, myRef)
                    taskToDelete = null
                }) { Text("Yes") }
            },
            dismissButton = {
                Button(onClick = { taskToDelete = null }) { Text("No") }
            }
        )
    }

    // ➕ Boîte de dialogue d'ajout de tâche
    if (isAddingTask) {
        TaskDialog(
            title = "Add Task",
            onDismiss = { isAddingTask = false },
            onConfirm = { title, description ->
                addTask(Task(title = title, description = description), myRef)
                isAddingTask = false
            }
        )
    }

    // ✏️ Boîte de dialogue de modification de tâche
    isEditingTask?.let { task ->
        TaskDialog(
            title = "Edit Task",
            initialTitle = task.title,
            initialDescription = task.description,
            onDismiss = { isEditingTask = null },
            onConfirm = { title, description ->
                val updatedTask = task.copy(title = title, description = description)
                updateTask(updatedTask, myRef)
                isEditingTask = null
            }
        )
    }
}

// ✅ Composable pour afficher une tâche
@Composable
fun TaskItem(
    task: Task,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onToggle: (Task) -> Unit
) {
    val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(task.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = task.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = task.description, fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Created on: $dateFormatted", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (task.completed) "Completed" else "Not Completed",
                    color = if (task.completed) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { onToggle(task) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (task.completed) Color.Red else Color.Blue
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (task.completed) "Mark Incomplete" else "Mark Complete",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onEdit(task) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
                IconButton(onClick = { onDelete(task) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

// ✅ Composable pour la boîte de dialogue (ajout/édition)
@Composable
fun TaskDialog(
    title: String,
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var taskTitle by remember { mutableStateOf(initialTitle) }
    var taskDescription by remember { mutableStateOf(initialDescription) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(taskTitle, taskDescription) }) { Text("Confirm") }
                }
            }
        }
    }
}

// ✅ Fonctions de manipulation des tâches avec Firebase
fun addTask(task: Task, myRef: DatabaseReference) {
    val id = myRef.push().key ?: return
    val newTask = task.copy(id = id, date = System.currentTimeMillis())
    myRef.child(id).setValue(newTask)
}

fun updateTask(task: Task, myRef: DatabaseReference) {
    myRef.child(task.id).setValue(task)
}

fun deleteTask(task: Task, myRef: DatabaseReference) {
    if (task.id.isNotEmpty()) {
        myRef.child(task.id).removeValue()
            .addOnSuccessListener { Log.d("DeleteTask", "Task deleted successfully.") }
            .addOnFailureListener { Log.e("DeleteTask", "Failed to delete task: ${it.message}") }
    }
}

fun toggleTaskCompletion(task: Task, myRef: DatabaseReference) {
    myRef.child(task.id).child("completed").setValue(!task.completed)
}
