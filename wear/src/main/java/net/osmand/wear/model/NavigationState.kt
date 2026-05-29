package net.osmand.wear.model

data class NavigationState(
    val isActive: Boolean = false,
    val nextTurnIconRes: Int? = null,
    val distanceToNextTurn: String = "",
    val instructionText: String = ""
)
