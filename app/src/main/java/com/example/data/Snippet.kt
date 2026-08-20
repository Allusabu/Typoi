package com.example.data

data class Snippet(
    val id: String,
    val title: String,
    val text: String,
    val category: String = "General",
    val defaultSpeedMs: Long = 80L,
    val isCustom: Boolean = false
)
