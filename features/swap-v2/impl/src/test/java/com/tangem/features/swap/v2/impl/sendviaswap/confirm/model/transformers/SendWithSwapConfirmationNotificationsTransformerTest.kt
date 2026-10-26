package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.CustomFeeFieldUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendWithSwapConfirmationNotificationsTransformerTest {

    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true)

    private val fees = TransactionFee.Choosable(
        normal = fee(value = "3"),
        minimum = fee(value = MINIMUM_FEE),
        priority = fee(value = PRIORITY_FEE),
    )

    private val contentConfirmUM = ConfirmUM.Content(
        isPrimaryButtonEnabled = true,
        isTransactionInProcess = false,
        isShowTapHelp = false,
        sendingFooter = stringReference("previous footer"),
        notifications = persistentListOf(NotificationUM.Warning.FeeTooLow),
        tosUM = ConfirmUM.Content.TosUM(
            tosLink = ConfirmUM.Content.LegalUM(title = stringReference("Terms"), link = "https://terms"),
            policyLink = null,
        ),
    )

    @Test
    fun `GIVEN confirmUM is not Content WHEN transform THEN prevState returned unchanged`() {
        // Arrange
        val prevState = buildState(
            confirmUM = ConfirmUM.Empty,
            feeSelectorUM = buildFeeSelector(selectedFeeItem = FeeItem.Market(fees.normal)),
        )
        val transformer = SendWithSwapConfirmationNotificationsTransformer(isHighNetworkFee = true)

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        assertThat(actual).isSameInstanceAs(prevState)
    }

    @Test
    fun `GIVEN feeSelectorUM is not Content WHEN transform THEN prevState returned unchanged`() {
        // Arrange
        val prevState = buildState(confirmUM = contentConfirmUM, feeSelectorUM = FeeSelectorUM.Loading)
        val transformer = SendWithSwapConfirmationNotificationsTransformer(isHighNetworkFee = true)

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        assertThat(actual).isSameInstanceAs(prevState)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN selected fee and high network fee flag WHEN transform THEN notifications rebuilt in order`(
        model: NotificationsModel,
    ) {
        // Arrange
        val prevState = buildState(
            confirmUM = contentConfirmUM,
            feeSelectorUM = buildFeeSelector(selectedFeeItem = model.selectedFeeItem),
        )
        val transformer = SendWithSwapConfirmationNotificationsTransformer(isHighNetworkFee = model.isHighNetworkFee)
        val expected = prevState.copy(
            confirmUM = contentConfirmUM.copy(
                sendingFooter = TextReference.EMPTY,
                tosUM = null,
                notifications = model.expectedNotifications.toPersistentList(),
            ),
        )

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        NotificationsModel(
            selectedFeeItem = FeeItem.Market(fees.normal),
            isHighNetworkFee = true,
            expectedNotifications = listOf(NotificationUM.Warning.HighNetworkFee),
        ),
        NotificationsModel(
            selectedFeeItem = FeeItem.Market(fees.normal),
            isHighNetworkFee = false,
            expectedNotifications = emptyList(),
        ),
        NotificationsModel(
            selectedFeeItem = customFeeItem(value = "3"),
            isHighNetworkFee = false,
            expectedNotifications = emptyList(),
        ),
        NotificationsModel(
            selectedFeeItem = customFeeItem(value = "1"),
            isHighNetworkFee = false,
            expectedNotifications = listOf(NotificationUM.Warning.FeeTooLow),
        ),
        NotificationsModel(
            selectedFeeItem = customFeeItem(value = "1"),
            isHighNetworkFee = true,
            expectedNotifications = listOf(NotificationUM.Warning.FeeTooLow, NotificationUM.Warning.HighNetworkFee),
        ),
        NotificationsModel(
            selectedFeeItem = customFeeItem(value = "40"),
            isHighNetworkFee = false,
            expectedNotifications = listOf(NotificationUM.Warning.TooHigh(value = "10")),
        ),
        NotificationsModel(
            selectedFeeItem = customFeeItem(value = "40"),
            isHighNetworkFee = true,
            expectedNotifications = listOf(
                NotificationUM.Warning.TooHigh(value = "10"),
                NotificationUM.Warning.HighNetworkFee,
            ),
        ),
    )

    private fun buildState(confirmUM: ConfirmUM, feeSelectorUM: FeeSelectorUM) = SendWithSwapUM(
        amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct),
        destinationUM = DestinationUM.Empty(),
        feeSelectorUM = feeSelectorUM,
        confirmUM = confirmUM,
    )

    private fun buildFeeSelector(selectedFeeItem: FeeItem) = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = true,
        fees = fees,
        feeItems = persistentListOf(selectedFeeItem),
        selectedFeeItem = selectedFeeItem,
        feeExtraInfo = FeeExtraInfo(
            isFeeApproximate = false,
            isFeeConvertibleToFiat = true,
            isTronToken = false,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        ),
        feeFiatRateUM = null,
        feeNonce = FeeNonce.None,
    )

    private fun customFeeItem(value: String) = FeeItem.Custom(
        fee = fee(value = value),
        customValues = persistentListOf(
            CustomFeeFieldUM(
                value = value,
                onValueChange = {},
                keyboardOptions = KeyboardOptions.Default,
                keyboardActions = KeyboardActions.Default,
                symbol = FEE_SYMBOL,
                decimals = FEE_DECIMALS,
                title = TextReference.EMPTY,
                footer = TextReference.EMPTY,
            ),
        ),
    )

    private fun fee(value: String) = Fee.Common(
        amount = Amount(currencySymbol = FEE_SYMBOL, value = BigDecimal(value), decimals = FEE_DECIMALS),
    )

    data class NotificationsModel(
        val selectedFeeItem: FeeItem,
        val isHighNetworkFee: Boolean,
        val expectedNotifications: List<NotificationUM>,
    )

    private companion object {
        const val FEE_SYMBOL = "ETH"
        const val FEE_DECIMALS = 18
        const val MINIMUM_FEE = "2"
        const val PRIORITY_FEE = "4"
    }
}