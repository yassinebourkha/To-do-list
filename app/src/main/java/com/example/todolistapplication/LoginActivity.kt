package com.example.todolistapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todolistapplication.ui.theme.ToDoListApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        setContent {
            ToDoListApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginPage(
                        onLoginSuccess = {
                            // Redirect to MainActivity after successful login
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish() // Close LoginActivity to avoid returning to it
                        },
                        onRegister = {
                            // Open RegisterAuth activity for user registration
                            val intent = Intent(this@LoginActivity, RegisterAuth::class.java)
                            startActivity(intent)
                        },
                        auth = auth,
                        modifier = Modifier.padding(innerPadding) // Apply padding
                    )
                }
            }
        }
    }
}

@Composable
fun LoginPage(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    auth: FirebaseAuth,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Wrapping all content in a Box to set a background image
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.test1), // Remplacez par le nom de votre image
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop, // Rend l'image couvrante
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Nécessaire pour activer l'effet de dessin
                .drawWithContent {
                    drawContent()
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f) // Optionnel : assombrir légèrement
                    )
                }
        )


        // Overlay the content on top of the background
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title at the top of the screen (To-Do List)
            Text(
                text = "To-Do List Application",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 252.dp),
                //color = MaterialTheme.colorScheme.onPrimary // Adjust text color for visibility
                color = MaterialTheme.colorScheme.primary
            )

            // Title of the login page
            Text(
                text = "Login",
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onPrimary // Adjust text color for visibility
            )

            // Email input field
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password input field
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Button
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                showError = true
                                errorMessage = task.exception?.localizedMessage ?: "Unknown error occurred"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Register Button for new users
            Button(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create New Account")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display error message if login failed
            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}