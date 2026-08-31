package com.jarvis.ai.data.model

enum class SuitMark {
    MARK_1, MARK_5, MARK_42, MARK_50, MARK_85, HULKBUSTER
}

data class ArmorSuit(
    val id: String,
    val mark: SuitMark,
    val name: String,
    val description: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val arcReactorColor: Int,
    val systemMode: String,
    val voicePitch: Float = 1.0f,
    val isLocked: Boolean = false
)
