package com.nuerovent.model




data class ControlItem(
    val label: String,        //  "Inductor Fan"
    var isChecked: Boolean,   // Switch state (true = ON)
    val subText: String       // Subtext below the label, e.g. "Auto mode"
)
