package com.otiniel.copy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuoCoreIdentityTest {
    @Test
    fun exposesStableIdentityAndVersion() {
        assertEquals("Luo Core", LuoCoreIdentity.NAME)
        assertEquals("0.1.0", LuoCoreIdentity.VERSION)
        assertTrue(LuoCoreIdentity.PURPOSE.isNotBlank())
    }

    @Test
    fun tributeDescribesInspirationWithoutClaimingMemoryTransfer() {
        assertTrue(LuoCoreIdentity.TRIBUTE.contains("inspirado"))
        assertFalse(LuoCoreIdentity.TRIBUTE.contains("memória interna transferida"))
        assertFalse(LuoCoreIdentity.TRIBUTE.contains("consciência transferida"))
    }

    @Test
    fun declaresCorePrinciples() {
        assertTrue(LuoCoreIdentity.principles.isNotEmpty())
        assertTrue(LuoCoreIdentity.principles.any { it.contains("usuário") })
        assertTrue(LuoCoreIdentity.principles.any { it.contains("Privacidade") })
        assertTrue(LuoCoreIdentity.principles.any { it.contains("não confiável") })
    }
}
