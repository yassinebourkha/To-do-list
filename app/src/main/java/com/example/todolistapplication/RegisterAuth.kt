package com.example.todolistapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.todolistapplication.ui.theme.ToDoListApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

class RegisterAuth : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        setContent {
            ToDoListApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegisterPage(
                        onRegisterSuccess = {
                            val intent = Intent(this@RegisterAuth, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onBackToLogin = {
                            val intent = Intent(this@RegisterAuth, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        auth = auth,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterPage(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    auth: FirebaseAuth,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔙 Bouton retour
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "",
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBackToLogin() },
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "",
                fontSize = 16.sp,
                modifier = Modifier.clickable { onBackToLogin() },
                color = Color.Blue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📝 Titre de la page d'inscription
        Text(
            text = "Create Account",
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ✉️ Champ Email
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 🔑 Champ Mot de passe
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Champ Confirmation du mot de passe
        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 📝 Bouton d'enregistrement
        Button(
            onClick = {
                when {
                    email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                        showError = true
                        errorMessage = "All fields are required."
                    }
                    password != confirmPassword -> {
                        showError = true
                        errorMessage = "Passwords do not match."
                    }
                    password.length < 6 -> {
                        showError = true
                        errorMessage = "Password must be at least 6 characters."
                    }
                    else -> {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onRegisterSuccess()
                                } else {
                                    showError = true
                                    errorMessage = task.exception?.localizedMessage ?: "Registration failed."
                                    Log.e("RegisterAuth", "Registration error: ${task.exception}", task.exception)
                                }
                            }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ⚠️ Message d'erreur
        if (showError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
