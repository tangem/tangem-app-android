package com.tangem.domain.wallets.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.wallets.analytics.Settings.NoticeMobileWalletLimit
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettingsTest {

    @Test
    fun `GIVEN connections button WHEN created THEN id and params match the spec`() {
        // Act
        val event = Settings.ButtonConnections()

        // Assert
        assertThat(event.id to event.params).isEqualTo("[Settings] Button - Connections" to emptyMap<String, String>())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a type and a limit WHEN NoticeMobileWalletLimit built THEN it carries the exact params`(
        model: NoticeModel,
    ) {
        // Act
        val actual = NoticeMobileWalletLimit(type = model.type, limit = model.limit)

        // Assert
        assertThat(actual.id).isEqualTo("[Settings] Notice - Mobile Wallet Limit")
        assertThat(actual.params).isEqualTo(model.expectedParams)
    }

    private fun provideTestModels() = listOf(
        NoticeModel(
            type = NoticeMobileWalletLimit.Type.Create,
            limit = NoticeMobileWalletLimit.Limit.MobileWallets,
            expectedParams = mapOf("Type" to "Create", "Limit" to "Mobile Wallets"),
        ),
        NoticeModel(
            type = NoticeMobileWalletLimit.Type.Import,
            limit = NoticeMobileWalletLimit.Limit.MobileWallets,
            expectedParams = mapOf("Type" to "Import", "Limit" to "Mobile Wallets"),
        ),
        NoticeModel(
            type = NoticeMobileWalletLimit.Type.SocialLogin,
            limit = NoticeMobileWalletLimit.Limit.MobileWallets,
            expectedParams = mapOf("Type" to "Social Login", "Limit" to "Mobile Wallets"),
        ),
        NoticeModel(
            type = NoticeMobileWalletLimit.Type.SocialLogin,
            limit = NoticeMobileWalletLimit.Limit.SocialLogin,
            expectedParams = mapOf("Type" to "Social Login", "Limit" to "Social Login"),
        ),
    )

    data class NoticeModel(
        val type: NoticeMobileWalletLimit.Type,
        val limit: NoticeMobileWalletLimit.Limit,
        val expectedParams: Map<String, String>,
    )
}