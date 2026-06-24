package com.tangem.features.txhistory.converter

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryInfoToTransactionItemUMConverterTest {

    private val txHistoryUiActions: TxHistoryUiActions = mockk(relaxed = true)
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val converter = TxHistoryInfoToTransactionItemUMConverter(
        txInfoConverter = TxHistoryItemToTransactionItemUMConverter(
            currency = currency,
            txHistoryUiActions = txHistoryUiActions,
        ),
        expressConverter = ExpressTxToTransactionItemUMConverter(
            currency = currency,
            txHistoryUiActions = txHistoryUiActions,
        ),
        txHistoryUiActions = txHistoryUiActions,
    )

    @Test
    fun `GIVEN on-chain content row WHEN row clicked THEN routes the incoming OnChainTx through onTransactionClick`() {
        // Arrange
        val item = OnChainTx.BSDK(txInfo(type = TransactionType.Transfer))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content
        result.onClick()

        // Assert
        verify { txHistoryUiActions.onTransactionClick(item) }
    }

    @Test
    fun `GIVEN on-chain swap row WHEN row clicked THEN routes the incoming OnChainTx through onTransactionClick`() {
        // Arrange
        val item = OnChainTx.BSDK(txInfo(type = TransactionType.Swap))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content
        result.onClick()

        // Assert
        verify { txHistoryUiActions.onTransactionClick(item) }
    }

    @Test
    fun `GIVEN on-chain pill row WHEN row clicked THEN stays on the explorer`() {
        // Arrange
        val item = OnChainTx.BSDK(txInfo(type = TransactionType.Approve))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Pill
        result.onClick()

        // Assert
        verify { txHistoryUiActions.openTxInExplorer(TX_HASH) }
    }

    @Test
    fun `GIVEN express row WHEN row clicked THEN routes the incoming ExpressTx through onTransactionClick`() {
        // Arrange
        val item = expressSwap()

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content
        result.onClick()

        // Assert
        verify { txHistoryUiActions.onTransactionClick(item) }
    }

    private fun txInfo(type: TransactionType): TxInfo = TxInfo(
        txHash = TX_HASH,
        timestampInMillis = TIMESTAMP,
        isOutgoing = false,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
        sourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
        interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        status = TxInfo.TransactionStatus.Confirmed,
        type = type,
        amount = BigDecimal.ONE,
    )

    private fun expressSwap(): ExpressTx.Swap = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "swap-1",
            status = ExpressExchangeStatus.Exchanging,
            createdAtMillis = TIMESTAMP,
            provider = null,
            payinHash = null,
            payoutHash = null,
            fromAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "ethereum", contractAddress = "0"),
                amount = BigDecimal("1.5"),
                decimals = 18,
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "bitcoin", contractAddress = "0"),
                amount = BigDecimal("0.001"),
                decimals = 8,
            ),
        ),
        isOutgoing = true,
        txInfo = null,
    )

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val USER_ADDRESS = "0x1234567890abcdef1234"
    }
}