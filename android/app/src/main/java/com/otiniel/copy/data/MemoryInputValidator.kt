package com.otiniel.copy.data

/**
 * Validation shared by the UI and persistence layer.
 * A memory is only stored after the user explicitly confirms Save.
 */
object MemoryInputValidator {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_DETAIL_LENGTH = 3000

    fun validate(title: String, detail: String): String? {
        val cleanTitle = title.trim()
        val cleanDetail = detail.trim()

        return when {
            cleanTitle.isEmpty() -> "Informe um título para a memória."
            cleanTitle.length > MAX_TITLE_LENGTH -> "O título deve ter até $MAX_TITLE_LENGTH caracteres."
            cleanDetail.isEmpty() -> "Informe o conteúdo da memória."
            cleanDetail.length > MAX_DETAIL_LENGTH -> "O conteúdo deve ter até $MAX_DETAIL_LENGTH caracteres."
            else -> null
        }
    }
}
