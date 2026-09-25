package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendWithSwapConfirmSentStateTransformerTest {

    private val provider = ExpressProvider(
        providerId = "sent-provider",
        name = "Sent Provider",
        type = ExpressProviderType.DEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )
    private val swapDataModel = SwapDataModel(toTokenAmount = BigDecimal("42"), transaction = mockk())

    private val amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Reverse)
    private val destinationUM = DestinationUM.Empty(isPrimaryButtonEnabled = true)
    private val feeSelectorUM = FeeSelectorUM.Loading

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN any confirmUM WHEN transform THEN confirmUM replaced with Success carrying prevState subcomponents`(
        case: PrevConfirmUMCase,
    ) {
        // Arrange
        val prevState = SendWithSwapUM(
            amountUM = amountUM,
            destinationUM = destinationUM,
            feeSelectorUM = feeSelectorUM,
            confirmUM = case.confirmUM,
        )
        val transformer = SendWithSwapConfirmSentStateTransformer(
            txUrl = TX_URL,
            timestamp = TIMESTAMP,
            provider = provider,
            swapDataModel = swapDataModel,
        )
        val expected = SendWithSwapUM(
            amountUM = amountUM,
            destinationUM = destinationUM,
            feeSelectorUM = feeSelectorUM,
            confirmUM = ConfirmUM.Success(
                isPrimaryButtonEnabled = true,
                transactionDate = TIMESTAMP,
                txUrl = TX_URL,
                swapDataModel = swapDataModel,
                provider = provider,
                amountUM = amountUM,
                destinationUM = destinationUM,
                feeSelectorUM = feeSelectorUM,
            ),
        )

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels(): List<PrevConfirmUMCase> = listOf(
        PrevConfirmUMCase(description = "Empty", confirmUM = ConfirmUM.Empty),
        PrevConfirmUMCase(
            description = "Content",
            confirmUM = ConfirmUM.Content(
                isPrimaryButtonEnabled = false,
                isTransactionInProcess = true,
                isShowTapHelp = false,
                sendingFooter = TextReference.EMPTY,
                notifications = persistentListOf(),
                tosUM = null,
            ),
        ),
    )

    data class PrevConfirmUMCase(val description: String, val confirmUM: ConfirmUM) {
        override fun toString(): String = description
    }

    private companion object {
        const val TX_URL = "https://explorer/tx/0xabc"
        const val TIMESTAMP = 1_700_000_000_000L
    }
}