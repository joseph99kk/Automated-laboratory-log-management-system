package com.nuerovent.model

data class Alert(
    val iconResId: Int,
    val title: String,
    val description: String,
    val timeAgo: String = "just now"
)
