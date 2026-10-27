package com.otiniel.copy

/**
 * JNI boundary to the Rust local response engine. The Kotlin fallback remains
 * available for IDE builds that have not generated native libraries yet.
 */
object CopyRustBridge {
    private val libraryLoaded: Boolean by lazy {
        try {
            System.loadLibrary("copy_mobile_core")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun isAvailable(): Boolean = reply("", "", "") == "Escreva uma mensagem para começar."

    fun reply(input: String, currentTime: String, currentDate: String): String? {
        if (!libraryLoaded) return null
        return try {
            nativeReply(input, currentTime, currentDate)
        } catch (_: LinkageError) {
            null
        }
    }

    private external fun nativeReply(input: String, currentTime: String, currentDate: String): String
}
