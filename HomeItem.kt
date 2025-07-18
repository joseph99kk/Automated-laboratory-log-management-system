package com.nuerovent.model


data class HomeItem(
    val label: Int,          // string resource ID (e.g., R.string.temperature)
    val reading: String,     // actual reading text (e.g., "23°C")
    val imageResId: Int      // drawable resource ID (e.g., R.drawable.temp_image)
)
