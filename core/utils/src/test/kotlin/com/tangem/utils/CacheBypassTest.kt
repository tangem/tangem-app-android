package com.tangem.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CacheBypassTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN url WHEN withCacheBypass THEN timestamp query param is appended`(model: WithCacheBypassModel) {
        // Act
        val actual = model.url.withCacheBypass(timestamp = TIMESTAMP)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        WithCacheBypassModel(
            url = "https://tangem.com/docs/en/privacy-policy.pdf",
            expected = "https://tangem.com/docs/en/privacy-policy.pdf?t=$TIMESTAMP",
        ),
        WithCacheBypassModel(
            url = "https://tangem.com/docs/en/privacy-policy.pdf?lang=en",
            expected = "https://tangem.com/docs/en/privacy-policy.pdf?lang=en&t=$TIMESTAMP",
        ),
        WithCacheBypassModel(
            url = "https://tangem.com/docs/en/privacy-policy.pdf#page=2",
            expected = "https://tangem.com/docs/en/privacy-policy.pdf?t=$TIMESTAMP#page=2",
        ),
        WithCacheBypassModel(
            url = "https://tangem.com/docs/en/privacy-policy.pdf?lang=en#page=2",
            expected = "https://tangem.com/docs/en/privacy-policy.pdf?lang=en&t=$TIMESTAMP#page=2",
        ),
    )

    data class WithCacheBypassModel(val url: String, val expected: String)

    private companion object {
        const val TIMESTAMP = 1_700_000_000_000L
    }
}