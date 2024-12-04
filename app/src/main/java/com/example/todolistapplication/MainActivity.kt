package com.example.todolistapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.todolistapplication.ui.theme.ToDoListApplicationTheme
import com.google.firebase.database.*

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false
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
    var filter by remember { mutableStateOf("All") } // Par défaut, afficher toutes les tâches
    var isAddingTask by remember { mutableStateOf(false) }
    var isEditingTask by remember { mutableStateOf<Task?>(null) }
    var showFilterDropdown by remember { mutableStateOf(false) } // État pour afficher ou non la liste déroulante

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
                    taskList.add(Task(id, title, description, completed))
                }
                tasks = taskList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to read tasks.", error.toException())
            }
        })
    }

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

        // Row pour les boutons Filter et Add Task
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton de filtre avec DropdownMenu
            Box {
                Button(onClick = { showFilterDropdown = !showFilterDropdown }) {
                    Text("Filter: $filter")
                }
                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            filter = "All"
                            showFilterDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Not Completed") },
                        onClick = {
                            filter = "Not Completed"
                            showFilterDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Completed") },
                        onClick = {
                            filter = "Completed"
                            showFilterDropdown = false
                        }
                    )
                }
            }

            // Bouton Ajouter une Tâche
            Button(
                onClick = { isAddingTask = true },
                modifier = Modifier
            ) {
                Text("Add Task")
            }
        }

        // Liste filtrée des tâches
        val filteredTasks = when (filter) {
            "Not Completed" -> tasks.filter { !it.completed }
            "Completed" -> tasks.filter { it.completed }
            else -> tasks
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(filteredTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onDelete = { deleteTask(it, myRef) },
                    onEdit = { isEditingTask = it },
                    onToggle = { toggleTaskCompletion(it, myRef) }
                )
            }
        }
    }

    // Dialog Ajouter une Tâche
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

    // Dialog Modifier une Tâche
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
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = task.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.description,
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (task.completed) "Completed" else "Not Completed",
                    color = if (task.completed) Color.Green else Color.Red
                )
                Row {
                    Button(onClick = { onToggle(task) }) {
                        Text(if (task.completed) "Mark Incomplete" else "Mark Complete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onEdit(task) }) {
                        Text("Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onDelete(task) }
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



// Fonction pour ajouter une tâche
fun addTask(task: Task, myRef: DatabaseReference) {
    val id = myRef.push().key ?: return
    val newTask = task.copy(id = id)
    myRef.child(id).setValue(newTask)
}

// Fonction pour mettre à jour une tâche
fun updateTask(task: Task, myRef: DatabaseReference) {
    myRef.child(task.id).setValue(task)
}

// Fonction pour supprimer une tâche
fun deleteTask(task: Task, myRef: DatabaseReference) {
    myRef.child(task.id).removeValue()
}

// Fonction pour basculer l'état d'une tâche
fun toggleTaskCompletion(task: Task, myRef: DatabaseReference) {
    myRef.child(task.id).child("completed").setValue(!task.completed)
}
