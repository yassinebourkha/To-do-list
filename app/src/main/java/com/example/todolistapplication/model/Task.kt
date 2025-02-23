package com.example.todolistapplication.model

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false,
    var date: Long = System.currentTimeMillis()
)
