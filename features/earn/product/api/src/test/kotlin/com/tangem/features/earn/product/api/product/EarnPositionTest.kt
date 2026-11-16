package com.tangem.features.earn.product.api.product

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnPositionTest {

    private val ethId = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum")
    private val usdtId = CryptoCurrency.ID.fromValue("token⟨ETH⟩tether⚓0xdAC17F958D2ee523a2206206994597C13D831ec7")
    private val address = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(value = "0xabc", type = NetworkAddress.Address.Type.Primary),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN network status WHEN hasOutgoingPendingTransaction THEN result matches pending transactions`(
        model: HasOutgoingPendingTransactionModel,
    ) {
        // Act
        val actual = model.status.hasOutgoingPendingTransaction(ethId)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        HasOutgoingPendingTransactionModel(
            name = "verified with outgoing pending transaction of the currency",
            status = createVerified(pendingTransactions = mapOf(ethId to setOf(createTxInfo(isOutgoing = true)))),
            expected = true,
        ),
        HasOutgoingPendingTransactionModel(
            name = "verified with outgoing and incoming pending transactions of the currency",
            status = createVerified(
                pendingTransactions = mapOf(
                    ethId to setOf(
                        createTxInfo(txHash = "0xincoming", isOutgoing = false),
                        createTxInfo(txHash = "0xoutgoing", isOutgoing = true),
                    ),
                ),
            ),
            expected = true,
        ),
        HasOutgoingPendingTransactionModel(
            name = "verified with outgoing pending multi transfer of the currency",
            status = createVerified(
                pendingTransactions = mapOf(ethId to setOf(createMultiTransfer(isOutgoing = true))),
            ),
            expected = true,
        ),
        HasOutgoingPendingTransactionModel(
            name = "verified with incoming pending transaction of the currency",
            status = createVerified(pendingTransactions = mapOf(ethId to setOf(createTxInfo(isOutgoing = false)))),
            expected = false,
        ),
        HasOutgoingPendingTransactionModel(
            name = "verified with outgoing pending transaction of another currency only",
            status = createVerified(pendingTransactions = mapOf(usdtId to setOf(createTxInfo(isOutgoing = true)))),
            expected = false,
        ),
        HasOutgoingPendingTransactionModel(
            name = "verified with empty pending set of the currency",
            status = createVerified(pendingTransactions = mapOf(ethId to emptySet())),
            expected = false,
        ),
        HasOutgoingPendingTransactionModel(
            name = "verified without pending transactions",
            status = createVerified(pendingTransactions = emptyMap()),
            expected = false,
        ),
        HasOutgoingPendingTransactionModel(
            name = "no account",
            status = NetworkStatus.NoAccount(
                address = address,
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "No account",
                source = StatusSource.ACTUAL,
            ),
            expected = false,
        ),
        HasOutgoingPendingTransactionModel(
            name = "unreachable with address",
            status = NetworkStatus.Unreachable(address = address),
            expected = false,
        ),
        HasOutgoingPendingTransactionModel(
            name = "unreachable without address",
            status = NetworkStatus.Unreachable(address = null),
            expected = false,
        ),
        HasOutgoingPendingTransactionModel(
            name = "missed derivation",
            status = NetworkStatus.MissedDerivation,
            expected = false,
        ),
    )

    private fun createVerified(
        pendingTransactions: Map<CryptoCurrency.ID, Set<TxInfo>> = emptyMap(),
        source: StatusSource = StatusSource.ACTUAL,
    ) = NetworkStatus.Verified(
        address = address,
        amounts = emptyMap(),
        pendingTransactions = pendingTransactions,
        yieldSupplyStatuses = emptyMap(),
        source = source,
    )

    private fun createTxInfo(txHash: String = "0xhash", isOutgoing: Boolean = true) = TxInfo.Single(
        txHash = txHash,
        timestampInMillis = 1_700_000_000_000L,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User("0xdestination")),
        sourceType = TxInfo.SourceType.Single("0xsource"),
        interactionAddressType = null,
        status = TxInfo.TransactionStatus.Unconfirmed,
        type = TxInfo.TransactionType.Transfer,
        amount = BigDecimal.ONE,
    )

    private fun createMultiTransfer(isOutgoing: Boolean = true) = TxInfo.MultiTransfer(
        amount = BigDecimal.ONE,
        isOutgoing = isOutgoing,
        type = TxInfo.TransactionType.Transfer,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User("0xdestination")),
        sourceType = TxInfo.SourceType.Single("0xsource"),
        interactionAddressType = null,
        transfers = listOf(createTxInfo(isOutgoing = isOutgoing)),
    )

    internal data class HasOutgoingPendingTransactionModel(
        val name: String,
        val status: NetworkStatus.Value,
        val expected: Boolean,
    ) {
        override fun toString(): String = "$name -> $expected"
    }
}