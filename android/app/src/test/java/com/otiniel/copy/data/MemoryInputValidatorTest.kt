package com.otiniel.copy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemoryInputValidatorTest {
    @Test
    fun rejectsBlankTitle() {
        assertEquals(
            "Informe um título para a memória.",
            MemoryInputValidator.validate("   ", "Conteúdo válido"),
        )
    }

    @Test
    fun rejectsBlankContent() {
        assertEquals(
            "Informe o conteúdo da memória.",
            MemoryInputValidator.validate("Título válido", " \n "),
        )
    }

    @Test
    fun rejectsTitleOverLimit() {
        val title = "t".repeat(MemoryInputValidator.MAX_TITLE_LENGTH + 1)
        assertEquals(
            "O título deve ter até 120 caracteres.",
            MemoryInputValidator.validate(title, "Conteúdo válido"),
        )
    }

    @Test
    fun rejectsContentOverLimit() {
        val detail = "d".repeat(MemoryInputValidator.MAX_DETAIL_LENGTH + 1)
        assertEquals(
            "O conteúdo deve ter até 3000 caracteres.",
            MemoryInputValidator.validate("Título válido", detail),
        )
    }

    @Test
    fun acceptsTrimmedValidInputAtLimits() {
        val title = "t".repeat(MemoryInputValidator.MAX_TITLE_LENGTH)
        val detail = "d".repeat(MemoryInputValidator.MAX_DETAIL_LENGTH)

        assertNull(MemoryInputValidator.validate(" $title ", " $detail "))
    }
}
