package com.jarvis.assistant.brain

import java.util.concurrent.ConcurrentHashMap

class MemoryManager {

    private val memory = ConcurrentHashMap<String, String>()

    fun remember(key: String, value: String) {
        memory[key.lowercase()] = value
    }

    fun recall(key: String): String? {
        return memory[key.lowercase()]
    }

    fun forget(key: String) {
        memory.remove(key.lowercase())
    }

    fun clear() {
        memory.clear()
    }

    fun contains(key: String): Boolean {
        return memory.containsKey(key.lowercase())
    }

    fun getAll(): Map<String, String> {
        return memory.toMap()
    }

    fun size(): Int {
        return memory.size
    }
}
