package com.tangem.common.uri

import android.net.Uri
import com.google.common.truth.Truth
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import java.net.URI

/**
[REDACTED_AUTHOR]
 */
class ExternalUrlValidatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isUriTrusted(model: Model) {
        // Act
        val actual = ExternalUrlValidator.isUriTrusted(uri = createUri(model.url))

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    private fun createUri(url: String): Uri {
        val uri = URI(url)

        return mockk {
            every { scheme } returns uri.scheme
            every { userInfo } returns uri.userInfo
            every { host } returns uri.host
        }
    }

    companion object {

        @JvmStatic
        fun provideTestModels(): Collection<Model> = listOf(
            // Trusted hosts — exact match
            Model(url = "https://tangem.com", expected = true),
            Model(url = "https://tangem.com/pricing/?promocode=tgapp20ups", expected = true),
            Model(url = "https://www.tangem.com", expected = true),
            Model(url = "https://app.tangem.com", expected = true),
            Model(url = "https://buy.tangem.com/?promocode=NEWINAPP", expected = true),
            Model(url = "https://feedback.tangem.com", expected = true),
            Model(url = "https://tangem.surveysparrow.com/s/tangem-pay/tt-F8XXH", expected = true),
            // Hostnames are case-insensitive per RFC 3986
            Model(url = "https://TANGEM.COM/pricing/", expected = true),
            Model(url = "https://Buy.Tangem.com", expected = true),
            // Subdomains not on the list.
            // Note: the AppsFlyer OneLink domains are deliberately absent here — they are attribution redirects,
            // not content, and are allowed only for the push web-link fallback
            // (see DeepLinkFactory.APPSFLYER_ONELINK_HOSTS).
            Model(url = "https://express.tangem.com/v1/", expected = false),
            Model(url = "https://fake.tangem.com", expected = false),
            Model(url = "https://join.tangem.com", expected = false),
            // Sibling hosts on the same registrable parent
            Model(url = "https://surveysparrow.com", expected = false),
            Model(url = "https://fake.surveysparrow.com", expected = false),
            // Suffix-injection attempts
            Model(url = "https://tangem.com.attacker.com", expected = false),
            Model(url = "https://faketangem.com", expected = false),
            Model(url = "https://buy.tangem.com.attacker.com", expected = false),
            // Wrong scheme
            Model(url = "http://tangem.com", expected = false),
            Model(url = "http://buy.tangem.com", expected = false),
            // User info
            Model(url = "https://[REDACTED_EMAIL]", expected = false),
            Model(url = "https://[REDACTED_EMAIL]", expected = false),
            // Typos
            Model(url = "https://tange.com", expected = false),
            Model(url = "http://tandem.com", expected = false),
            // Garbage
            Model(url = "adawdawdassdw", expected = false),
        )

        data class Model(val url: String, val expected: Boolean)
    }
}