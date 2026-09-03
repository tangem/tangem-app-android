package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import org.junit.jupiter.api.Test

internal class SetContentTransformerTest {

    private val firstWallet = MockUserWalletFactory.create().copy(
        walletId = UserWalletId("011"),
        name = "My wallet",
    )
    private val secondWallet = MockUserWalletFactory.create().copy(
        walletId = UserWalletId("012"),
        name = "My wallet 2",
    )

    @Test
    fun `GIVEN preview WHEN transform THEN it is mapped into Content`() {
        // Arrange
        val onCreatorInfoClick = {}
        val onContinueClick = {}
        val onCloseClick = {}
        val transformer = createTransformer(
            wallets = listOf(firstWallet),
            selectedWallet = firstWallet,
            onCreatorInfoClick = onCreatorInfoClick,
            onContinueClick = onContinueClick,
            onCloseClick = onCloseClick,
        )

        // Act
        val actual = transformer.transform(prevState = JointAccountInvitePreviewUM.Loading(onCloseClick = {}))

        // Assert
        val expected = JointAccountInvitePreviewUM.Content(
            accountName = "Family savings",
            accountIcon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            requiredToSign = 2,
            totalMembers = 5,
            creatorName = "Alice",
            creatorAddress = OWNER_ADDRESS,
            wallet = null,
            confirmation = null,
            onCreatorInfoClick = onCreatorInfoClick,
            onContinueClick = onContinueClick,
            onCloseClick = onCloseClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN unknown icon and color WHEN transform THEN they fall back to defaults`() {
        // Arrange
        val transformer = createTransformer(
            preview = preview(icon = "SomeFutureIcon", iconColor = "SomeFutureColor"),
            wallets = listOf(firstWallet),
            selectedWallet = firstWallet,
        )

        // Act
        val actual = transformer.transform(prevState = JointAccountInvitePreviewUM.Loading(onCloseClick = {}))
            as JointAccountInvitePreviewUM.Content

        // Assert
        val expected = AccountIconUM.CryptoPortfolio(
            value = CryptoPortfolioIcon.Icon.Letter,
            color = CryptoPortfolioIcon.Color.Azure,
        )
        assertThat(actual.accountIcon).isEqualTo(expected)
    }

    @Test
    fun `GIVEN single wallet WHEN transform THEN wallet row is hidden`() {
        // Arrange
        val transformer = createTransformer(wallets = listOf(firstWallet), selectedWallet = firstWallet)

        // Act
        val actual = transformer.transform(prevState = JointAccountInvitePreviewUM.Loading(onCloseClick = {}))
            as JointAccountInvitePreviewUM.Content

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
        val actual = transformer.transform(prevState = JointAccountInvitePreviewUM.Loading(onCloseClick = {}))
            as JointAccountInvitePreviewUM.Content

        // Assert
        assertThat(actual.wallet?.name).isEqualTo(secondWallet.name)
    }

    private fun createTransformer(
        preview: JointAccountInvitePreview = preview(),
        wallets: List<UserWallet>,
        selectedWallet: UserWallet?,
        onCreatorInfoClick: () -> Unit = {},
        onContinueClick: () -> Unit = {},
        onCloseClick: () -> Unit = {},
    ) = SetContentTransformer(
        preview = preview,
        wallets = wallets,
        selectedWallet = selectedWallet,
        onCreatorInfoClick = onCreatorInfoClick,
        onContinueClick = onContinueClick,
        onWalletRowClick = {},
        onCloseClick = onCloseClick,
    )

    private fun preview(
        icon: String = "Family",
        iconColor: String = "Azure",
    ) = JointAccountInvitePreview(
        config = JointAccountConfig(
            name = "Family savings",
            icon = icon,
            iconColor = iconColor,
            membersCount = 5,
            threshold = 2,
        ),
        creator = JointAccountInvitePreview.Creator(name = "Alice", address = OWNER_ADDRESS),
    )

    private companion object {
        const val OWNER_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
    }
}