package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.join.confirmation.ui.state.JointAccountJoinConfirmationUM
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import org.junit.jupiter.api.Test

internal class SetJoinConfirmationTransformerTest {

    private val initialState = createState(confirmation = null)

    @Test
    fun `GIVEN no confirmation WHEN set confirmation THEN confirmation is shown`() {
        // Arrange
        val confirmation = JointAccountJoinConfirmationUM(onContinueClick = {}, onCancelClick = {})
        val transformer = SetJoinConfirmationTransformer(confirmation = confirmation)

        // Act
        val actual = transformer.transform(prevState = initialState)

        // Assert
        assertThat(actual).isEqualTo(initialState.copy(confirmation = confirmation))
    }

    @Test
    fun `GIVEN shown confirmation WHEN set null THEN confirmation is hidden`() {
        // Arrange
        val shownState = createState(
            confirmation = JointAccountJoinConfirmationUM(onContinueClick = {}, onCancelClick = {}),
        )
        val transformer = SetJoinConfirmationTransformer(confirmation = null)

        // Act
        val actual = transformer.transform(prevState = shownState)

        // Assert
        assertThat(actual).isEqualTo(shownState.copy(confirmation = null))
    }

    private fun createState(confirmation: JointAccountJoinConfirmationUM?): JointAccountInvitePreviewUM {
        return JointAccountInvitePreviewUM(
            accountName = "Family savings",
            accountIcon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            requiredToSign = 2,
            totalMembers = 5,
            creatorName = "Igor Sinyak",
            wallet = null,
            confirmation = confirmation,
            onCreatorInfoClick = {},
            onContinueClick = {},
            onCloseClick = {},
        )
    }
}