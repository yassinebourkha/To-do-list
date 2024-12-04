package com.example.todolistapplication.model

data class Task(
    var id: String? = null,
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false
)