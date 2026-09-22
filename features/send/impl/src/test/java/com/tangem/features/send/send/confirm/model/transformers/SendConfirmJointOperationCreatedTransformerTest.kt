package com.tangem.features.send.send.confirm.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.common.ui.state.SendSignaturesUM
import com.tangem.features.send.send.ui.state.SendUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class SendConfirmJointOperationCreatedTransformerTest {

    @Test
    fun `GIVEN a joint confirm WHEN the operation is created THEN success carries the date and the signatures`() {
        // Arrange
        val signaturesUM = SendSignaturesUM(threshold = 2, membersCount = 3)
        val prevState = createSendUM(
            confirmUM = ConfirmUM.Content(
                walletName = TextReference.EMPTY,
                isSending = true,
                isShowTapHelp = false,
                sendingFooter = TextReference.EMPTY,
                notifications = persistentListOf(),
                signaturesUM = signaturesUM,
            ),
        )

        // Act
        val actual = SendConfirmJointOperationCreatedTransformer(createdAt = 1_700_000_000_000L).transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(
            prevState.copy(
                confirmUM = ConfirmUM.Success(
                    transactionDate = 1_700_000_000_000L,
                    txUrl = "",
                    signaturesUM = signaturesUM,
                ),
            ),
        )
    }

    private fun createSendUM(confirmUM: ConfirmUM) = SendUM(
        amountUM = AmountState.Empty,
        destinationUM = DestinationUM.Empty(),
        feeSelectorUM = FeeSelectorUM.Loading,
        confirmUM = confirmUM,
        confirmData = null,
    )
}