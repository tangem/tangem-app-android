package com.tangem.domain.models.network

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.TxInfo.TransactionStatus
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxInfoMultiTransferTest {

    @ParameterizedTest
    @ProvideTestModels
    fun status(model: StatusModel) {
        // Act
        val actual = multiTransfer(model.legs).status

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        // The fee leg listed first must not speak for the failed transfer it paid for
        StatusModel(
            legs = listOf(
                leg(TransactionType.GaslessFee, TransactionStatus.Confirmed),
                leg(TransactionType.Transfer, TransactionStatus.Failed),
            ),
            expected = TransactionStatus.Failed,
        ),
        StatusModel(
            legs = listOf(
                leg(TransactionType.Transfer, TransactionStatus.Unconfirmed),
                leg(TransactionType.GaslessFee, TransactionStatus.Confirmed),
            ),
            expected = TransactionStatus.Unconfirmed,
        ),
        StatusModel(
            legs = listOf(
                leg(TransactionType.GaslessFee, TransactionStatus.Confirmed),
                leg(TransactionType.GaslessFee, TransactionStatus.Failed),
            ),
            expected = TransactionStatus.Confirmed,
        ),
    )

    private fun multiTransfer(legs: List<TxInfo.Single>) = TxInfo.MultiTransfer(
        amount = BigDecimal.ONE,
        isOutgoing = true,
        type = TransactionType.Transfer,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User(RECIPIENT)),
        sourceType = TxInfo.SourceType.Single(SENDER),
        interactionAddressType = null,
        transfers = legs,
    )

    private fun leg(type: TransactionType, status: TransactionStatus) = TxInfo.Single(
        txHash = "0xhash",
        timestampInMillis = 0L,
        isOutgoing = true,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User(RECIPIENT)),
        sourceType = TxInfo.SourceType.Single(SENDER),
        interactionAddressType = null,
        status = status,
        type = type,
        amount = BigDecimal.ONE,
    )

    internal data class StatusModel(val legs: List<TxInfo.Single>, val expected: TransactionStatus)

    private companion object {
        const val SENDER = "0xsender"
        const val RECIPIENT = "0xrecipient"
    }
}