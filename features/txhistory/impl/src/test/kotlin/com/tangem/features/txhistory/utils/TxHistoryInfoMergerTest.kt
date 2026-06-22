package com.tangem.features.txhistory.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryInfoMergerTest {

    @Test
    fun `GIVEN express op matched to on-chain WHEN merge THEN enriched single row and on-chain not duplicated`() {
        // Arrange
        val onChain = listOf(createTxInfo(txHash = "h1", timestamp = 100))
        val express = listOf(createSwap(matchHash = "h1", status = ExpressExchangeStatus.Waiting))

        // Act
        val result = mergeTxHistoryInfos(onChain, express)

        // Assert
        assertThat(result).hasSize(1)
        val row = result.single()
        assertThat(row).isInstanceOf(ExpressTx.Swap::class.java)
        assertThat((row as ExpressTx).txInfo).isInstanceOf(OnChainTx.BSDK::class.java)
    }

    @Test
    fun `GIVEN unmatched active express op WHEN merge THEN standalone live row kept`() {
        // Arrange
        val express = listOf(createSwap(matchHash = "missing", status = ExpressExchangeStatus.Waiting))

        // Act
        val result = mergeTxHistoryInfos(onChain = emptyList(), express = express)

        // Assert
        assertThat(result).hasSize(1)
        assertThat((result.single() as ExpressTx).txInfo).isNull()
    }

    @Test
    fun `GIVEN unmatched terminal express op WHEN merge THEN standalone row kept`() {
        // Arrange
        val express = listOf(createSwap(matchHash = "missing", status = ExpressExchangeStatus.Finished))

        // Act
        val result = mergeTxHistoryInfos(onChain = emptyList(), express = express)

        // Assert
        assertThat(result).hasSize(1)
        val row = result.single()
        assertThat(row).isInstanceOf(ExpressTx.Swap::class.java)
        assertThat((row as ExpressTx).txInfo).isNull()
    }

    @Test
    fun `GIVEN on-chain tx unclaimed by express WHEN merge THEN passed through as OnChain`() {
        // Arrange
        val onChain = listOf(createTxInfo(txHash = "h1", timestamp = 100))

        // Act
        val result = mergeTxHistoryInfos(onChain, express = emptyList())

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result.single()).isInstanceOf(OnChainTx.BSDK::class.java)
    }

    @Test
    fun `GIVEN rows of different timestamps WHEN merge THEN sorted by timestamp descending`() {
        // Arrange
        val onChain = listOf(createTxInfo(txHash = "h1", timestamp = 100))
        val express = listOf(createSwap(matchHash = "missing", createdAtMillis = 200, status = ExpressExchangeStatus.Waiting))

        // Act
        val result = mergeTxHistoryInfos(onChain, express)

        // Assert
        assertThat(result.map { it.timestampMillis }).containsExactly(200L, 100L).inOrder()
    }

    private fun createTxInfo(txHash: String, timestamp: Long) = TxInfo(
        txHash = txHash,
        timestampInMillis = timestamp,
        isOutgoing = true,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User("addr")),
        sourceType = TxInfo.SourceType.Single("addr"),
        interactionAddressType = null,
        status = TxInfo.TransactionStatus.Confirmed,
        type = TxInfo.TransactionType.Transfer,
        amount = BigDecimal.ONE,
    )

    private fun createSwap(
        matchHash: String?,
        status: ExpressExchangeStatus,
        createdAtMillis: Long = 100,
        isOutgoing: Boolean = true,
    ) = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "tx-1",
            status = status,
            createdAtMillis = createdAtMillis,
            provider = null,
            payinHash = matchHash.takeIf { isOutgoing },
            payoutHash = matchHash.takeUnless { isOutgoing },
            fromAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "eth", contractAddress = "0"),
                amount = BigDecimal("1.5"),
                decimals = 18,
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "btc", contractAddress = "0xt"),
                amount = BigDecimal("0.001"),
                decimals = 8,
            ),
        ),
        isOutgoing = isOutgoing,
        txInfo = null,
    )
}