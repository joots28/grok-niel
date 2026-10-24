package com.otiniel.copy.data

/**
 * Read-only access helpers for explicitly saved user memories.
 *
 * This layer does not create memories, infer personal facts, or attach memories to conversations.
 * Callers must decide when to request context and must keep the user in control.
 */
class SavedMemoryAccess(private val repository: MemoryRepository) {
    /** Returns the user's saved memories in repository order. */
    fun listSaved(): List<MemoryItem> = repository.getAll()

    /** Returns a saved memory by its local database ID, or null when it no longer exists. */
    fun findSaved(id: Long): MemoryItem? {
        if (id <= 0L) return null
        return repository.getById(id)
    }
}
