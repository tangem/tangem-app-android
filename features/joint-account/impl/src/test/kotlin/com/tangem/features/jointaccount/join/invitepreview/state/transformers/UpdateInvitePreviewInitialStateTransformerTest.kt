package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.join.confirmation.ui.state.JointAccountJoinConfirmationUM
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import org.junit.jupiter.api.Test

internal class UpdateInvitePreviewInitialStateTransformerTest {

    private val transformer = UpdateInvitePreviewInitialStateTransformer(
        onCreatorInfoClick = onCreatorInfoClick,
        onContinueClick = onContinueClick,
        onCloseClick = onCloseClick,
    )

    @Test
    fun `GIVEN empty state WHEN update initial state THEN only callbacks are wired`() {
        // Act
        val actual = transformer.transform(prevState = emptyState)

        // Assert
        val expected = JointAccountInvitePreviewUM(
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
            onCreatorInfoClick = onCreatorInfoClick,
            onContinueClick = onContinueClick,
            onCloseClick = onCloseClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN state with wallet and confirmation WHEN update initial state THEN they survive`() {
        // Arrange
        val filledState = emptyState.copy(
            wallet = JointAccountInvitePreviewUM.WalletUM(name = "My wallet", onClick = onWalletClick),
            confirmation = JointAccountJoinConfirmationUM(
                onContinueClick = onConfirmationContinueClick,
                onCancelClick = onConfirmationCancelClick,
            ),
        )

        // Act
        val actual = transformer.transform(prevState = filledState)

        // Assert
        val expected = filledState.copy(
            onCreatorInfoClick = onCreatorInfoClick,
            onContinueClick = onContinueClick,
            onCloseClick = onCloseClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private companion object {

        /** The state the controller starts with, before the model wires the callbacks */
        val emptyState = JointAccountInvitePreviewUM(
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

        // Shared instances so the expected state can pin the exact callbacks passed to the transformer
        val onCreatorInfoClick: () -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onCloseClick: () -> Unit = {}
        val onWalletClick: () -> Unit = {}
        val onConfirmationContinueClick: () -> Unit = {}
        val onConfirmationCancelClick: () -> Unit = {}
    }
}