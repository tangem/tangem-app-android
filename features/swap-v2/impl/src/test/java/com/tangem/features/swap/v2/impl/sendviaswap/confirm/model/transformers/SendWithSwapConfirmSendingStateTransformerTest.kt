package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendWithSwapConfirmSendingStateTransformerTest {

    @Test
    fun `GIVEN confirmUM is not Content WHEN transform THEN prevState returned unchanged`() {
        // Arrange
        val prevState = buildState(confirmUM = ConfirmUM.Empty)
        val transformer = SendWithSwapConfirmSendingStateTransformer(isTransactionInProcess = true)

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        assertThat(actual).isSameInstanceAs(prevState)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN confirmUM is Content WHEN transform THEN only in-process and primary button flags updated`(
        model: SendingModel,
    ) {
        // Arrange
        val confirmUM = buildContent()
        val prevState = buildState(confirmUM = confirmUM)
        val transformer = SendWithSwapConfirmSendingStateTransformer(
            isTransactionInProcess = model.isTransactionInProcess,
        )
        val expected = prevState.copy(
            confirmUM = confirmUM.copy(
                isPrimaryButtonEnabled = model.expectedPrimaryButtonEnabled,
                isTransactionInProcess = model.isTransactionInProcess,
            ),
        )

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        SendingModel(isTransactionInProcess = true, expectedPrimaryButtonEnabled = false),
        SendingModel(isTransactionInProcess = false, expectedPrimaryButtonEnabled = true),
    )

    private fun buildState(confirmUM: ConfirmUM) = SendWithSwapUM(
        amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct),
        destinationUM = DestinationUM.Empty(isPrimaryButtonEnabled = true),
        feeSelectorUM = FeeSelectorUM.Loading,
        confirmUM = confirmUM,
    )

    private fun buildContent() = ConfirmUM.Content(
        isPrimaryButtonEnabled = true,
        isTransactionInProcess = false,
        isShowTapHelp = true,
        sendingFooter = stringReference("footer"),
        notifications = persistentListOf(NotificationUM.Warning.HighNetworkFee),
        tosUM = ConfirmUM.Content.TosUM(
            tosLink = ConfirmUM.Content.LegalUM(title = stringReference("Terms"), link = "https://terms"),
            policyLink = null,
        ),
    )

    data class SendingModel(val isTransactionInProcess: Boolean, val expectedPrimaryButtonEnabled: Boolean)
}