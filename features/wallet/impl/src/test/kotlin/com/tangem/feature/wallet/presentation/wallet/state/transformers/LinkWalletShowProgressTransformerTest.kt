package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.createWalletContentUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LinkWalletShowProgressTransformerTest {

    private val userWalletId = UserWalletId("01")
    private val onLinkClick: () -> Unit = {}

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN link banner shown WHEN transform THEN only its progress changes`(model: ProgressModel) {
        // Arrange
        val walletUM = createWalletContentUM(
            userWalletId = userWalletId,
            notifications = persistentListOf(linkBanner(shouldShowProgress = !model.shouldShowProgress), otherBanner),
        )
        val expected = createWalletContentUM(
            userWalletId = userWalletId,
            notifications = persistentListOf(linkBanner(shouldShowProgress = model.shouldShowProgress), otherBanner),
        )

        // Act
        val actual = LinkWalletShowProgressTransformer(userWalletId, model.shouldShowProgress).transform(walletUM)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        ProgressModel(shouldShowProgress = true),
        ProgressModel(shouldShowProgress = false),
    )

    private fun linkBanner(shouldShowProgress: Boolean) = WalletNotificationUM.LinkWalletToDevice(
        tangemIcon = null,
        isHotWallet = true,
        shouldShowProgress = shouldShowProgress,
        onLinkClick = onLinkClick,
    )

    private val otherBanner = WalletNotificationUM.SomeNetworksUnreachable

    data class ProgressModel(val shouldShowProgress: Boolean)
}