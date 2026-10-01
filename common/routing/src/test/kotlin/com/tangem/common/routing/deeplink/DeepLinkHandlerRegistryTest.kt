package com.tangem.common.routing.deeplink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class DeepLinkHandlerRegistryTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Find {

        private val stakingHandler = DeepLinkHandler {}
        private val newsHandler = DeepLinkHandler {}
        private val newsDetailsHandler = DeepLinkHandler {}
        private val walletConnectHandler = DeepLinkHandler {}

        private val registry = DeepLinkHandlerRegistry(
            handlers = mapOf(
                "tangem://staking" to stakingHandler,
                "https://tangem.com/news" to newsHandler,
                "https://tangem.com/news/details" to newsDetailsHandler,
                "wc:" to walletConnectHandler,
            ),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun find(model: FindModel) {
            // Act
            val actual = registry.find(createUri(scheme = model.scheme, host = model.host, path = model.path))

            // Assert
            assertThat(actual).isSameInstanceAs(model.expected)
        }

        @Test
        fun `GIVEN disabled nested handler WHEN find THEN parent path handler is used`() {
            // Arrange
            var isDetailsEnabled = false
            val toggledDetailsHandler = DeepLinkHandler(isEnabled = { isDetailsEnabled }, handle = {})
            val toggledRegistry = DeepLinkHandlerRegistry(
                handlers = mapOf(
                    "https://tangem.com/news" to newsHandler,
                    "https://tangem.com/news/details" to toggledDetailsHandler,
                ),
            )
            val uri = createUri(scheme = "https", host = "tangem.com", path = "/news/details/1")

            // Act
            val whenDisabled = toggledRegistry.find(uri)
            isDetailsEnabled = true
            val whenEnabled = toggledRegistry.find(uri)

            // Assert
            assertThat(whenDisabled).isSameInstanceAs(newsHandler)
            assertThat(whenEnabled).isSameInstanceAs(toggledDetailsHandler)
        }

        @Test
        fun `GIVEN only disabled handler WHEN find THEN returns null`() {
            // Arrange
            val disabledRegistry = DeepLinkHandlerRegistry(
                handlers = mapOf("tangem://staking" to DeepLinkHandler(isEnabled = { false }, handle = {})),
            )

            // Act
            val actual = disabledRegistry.find(createUri(scheme = "tangem", host = "staking", path = null))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN no handlers WHEN find THEN returns null`() {
            // Arrange
            val emptyRegistry = DeepLinkHandlerRegistry(handlers = emptyMap())

            // Act
            val actual = emptyRegistry.find(createUri(scheme = "tangem", host = "staking", path = null))

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(
            // Host key, query and path do not matter
            FindModel(scheme = "tangem", host = "staking", path = null, expected = stakingHandler),
            FindModel(scheme = "tangem", host = "staking", path = "/extra", expected = stakingHandler),
            // Whole path segments, the most specific key wins
            FindModel(scheme = "https", host = "tangem.com", path = "/news", expected = newsHandler),
            FindModel(scheme = "https", host = "tangem.com", path = "/news/", expected = newsHandler),
            FindModel(scheme = "https", host = "tangem.com", path = "/news/12-slug", expected = newsHandler),
            FindModel(scheme = "https", host = "tangem.com", path = "/news/details", expected = newsDetailsHandler),
            FindModel(scheme = "https", host = "tangem.com", path = "/news/details/1", expected = newsDetailsHandler),
            // A path that only starts with a registered segment is not claimed
            FindModel(scheme = "https", host = "tangem.com", path = "/newsletter", expected = null),
            // Scheme key claims every link of the scheme
            FindModel(scheme = "wc", host = null, path = null, expected = walletConnectHandler),
            FindModel(scheme = "wc", host = "anything", path = "/x", expected = walletConnectHandler),
            // Different scheme of the same host is a different key
            FindModel(scheme = "tangem", host = "wc", path = null, expected = null),
            // Case-sensitive, as received
            FindModel(scheme = "tangem", host = "STAKING", path = null, expected = null),
            // Nothing claimed
            FindModel(scheme = "tangem", host = "unknown", path = null, expected = null),
            FindModel(scheme = "https", host = "tangem.com", path = null, expected = null),
            FindModel(scheme = null, host = "staking", path = null, expected = null),
        )

        private fun createUri(scheme: String?, host: String?, path: String?): Uri = mockk {
            every { this@mockk.scheme } returns scheme
            every { this@mockk.host } returns host
            every { this@mockk.path } returns path
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CandidateKeys {

        @ParameterizedTest
        @ProvideTestModels
        fun candidateKeys(model: CandidateKeysModel) {
            // Act
            val actual = DeepLinkHandlerRegistry.candidateKeys(
                scheme = model.scheme,
                host = model.host,
                path = model.path,
            )

            // Assert
            assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
        }

        private fun provideTestModels() = listOf(
            CandidateKeysModel(
                scheme = "tangem",
                host = "staking",
                path = null,
                expected = listOf("tangem://staking", "tangem:"),
            ),
            CandidateKeysModel(
                scheme = "https",
                host = "tangem.com",
                path = "/joint-account/join",
                expected = listOf(
                    "https://tangem.com/joint-account/join",
                    "https://tangem.com/joint-account",
                    "https://tangem.com",
                    "https:",
                ),
            ),
            CandidateKeysModel(
                scheme = "https",
                host = "tangem.com",
                path = "//news//",
                expected = listOf("https://tangem.com/news", "https://tangem.com", "https:"),
            ),
            CandidateKeysModel(scheme = "wc", host = null, path = null, expected = listOf("wc:")),
            CandidateKeysModel(scheme = "wc", host = "", path = "/x", expected = listOf("wc:")),
            CandidateKeysModel(scheme = null, host = "staking", path = null, expected = emptyList()),
            CandidateKeysModel(scheme = "", host = "staking", path = null, expected = emptyList()),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NormalizeKey {

        @ParameterizedTest
        @ProvideTestModels
        fun normalizeKey(model: NormalizeKeyModel) {
            // Act
            val actual = runCatching { DeepLinkHandlerRegistry.normalizeKey(model.key) }

            // Assert
            if (model.expected == null) {
                assertThat(actual.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
            } else {
                assertThat(actual.getOrNull()).isEqualTo(model.expected)
            }
        }

        private fun provideTestModels() = listOf(
            // Already normalized
            NormalizeKeyModel(key = "tangem://staking", expected = "tangem://staking"),
            NormalizeKeyModel(
                key = "https://tangem.com/joint-account/join",
                expected = "https://tangem.com/joint-account/join",
            ),
            NormalizeKeyModel(key = "wc:", expected = "wc:"),
            // Trailing and doubled slashes do not matter
            NormalizeKeyModel(key = "tangem://staking/", expected = "tangem://staking"),
            NormalizeKeyModel(key = "https://tangem.com//news//", expected = "https://tangem.com/news"),
            NormalizeKeyModel(key = "https://tangem.com/", expected = "https://tangem.com"),
            // Malformed
            NormalizeKeyModel(key = "", expected = null),
            NormalizeKeyModel(key = "staking", expected = null),
            NormalizeKeyModel(key = "tangem:/staking", expected = null),
            NormalizeKeyModel(key = "tangem://", expected = null),
            NormalizeKeyModel(key = "tangem://staking?network_id=eth", expected = null),
            NormalizeKeyModel(key = "https://tangem.com/news#top", expected = null),
            NormalizeKeyModel(key = " tangem://staking", expected = null),
            NormalizeKeyModel(key = "wc:abc", expected = null),
        )
    }

    @Nested
    inner class Construction {

        @Test
        fun `GIVEN key with trailing slash WHEN find THEN handler is found`() {
            // Arrange
            val handler = DeepLinkHandler {}
            val registry = DeepLinkHandlerRegistry(handlers = mapOf("tangem://staking/" to handler))
            val uri = mockk<Uri> {
                every { scheme } returns "tangem"
                every { host } returns "staking"
                every { path } returns null
            }

            // Act
            val actual = registry.find(uri)

            // Assert
            assertThat(actual).isSameInstanceAs(handler)
        }

        @Test
        fun `GIVEN keys equal after normalization WHEN registry created THEN fails`() {
            // Act
            val actual = runCatching {
                DeepLinkHandlerRegistry(
                    handlers = mapOf("tangem://staking" to mockk(), "tangem://staking/" to mockk()),
                )
            }

            // Assert
            assertThat(actual.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `GIVEN malformed key WHEN registry created THEN fails`() {
            // Act
            val actual = runCatching { DeepLinkHandlerRegistry(handlers = mapOf("tangem:/staking" to mockk())) }

            // Assert
            assertThat(actual.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    data class NormalizeKeyModel(val key: String, val expected: String?)

    data class FindModel(
        val scheme: String?,
        val host: String?,
        val path: String?,
        val expected: DeepLinkHandler?,
    )

    data class CandidateKeysModel(
        val scheme: String?,
        val host: String?,
        val path: String?,
        val expected: List<String>,
    )
}