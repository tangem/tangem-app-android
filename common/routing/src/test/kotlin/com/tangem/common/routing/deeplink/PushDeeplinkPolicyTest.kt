package com.tangem.common.routing.deeplink

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PushDeeplinkPolicyTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun isOpenableFromPush(model: Model) {
        // Act
        val actual = PushDeeplinkPolicy.isOpenableFromPush(scheme = model.scheme, host = model.host)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val scheme: String?, val host: String?, val expected: Boolean)

    private fun provideTestModels() = listOf(
        // Blocked — the sell redirect (Send prefill) and WalletConnect pairing
        Model(scheme = "tangem", host = "redirect_sell", expected = false),
        Model(scheme = "tangem", host = "wc", expected = false),
        Model(scheme = "wc", host = null, expected = false),
        Model(scheme = "wc", host = "anything", expected = false),
        Model(scheme = "WC", host = null, expected = false),
        // Allowed — everything else, including routes once flagged sensitive
        Model(scheme = "tangem", host = "main", expected = true),
        Model(scheme = "tangem", host = "token", expected = true),
        Model(scheme = "tangem", host = "staking", expected = true),
        Model(scheme = "tangem", host = "promo", expected = true),
        Model(scheme = "tangem", host = "sell", expected = true),
        Model(scheme = "tangem", host = "buy", expected = true),
        Model(scheme = "tangem", host = "swap", expected = true),
        Model(scheme = "tangem", host = "onramp", expected = true),
        Model(scheme = "tangem", host = "onboard-visa", expected = true),
        Model(scheme = "tangem", host = "survey", expected = true),
        Model(scheme = "tangem", host = "pay-app-main", expected = true),
        Model(scheme = "https", host = "tangem.com", expected = true),
        // Allowed — this policy only denies the sensitive routes above. An unknown host is not harmless on its
        // own any more: for a push it now reaches DeepLinkFactory's web fallback, where the host allowlist
        // (ExternalUrlValidator + APPSFLYER_ONELINK_HOSTS) is what stops it from opening in a browser.
        Model(scheme = "https", host = "evil.com", expected = true),
        Model(scheme = "tangem", host = "unknown_route", expected = true),
        Model(scheme = "tangem", host = null, expected = true),
        Model(scheme = null, host = null, expected = true),
    )
}