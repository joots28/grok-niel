package com.otiniel.copy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProviderConfigValidatorTest {
    @Test
    fun versionedBaseUrlBuildsChatCompletionsEndpoint() {
        assertEquals(
            "https://api.x.ai/v1/chat/completions",
            ProviderConfigValidator.completionUrl("https://api.x.ai/v1").value
        )
    }

    @Test
    fun hostOnlyBaseUrlUsesV1Endpoint() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            ProviderConfigValidator.completionUrl("https://example.com").value
        )
    }

    @Test
    fun explicitChatCompletionsEndpointIsNotDuplicated() {
        assertEquals(
            "https://example.com/custom/v1/chat/completions",
            ProviderConfigValidator.completionUrl("https://example.com/custom/v1/chat/completions").value
        )
    }

    @Test
    fun normalizesTrailingSlash() {
        assertEquals(
            "https://api.x.ai/v1",
            ProviderConfigValidator.normalizeBaseUrl("  https://api.x.ai/v1/  ")
        )
    }

    @Test
    fun rejectsInsecureHttpEndpoint() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderConfigValidator.normalizeBaseUrl("http://example.com/v1")
        }
    }

    @Test
    fun rejectsCredentialsInsideUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderConfigValidator.normalizeBaseUrl("https://user:pass@example.com/v1")
        }
    }

    @Test
    fun rejectsQueryParametersInBaseUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderConfigValidator.normalizeBaseUrl("https://example.com/v1?api_key=secret")
        }
    }

    @Test
    fun rejectsBlankModelId() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderConfigValidator.normalizeModel("  ")
        }
    }
}
