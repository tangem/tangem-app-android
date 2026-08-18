package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import org.junit.jupiter.api.Test

internal class UpdateInvitePreviewWalletsTransformerTest {

    private val firstWallet = MockUserWalletFactory.create().copy(
        walletId = UserWalletId("011"),
        name = "My wallet",
    )
    private val secondWallet = MockUserWalletFactory.create().copy(
        walletId = UserWalletId("012"),
        name = "My wallet 2",
    )

    private val initialState = JointAccountInvitePreviewUM(
        accountName = "Family savings",
        accountIcon = AccountIconUM.CryptoPortfolio(
            value = CryptoPortfolioIcon.Icon.Family,
            color = CryptoPortfolioIcon.Color.Azure,
        ),
        requiredToSign = 2,
        totalMembers = 5,
        creatorName = "Igor Sinyak",
        wallet = null,
        confirmation = null,
        onCreatorInfoClick = {},
        onContinueClick = {},
        onCloseClick = {},
    )

    @Test
    fun `GIVEN single wallet WHEN transform THEN wallet row is hidden`() {
        // Arrange
        val transformer = createTransformer(wallets = listOf(firstWallet), selectedWallet = firstWallet)

        // Act
        val actual = transformer.transform(prevState = initialState)

        // Assert
        assertThat(actual.wallet).isNull()
    }

    @Test
    fun `GIVEN two wallets WHEN transform THEN wallet row shows the selected wallet name`() {
        // Arrange
        val transformer = createTransformer(
            wallets = listOf(firstWallet, secondWallet),
            selectedWallet = secondWallet,
        )

        // Act
        val actual = transformer.transform(prevState = initialState)

        // Assert
        assertThat(actual.wallet?.name).isEqualTo(secondWallet.name)
    }

    @Test
    fun `GIVEN no selected wallet WHEN transform THEN wallet row is hidden`() {
        // Arrange
        val transformer = createTransformer(wallets = listOf(firstWallet, secondWallet), selectedWallet = null)

        // Act
        val actual = transformer.transform(prevState = initialState)

        // Assert
        assertThat(actual.wallet).isNull()
    }

    private fun createTransformer(
        wallets: List<UserWallet>,
        selectedWallet: UserWallet?,
    ): UpdateInvitePreviewWalletsTransformer = UpdateInvitePreviewWalletsTransformer(
        wallets = wallets,
        selectedWallet = selectedWallet,
        onWalletRowClick = {},
    )
}