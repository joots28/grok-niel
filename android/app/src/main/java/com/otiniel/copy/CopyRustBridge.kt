package com.otiniel.copy

/**
 * Optional JNI boundary to the Rust core. The Kotlin fallback remains available
 * for local IDE builds that have not generated the native libraries yet.
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

    fun isAvailable(): Boolean = libraryLoaded

    fun classify(input: String): String? {
        if (!libraryLoaded) return null
        return try {
            nativeClassify(input)
        } catch (_: LinkageError) {
            null
        }
    }

    private external fun nativeClassify(input: String): String
}
