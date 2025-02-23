package com.example.todolistapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Logout
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
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false,
    var date: Long = System.currentTimeMillis() // Timestamp pour la date de création
)

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

@Composable
fun TaskManager(modifier: Modifier = Modifier) {
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var filter by remember { mutableStateOf("All") } // Default: show all tasks
    var isAddingTask by remember { mutableStateOf(false) }
    var isEditingTask by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) } // Pour afficher le menu déroulant

    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()
    val myRef: DatabaseReference = database.getReference("tasks")

    // Charger les tâches depuis Firebase
    LaunchedEffect(Unit) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val taskList = mutableListOf<Task>()
                for (taskSnapshot in dataSnapshot.children) {
                    val id = taskSnapshot.key ?: ""
                    val title = taskSnapshot.child("title").getValue(String::class.java) ?: ""
                    val description = taskSnapshot.child("description").getValue(String::class.java) ?: ""
                    val completed = taskSnapshot.child("completed").getValue(Boolean::class.java) ?: false
                    val date = taskSnapshot.child("date").getValue(Long::class.java) ?: System.currentTimeMillis()

                    taskList.add(Task(id, title, description, completed, date))
                }
                tasks = taskList.sortedWith(compareByDescending<Task> { it.completed }.thenByDescending { it.date })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to read tasks.", error.toException())
            }
        })
    }

    // Liste filtrée des tâches
    val filteredTasks = when (filter) {
        "Completed" -> tasks.filter { it.completed }
        "Not Completed" -> tasks.filter { !it.completed }
        else -> tasks
    }

    // Contenu principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 54.dp)
    ) {
        // Icône Logout avec redirection vers LoginActivity
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    Log.d("Logout", "Logout button or text clicked")
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)

                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
        ) {
            // Icône Logout
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Logout Icon",
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp)) // Espacement entre l'icône et le texte

            // Texte Logout
            Text(
                text = "Logout",
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,


            )
        }


        // Actions principales
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Titre
            Text(
                text = "To-Do List Application",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            )

            // Bouton Ajouter une tâche
            Button(
                onClick = { isAddingTask = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Add Task")
            }

            // Bouton Filtrer
            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)
                 // ✅ Ajout de la bordure noire
            ) {
                Button(
                    onClick = { showFilterMenu = !showFilterMenu },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Filter: $filter")
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            filter = "All"
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Completed") },
                        onClick = {
                            filter = "Completed"
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Not Completed") },
                        onClick = {
                            filter = "Not Completed"
                            showFilterMenu = false
                        }
                    )
                }
            }

            // Liste des tâches
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)

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
    }

    // Dialog supprimer une tâche
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                Button(onClick = {
                    deleteTask(task, myRef)
                    taskToDelete = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { taskToDelete = null }) {
                    Text("No")
                }
            }
        )
    }

    // Dialog Ajouter une tâche
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

    // Dialog Modifier une tâche
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



@Composable
fun TaskItem(
    task: Task,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onToggle: (Task) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Titre de la tâche
            Text(
                text = task.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Description de la tâche
            Text(
                text = task.description,
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Affichage de la date
            val dateFormatted = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(task.date))
            Text(
                text = "Created on: $dateFormatted",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Statut et actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Statut de la tâche (Aligné à gauche)
                Text(
                    text = if (task.completed) "Completed" else "Not Completed",
                    color = if (task.completed) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold
                )

                // Bouton Toggle Completion (Aligné à droite)
                Button(
                    onClick = { onToggle(task) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                    modifier = Modifier
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = if (task.completed) "Mark Incomplete" else "Mark Complete",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Icônes Edit et Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icône Edit
                IconButton(
                    onClick = { onEdit(task) },
                    modifier = Modifier.size(48.dp) // Taille augmentée
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = Color.Gray
                    )
                }

                // Icône Delete
                IconButton(
                    onClick = { onDelete(task) },
                    modifier = Modifier.size(48.dp) // Taille augmentée
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}





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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
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
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(taskTitle, taskDescription) }) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}


// Function to add a task
fun addTask(task: Task, myRef: DatabaseReference) {
    val id = myRef.push().key ?: return
    val newTask = task.copy(id = id, date = System.currentTimeMillis()) // Ajouter la date actuelle
    myRef.child(id).setValue(newTask)
}


// Function to update a task
fun updateTask(task: Task, myRef: DatabaseReference) {
    myRef.child(task.id).setValue(task)
}

// Function to delete a task
fun deleteTask(task: Task, myRef: DatabaseReference) {
    if (task.id.isNotEmpty()) {
        myRef.child(task.id).removeValue()
            .addOnSuccessListener {
                Log.d("DeleteTask", "Task deleted successfully.")
            }
            .addOnFailureListener {
                Log.e("DeleteTask", "Failed to delete task: ${it.message}")
            }
    }
}

// Function to toggle task completion
fun toggleTaskCompletion(task: Task, myRef: DatabaseReference) {
    myRef.child(task.id).child("completed").setValue(!task.completed)
}
