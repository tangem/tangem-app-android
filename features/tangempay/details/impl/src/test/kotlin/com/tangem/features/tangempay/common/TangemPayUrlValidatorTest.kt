package com.tangem.features.tangempay.common

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TangemPayUrlValidatorTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsLinkTrusted {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN backend link WHEN isLinkTrusted THEN only https links to trusted hosts pass`(model: UrlModel) {
            // Act
            val actual = TangemPayUrlValidator.isLinkTrusted(model.url)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            UrlModel(url = "https://tangem.com/docs/en/tangem-pay-cashback-terms.pdf", expected = true),
            UrlModel(url = "https://www.tangem.com/pricing/?promocode=tgapp20ups", expected = true),
            UrlModel(url = "https://paera.com/tangempay", expected = true),
            UrlModel(url = "https://www.paera.com/tangempay", expected = true),
            UrlModel(url = "https://TANGEM.COM/docs/en/tangem-visa-tariffs.pdf", expected = true),
            UrlModel(url = "tangem://wc?uri=wc:topic@2?relay-protocol=irn&symKey=secret", expected = false),
            UrlModel(url = "tangem://redirect_sell?transactionId=1", expected = false),
            UrlModel(url = "wc:topic@2?relay-protocol=irn&symKey=secret", expected = false),
            UrlModel(url = "market://details?id=com.evil", expected = false),
            UrlModel(url = "intent://scan/#Intent;scheme=tangem;end", expected = false),
            UrlModel(url = "javascript:alert(1)", expected = false),
            UrlModel(url = "http://tangem.com/docs/en/tangem-pay-cashback-terms.pdf", expected = false),
            UrlModel(url = "https://evil.com/tangem.com", expected = false),
            UrlModel(url = "https://tangem.com.evil.com", expected = false),
            UrlModel(url = "https://app.tangem.com", expected = false),
            UrlModel(url = "https://images.us.paera.com/terms.pdf", expected = false),
            UrlModel(url = "https://[REDACTED_EMAIL]", expected = false),
            UrlModel(url = "https://evil.com\\@tangem.com", expected = false),
            UrlModel(url = "https:tangem.com", expected = false),
            UrlModel(url = "", expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsImageUrlSecure {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN merchant icon url WHEN isImageUrlSecure THEN only https urls pass`(model: UrlModel) {
            // Act
            val actual = TangemPayUrlValidator.isImageUrlSecure(model.url)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            UrlModel(url = "https://images.us.paera.com/merchant.png", expected = true),
            UrlModel(url = "https://cdn.example.com/merchant.png", expected = true),
            UrlModel(url = "http://cdn.example.com/merchant.png", expected = false),
            UrlModel(url = "file:///data/data/com.tangem.wallet/files/merchant.png", expected = false),
            UrlModel(url = "content://com.evil.provider/merchant.png", expected = false),
            UrlModel(url = "", expected = false),
        )
    }

    data class UrlModel(val url: String, val expected: Boolean)
}