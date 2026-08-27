package com.tangem.data.txhistory.list

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryInfoMergerTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeterministicHashMatch {

        @Test
        fun `GIVEN express op matched to on-chain by hash WHEN merge THEN enriched single row and on-chain not duplicated`() {
            // Arrange
            val onChain = listOf(createTxInfo(txHash = "h1", timestamp = 100))
            val express = listOf(createSwap(matchHash = "h1", status = ExpressExchangeStatus.Waiting))

            // Act
            val result = merge(onChain, express)

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
            val result = merge(onChain = emptyList(), express = express)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo).isNull()
        }

        @Test
        fun `GIVEN unmatched terminal outgoing swap WHEN merge THEN standalone row kept`() {
            // Arrange: the outgoing (pay-in) side is always kept — see UnmatchedTerminalHiding for the hidden case.
            val express = listOf(
                createSwap(matchHash = "missing", status = ExpressExchangeStatus.Finished, isOutgoing = true),
            )

            // Act
            val result = merge(onChain = emptyList(), express = express)

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
            val result = merge(onChain, express = emptyList())

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result.single()).isInstanceOf(OnChainTx.BSDK::class.java)
        }

        @Test
        fun `GIVEN rows of different timestamps WHEN merge THEN sorted by timestamp descending`() {
            // Arrange
            val onChain = listOf(createTxInfo(txHash = "h1", timestamp = 100))
            val express = listOf(
                createSwap(matchHash = "missing", createdAtMillis = 200, status = ExpressExchangeStatus.Waiting),
            )

            // Act
            val result = merge(onChain, express)

            // Assert
            assertThat(result.map { it.timestampMillis }).containsExactly(200L, 100L).inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HeuristicFallbackMatch {

        @Test
        fun `GIVEN incoming swap without payout hash WHEN on-chain credit matches address amount time THEN one enriched row`() {
            // Arrange: the provider never returned the payout hash, so only the heuristic can join the pair.
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 200,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.001"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Finished,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    fromAddress = "mySource",
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            val row = result.single()
            assertThat(row).isInstanceOf(ExpressTx.Swap::class.java)
            assertThat((row as ExpressTx).txInfo?.explorerHash).isEqualTo("credit")
        }

        @Test
        fun `GIVEN incoming swap WHEN on-chain sender is the user THEN self-transfer not matched and both rows kept`() {
            // Arrange: active deal so the unmatched express row stays visible (terminal hiding is covered elsewhere).
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 200,
                    isOutgoing = false,
                    sourceAddress = "mySource",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.001"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Waiting,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    fromAddress = "mySource",
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(2)
        }

        @Test
        fun `GIVEN incoming swap WHEN on-chain tx is outside the 24h window THEN not matched`() {
            // Arrange: active deal so the unmatched express row stays visible (terminal hiding is covered elsewhere).
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 100 + DAY_MILLIS + 1,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.001"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Waiting,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(2)
        }

        @Test
        fun `GIVEN incoming swap with expected amount only WHEN on-chain amount is beyond slippage THEN not matched`() {
            // Arrange: active deal so the unmatched express row stays visible (terminal hiding is covered elsewhere).
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 200,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.01"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Waiting,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(2)
        }

        @Test
        fun `GIVEN outgoing swap without payin hash WHEN on-chain pay-in matches deposit address and amount THEN matched`() {
            // Arrange
            val onChain = listOf(
                createTxInfo(
                    txHash = "payin",
                    timestamp = 120,
                    isOutgoing = true,
                    sourceAddress = "mySource",
                    destinationAddress = "providerDeposit",
                    amount = BigDecimal("1.5"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Confirming,
                    isOutgoing = true,
                    createdAtMillis = 100,
                    fromAddress = "mySource",
                    payinAddress = "providerDeposit",
                    fromAmount = BigDecimal("1.5"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = FROM_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo?.explorerHash).isEqualTo("payin")
        }

        @Test
        fun `GIVEN refunded swap WHEN incoming refund is within window and amount tolerance THEN matched`() {
            // Arrange: refund returns the from-asset to the user on the from-token screen.
            val onChain = listOf(
                createTxInfo(
                    txHash = "refund",
                    timestamp = 2000,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "mySource",
                    amount = BigDecimal("1.4"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Refunded,
                    isOutgoing = true,
                    createdAtMillis = 100,
                    updatedAtMillis = 1000,
                    fromAddress = "mySource",
                    fromAmount = BigDecimal("1.5"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = FROM_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo?.explorerHash).isEqualTo("refund")
        }

        @Test
        fun `GIVEN onramp without payout hash WHEN on-chain credit matches address amount time THEN matched`() {
            // Arrange
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 200,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.001"),
                ),
            )
            val express = listOf(
                createOnramp(
                    matchHash = null,
                    status = ExpressOnrampStatus.Finished,
                    createdAtMillis = 100,
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo?.explorerHash).isEqualTo("credit")
        }

        @Test
        fun `GIVEN two on-chain candidates WHEN incoming swap matches both THEN the closest in time is claimed and the other passes through`() {
            // Arrange
            val near = createTxInfo(
                txHash = "near",
                timestamp = 150,
                isOutgoing = false,
                sourceAddress = "provider",
                destinationAddress = "myPayout",
                amount = BigDecimal("0.001"),
            )
            val far = createTxInfo(
                txHash = "far",
                timestamp = 500,
                isOutgoing = false,
                sourceAddress = "provider",
                destinationAddress = "myPayout",
                amount = BigDecimal("0.001"),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Finished,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                ),
            )

            // Act
            val result = merge(onChain = listOf(near, far), express = express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(2)
            val enriched = result.filterIsInstance<ExpressTx>().single()
            assertThat(enriched.txInfo?.explorerHash).isEqualTo("near")
            val passthrough = result.filterIsInstance<OnChainTx.BSDK>().single()
            assertThat(passthrough.txInfo.txHash).isEqualTo("far")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CurrencyAndAmountScoping {

        @Test
        fun `GIVEN incoming swap WHEN open currency is not the to-asset THEN not matched`() {
            // Arrange: address/amount/time all line up, but the open currency is the swap's from-asset, not the to-asset.
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 200,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.001"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Waiting,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = FROM_CURRENCY)

            // Assert
            assertThat(result).hasSize(2)
        }

        @Test
        fun `GIVEN incoming swap with actual amount WHEN on-chain equals actual THEN matched`() {
            // Arrange: the settled amount differs from the expected one; the actual value carries the match.
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 200,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.00095"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Finished,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                    toActualAmount = BigDecimal("0.00095"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo?.explorerHash).isEqualTo("credit")
        }

        @Test
        fun `GIVEN incoming swap with actual amount WHEN on-chain differs from actual THEN not matched despite expected slippage`() {
            // Arrange: on-chain 0.001 is within 5% of the expected 0.001, but the actual is 0.0009 and matched exactly.
            val onChain = listOf(
                createTxInfo(
                    txHash = "credit",
                    timestamp = 200,
                    isOutgoing = false,
                    sourceAddress = "provider",
                    destinationAddress = "myPayout",
                    amount = BigDecimal("0.001"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Waiting,
                    isOutgoing = false,
                    createdAtMillis = 100,
                    payoutAddress = "myPayout",
                    toAmount = BigDecimal("0.001"),
                    toActualAmount = BigDecimal("0.0009"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(2)
        }

        @Test
        fun `GIVEN outgoing pay-in on UTXO chain WHEN amount is within dust tolerance THEN matched`() {
            // Arrange: 1.5008 vs 1.5 is ~0.05% off — within the 0.1% UTXO fee/dust tolerance.
            val onChain = listOf(
                createTxInfo(
                    txHash = "payin",
                    timestamp = 120,
                    isOutgoing = true,
                    sourceAddress = "mySource",
                    destinationAddress = "providerDeposit",
                    amount = BigDecimal("1.5008"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Confirming,
                    isOutgoing = true,
                    createdAtMillis = 100,
                    fromAddress = "mySource",
                    payinAddress = "providerDeposit",
                    fromAmount = BigDecimal("1.5"),
                    fromCurrency = UTXO_CURRENCY,
                ),
            )

            // Act
            val result = merge(onChain, express, currency = UTXO_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo?.explorerHash).isEqualTo("payin")
        }

        @Test
        fun `GIVEN outgoing pay-in on non-UTXO chain WHEN amount is not exact THEN not matched`() {
            // Arrange: same ~0.05% offset, but off UTXO the pay-in amount must match exactly.
            val onChain = listOf(
                createTxInfo(
                    txHash = "payin",
                    timestamp = 120,
                    isOutgoing = true,
                    sourceAddress = "mySource",
                    destinationAddress = "providerDeposit",
                    amount = BigDecimal("1.5008"),
                ),
            )
            val express = listOf(
                createSwap(
                    matchHash = null,
                    status = ExpressExchangeStatus.Confirming,
                    isOutgoing = true,
                    createdAtMillis = 100,
                    fromAddress = "mySource",
                    payinAddress = "providerDeposit",
                    fromAmount = BigDecimal("1.5"),
                ),
            )

            // Act
            val result = merge(onChain, express, currency = FROM_CURRENCY)

            // Assert
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnmatchedTerminalHiding {

        @Test
        fun `GIVEN unmatched terminal unsuccessful incoming swap WHEN merge THEN hidden`() {
            // Arrange: to-token screen (isOutgoing = false), deal ended (expired) without ever landing on-chain.
            val express = listOf(
                createSwap(matchHash = "missing", status = ExpressExchangeStatus.Expired, isOutgoing = false),
            )

            // Act
            val result = merge(onChain = emptyList(), express = express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `GIVEN unmatched finished incoming swap WHEN merge THEN standalone row kept`() {
            // Arrange: a successful incoming swap is always shown, even when its on-chain credit could not be matched
            // (e.g. the index-table backbone has no on-chain row to fall back to).
            val express = listOf(
                createSwap(matchHash = "missing", status = ExpressExchangeStatus.Finished, isOutgoing = false),
            )

            // Act
            val result = merge(onChain = emptyList(), express = express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo).isNull()
        }

        @Test
        fun `GIVEN unmatched active incoming swap WHEN merge THEN standalone row kept`() {
            // Arrange: not terminal yet, so the live row must still be shown.
            val express = listOf(
                createSwap(matchHash = "missing", status = ExpressExchangeStatus.Waiting, isOutgoing = false),
            )

            // Act
            val result = merge(onChain = emptyList(), express = express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo).isNull()
        }

        @Test
        fun `GIVEN unmatched terminal refunded swap on from-token WHEN merge THEN standalone row kept`() {
            // Arrange: refund lands on the open from-token (isOutgoing = true), so it is never hidden.
            val express = listOf(
                createSwap(matchHash = "missing", status = ExpressExchangeStatus.Refunded, isOutgoing = true),
            )

            // Act
            val result = merge(onChain = emptyList(), express = express, currency = FROM_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo).isNull()
        }

        @Test
        fun `GIVEN unmatched terminal onramp WHEN merge THEN standalone row kept`() {
            // Arrange: an onramp purchase is always kept, even terminal without an on-chain leg.
            val express = listOf(createOnramp(matchHash = "missing", status = ExpressOnrampStatus.Finished))

            // Act
            val result = merge(onChain = emptyList(), express = express, currency = TO_CURRENCY)

            // Assert
            assertThat(result).hasSize(1)
            assertThat((result.single() as ExpressTx).txInfo).isNull()
        }
    }

    private fun merge(
        onChain: List<TxInfo>,
        express: List<ExpressTx>,
        currency: CryptoCurrency = FROM_CURRENCY,
    ): List<TxHistoryInfo> = mergeTxHistoryInfos(onChain = onChain, express = express, currency = currency)

    private fun createTxInfo(
        txHash: String,
        timestamp: Long,
        isOutgoing: Boolean = true,
        sourceAddress: String = "addr",
        destinationAddress: String = "addr",
        amount: BigDecimal = BigDecimal.ONE,
    ) = TxInfo(
        txHash = txHash,
        timestampInMillis = timestamp,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User(destinationAddress)),
        sourceType = TxInfo.SourceType.Single(sourceAddress),
        interactionAddressType = null,
        status = TxInfo.TransactionStatus.Confirmed,
        type = TxInfo.TransactionType.Transfer,
        amount = amount,
    )

    @Suppress("LongParameterList")
    private fun createSwap(
        matchHash: String?,
        status: ExpressExchangeStatus,
        createdAtMillis: Long = 100,
        isOutgoing: Boolean = true,
        updatedAtMillis: Long = createdAtMillis,
        fromAddress: String = "fromAddr",
        payoutAddress: String = "payoutAddr",
        payinAddress: String = "payinAddr",
        fromAmount: BigDecimal? = BigDecimal("1.5"),
        toAmount: BigDecimal? = BigDecimal("0.001"),
        toActualAmount: BigDecimal? = null,
        fromCurrency: CryptoCurrency = FROM_CURRENCY,
        toCurrency: CryptoCurrency = TO_CURRENCY,
    ) = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "tx-1",
            status = status,
            createdAtMillis = createdAtMillis,
            provider = null,
            payinHash = matchHash.takeIf { isOutgoing },
            payoutHash = matchHash.takeUnless { isOutgoing },
            fromAddress = fromAddress,
            payoutAddress = payoutAddress,
            fromAsset = ExpressTransactionAsset(
                id = ExpressAssetId(fromCurrency),
                amount = fromAmount,
                decimals = 18,
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(toCurrency),
                amount = toAmount,
                decimals = 8,
            ),
            externalTxUrl = null,
            externalTxId = null,
            payinAddress = payinAddress,
            updatedAtMillis = updatedAtMillis,
            refundAssetId = null,
            refundCurrency = null,
            fromAmount = fromAmount ?: BigDecimal.ZERO,
            toAmount = toAmount ?: BigDecimal.ZERO,
            toActualAmount = toActualAmount,
        ),
        isOutgoing = isOutgoing,
        txInfo = null,
    )

    private fun createOnramp(
        matchHash: String?,
        status: ExpressOnrampStatus,
        createdAtMillis: Long = 100,
        payoutAddress: String = "payoutAddr",
        toAmount: BigDecimal? = BigDecimal("0.001"),
        toActualAmount: BigDecimal? = null,
        toCurrency: CryptoCurrency = TO_CURRENCY,
    ) = ExpressTx.Onramp(
        tx = OnrampTransaction(
            txId = "onramp-1",
            status = status,
            createdAtMillis = createdAtMillis,
            provider = null,
            payoutHash = matchHash,
            payoutAddress = payoutAddress,
            fromFiat = Amount(
                currencySymbol = "USD",
                value = BigDecimal.TEN,
                decimals = 2,
                type = AmountType.FiatType(code = "USD"),
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(toCurrency),
                amount = toAmount,
                decimals = 8,
            ),
            externalTxUrl = null,
            country = null,
            toAmount = toAmount,
            toActualAmount = toActualAmount,
        ),
        txInfo = null,
    )

    private companion object {
        const val DAY_MILLIS = 86_400_000L

        private val currencyFactory = MockCryptoCurrencyFactory()

        /** Open currency = the swap's `from` / pay-in asset (non-UTXO chain). */
        val FROM_CURRENCY: CryptoCurrency = currencyFactory.ethereum

        /** Open currency = the swap's `to` / payout (and onramp) asset. */
        val TO_CURRENCY: CryptoCurrency = currencyFactory.stellar

        /** UTXO chain, used to exercise the pay-in dust tolerance. */
        val UTXO_CURRENCY: CryptoCurrency = currencyFactory.bitcoin
    }
}