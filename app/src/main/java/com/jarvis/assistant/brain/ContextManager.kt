package com.jarvis.assistant.brain

data class ConversationContext(
    var lastUserCommand: String? = null,
    var lastAction: String? = null,
    var lastTarget: String? = null,
    var lastResponse: String? = null,
    var currentTopic: String? = null
)

class ContextManager {

    private val context = ConversationContext()

    fun update(
        command: String,
        action: String,
        target: String?,
        response: String
    ) {
        context.lastUserCommand = command
        context.lastAction = action
        context.lastTarget = target
        context.lastResponse = response
    }

    fun get(): ConversationContext = context

    fun clear() {
        context.lastUserCommand = null
        context.lastAction = null
        context.lastTarget = null
        context.lastResponse = null
        context.currentTopic = null
    }

    fun lastTarget(): String? = context.lastTarget

    fun lastAction(): String? = context.lastAction
}
