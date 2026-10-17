package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.domain.blockaid.models.transaction.simultation.ApproveInfo
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.domain.blockaid.models.transaction.simultation.TokenInfo
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class WcSendAndReceiveBlockAidUiConverterTest {

    private val converter = WcSendAndReceiveBlockAidUiConverter()

    @Test
    fun `GIVEN approve with outgoing transfer WHEN convert THEN estimated wallet changes contain send row`() {
        // Arrange
        val ethToken = TokenInfo(chainId = 1, logoUrl = "eth", symbol = "ETH", decimals = 18)
        val simulation = SimulationData.Approve(
            items = listOf(
                ApproveInfo.Amount(
                    approvedAmount = BigDecimal.ONE,
                    isUnlimited = false,
                    tokenInfo = TokenInfo(chainId = 1, logoUrl = "usdc", symbol = "USDC", decimals = 6),
                ),
            ),
            send = listOf(AmountInfo.FungibleTokens(amount = BigDecimal("5"), token = ethToken)),
        )
        val input = createInput(simulation)

        // Act
        val actual = converter.convert(input)

        // Assert
        val expected = WcEstimatedWalletChangesUM(
            items = persistentListOf(
                WcEstimatedWalletChangeUM(
                    iconRes = R.drawable.ic_send_new_24,
                    title = resourceReference(R.string.common_send),
                    description = "${StringsSigns.MINUS} ${BigDecimal("5").format { crypto("", 18) }} ETH",
                    tokenIconUrl = "eth",
                ),
            ),
        )
        assertThat(actual.estimatedWalletChanges).isEqualTo(expected)
    }

    @Test
    fun `GIVEN fungible approve without transfers WHEN convert THEN estimated wallet changes are null`() {
        // Arrange
        val simulation = SimulationData.Approve(
            items = listOf(
                ApproveInfo.Amount(
                    approvedAmount = BigDecimal.ONE,
                    isUnlimited = false,
                    tokenInfo = TokenInfo(chainId = 1, logoUrl = "usdc", symbol = "USDC", decimals = 6),
                ),
            ),
        )
        val input = createInput(simulation)

        // Act
        val actual = converter.convert(input)

        // Assert
        assertThat(actual.estimatedWalletChanges).isNull()
    }

    private fun createInput(simulation: SimulationData) = WcSendAndReceiveBlockAidUiConverter.Input(
        result = CheckTransactionResult(
            validation = ValidationResult.SAFE,
            description = null,
            simulation = SimulationResult.Success(simulation),
        ),
        approvedAmount = null,
        onApproveLearnMoreClick = {},
    )
}