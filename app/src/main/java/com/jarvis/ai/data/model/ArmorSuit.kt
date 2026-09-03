package com.jarvis.ai.data.model

enum class SuitMark {
    MARK_1, MARK_2, MARK_3, MARK_5, MARK_6, MARK_7,
    MARK_42, MARK_46, MARK_50, MARK_85, HULKBUSTER, MARK_ENDGAME
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
    val isLocked: Boolean = false,
    /** Vector drawable resource name without extension, e.g. ic_suit_mark3 */
    val vectorResName: String = "ic_suit_mark3"
)
