package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxInfoToTxHistoryDetailsUMConverterTest {

    private val converter = TxInfoToTxHistoryDetailsUMConverter()

    @Test
    fun `GIVEN Swap WHEN convert THEN TwoAssets`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Swap)

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.TwoAssets::class.java)
    }

    @Test
    fun `GIVEN non-Swap TransactionType WHEN convert THEN SingleAsset`() {
        val nonSwapTypes = listOf(
            TransactionType.Transfer,
            TransactionType.Approve,
            TransactionType.Operation(name = "Mint NFT"),
            TransactionType.UnknownOperation,
            TransactionType.GaslessFee,
            TransactionType.Staking.Stake,
            TransactionType.Staking.ClaimRewards,
            TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS),
            TransactionType.YieldSupply.Topup,
            TransactionType.YieldSupply.Enter(address = USER_ADDRESS),
        )

        nonSwapTypes.forEach { type ->
            val result = converter.convert(txInfo(type = type))

            assertThat(result).isInstanceOf(TxHistoryDetailsUM.SingleAsset::class.java)
        }
    }

    @Test
    fun `GIVEN tx WHEN convert THEN title is the transaction type`() {
        // Arrange
        val type = TransactionType.Transfer
        val tx = txInfo(type = type)

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result.title).isEqualTo(type.toString())
    }

    private fun txInfo(type: TransactionType): TxInfo = TxInfo(
        txHash = TX_HASH,
        timestampInMillis = TIMESTAMP,
        isOutgoing = false,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
        sourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
        interactionAddressType = null,
        status = TxInfo.TransactionStatus.Confirmed,
        type = type,
        amount = BigDecimal.ONE,
    )

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val USER_ADDRESS = "0x1234567890abcdef1234"
        const val VALIDATOR_ADDRESS = "0xvalidator"
    }
}