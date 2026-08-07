package com.tangem.common.uri

import com.google.common.truth.Truth
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
class ExternalUrlValidatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun test(model: Model) {
        val actual = ExternalUrlValidator.isUriTrusted(externalUri = model.url)

        Truth.assertThat(actual).isEqualTo(model.expected)
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
            // Typos
            Model(url = "https://tange.com", expected = false),
            Model(url = "http://tandem.com", expected = false),
            // Garbage
            Model(url = "adawdawdassdw", expected = false),
        )

        data class Model(val url: String, val expected: Boolean)
    }
}