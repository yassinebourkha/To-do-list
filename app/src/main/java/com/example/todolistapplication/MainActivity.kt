package com.example.todolistapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.todolistapplication.ui.theme.ToDoListApplicationTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ToDoListApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FirebaseDataScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun FirebaseDataScreen(modifier: Modifier = Modifier) {
    var firebaseData by remember { mutableStateOf("") } // Stockage des données récupérées

    // Firebase Database
    val database = FirebaseDatabase.getInstance()
    val myRef: DatabaseReference = database.getReference("users")

    // Charger les données Firebase
    LaunchedEffect(Unit) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val data = StringBuilder()

                for (userSnapshot in dataSnapshot.children) {
                    val name = userSnapshot.child("name").getValue(String::class.java)
                    val email = userSnapshot.child("email").getValue(String::class.java)

                    data.append("Name: ").append(name).append("\n")
                    data.append("Email: ").append(email).append("\n\n")
                }

                firebaseData = data.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to read value.", error.toException())
                firebaseData = "Error: ${error.message}"
            }
        })
    }

    // Afficher les données dans l'interface Compose
    Column(modifier = modifier.fillMaxSize()) {
        Text(text = "Firebase Data:")
        Text(text = firebaseData) // Affiche les données récupérées
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToDoListApplicationTheme {
        FirebaseDataScreen()
    }
}
