package com.tangem.features.txhistory.converter.fields

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryInfoFieldConverterRowTest {

    private val onRowClick: (TxHistoryInfo) -> Unit = mockk(relaxed = true)
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val converter = TxHistoryInfoFieldConverter(
        currency = currency,
        onRowClick = onRowClick,
        lookup = TxHistoryLookupContext(
            ownAccountByNetwork = emptyMap(),
            isAccountsModeEnabled = false,
            walletInfoById = emptyMap(),
        ),
    )

    @Test
    fun `GIVEN joint operation matched to its chain tx WHEN row clicked THEN routes the joint operation`() {
        // Arrange
        val item = jointOperation(status = FinishedJointOperation.Status.EXECUTED)
            .copy(txInfo = OnChainTx.BSDK(txInfo(type = TransactionType.Transfer)))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content
        result.onClick()

        // Assert
        assertThat(result.txHash).isEqualTo(TX_HASH)
        verify(exactly = 1) { onRowClick(item) }
    }

    @Test
    fun `GIVEN failed joint operation carrying its chain tx WHEN convert THEN rendered from the operation`() {
        // Arrange
        val item = jointOperation(status = FinishedJointOperation.Status.FAILED)
            .copy(txInfo = OnChainTx.BSDK(txInfo(type = TransactionType.Transfer)))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content

        // Assert
        assertThat(listOf(result.status, result.title, result.amount)).containsExactly(
            Status.Failed,
            resourceReference(R.string.transaction_history_status_send_failed),
            "1.00",
        ).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN standalone joint operation WHEN convert THEN status title and amount follow how it ended`(
        model: JointRowModel,
    ) {
        // Act
        val result = converter.convert(jointOperation(status = model.status)) as TransactionItemUM.Content

        // Assert
        assertThat(listOf(result.status, result.title, result.amount))
            .containsExactly(model.expectedStatus, resourceReference(model.expectedTitle), model.expectedAmount)
            .inOrder()
    }

    internal data class JointRowModel(
        val status: FinishedJointOperation.Status,
        val expectedStatus: Status,
        val expectedTitle: Int,
        val expectedAmount: String,
    )

    private fun provideTestModels() = listOf(
        JointRowModel(FinishedJointOperation.Status.EXECUTED, Status.Confirmed, R.string.common_sent, "${StringsSigns.MINUS_SIGN}1.00"),
        JointRowModel(
            FinishedJointOperation.Status.FAILED,
            Status.Failed,
            R.string.transaction_history_status_send_failed,
            "1.00",
        ),
        JointRowModel(
            FinishedJointOperation.Status.CANCELLED,
            Status.Failed,
            R.string.express_exchange_status_canceled,
            "1.00",
        ),
    )

    private fun jointOperation(status: FinishedJointOperation.Status) = JointOperationTx(
        operation = FinishedJointOperation(
            operationId = "operation-1",
            chainId = 1,
            safeAddress = "0xsafe",
            safeTx = SafeTx(
                to = USER_ADDRESS,
                value = BigInteger.TEN.pow(18),
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = BigInteger.ONE,
                baseGas = BigInteger.ONE,
                gasPrice = BigInteger.ONE,
                gasToken = "0x0",
                refundReceiver = "0xrelayer",
                nonce = BigInteger.ONE,
            ),
            status = status,
            initiator = USER_ADDRESS,
            signatures = emptyList(),
            createdAt = "2026-09-08T09:20:37.000Z",
            finishedAtMillis = TIMESTAMP,
            txHash = TX_HASH.takeIf { status == FinishedJointOperation.Status.EXECUTED },
        ),
        transfer = JointOperationTransfer(
            currency = currency,
            intent = JointTransferIntent.Native(recipient = USER_ADDRESS, amountWei = BigInteger.TEN.pow(18)),
        ),
        txInfo = null,
    )

    @Test
    fun `GIVEN on-chain content row WHEN row clicked THEN routes the incoming OnChainTx through onTransactionClick`() {
        // Arrange
        val item = OnChainTx.BSDK(txInfo(type = TransactionType.Transfer))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content
        result.onClick()

        // Assert
        verify(exactly = 1) { onRowClick(item) }
    }

    @Test
    fun `GIVEN on-chain swap row WHEN row clicked THEN routes the incoming OnChainTx through onTransactionClick`() {
        // Arrange
        val item = OnChainTx.BSDK(txInfo(type = TransactionType.Swap))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content
        result.onClick()

        // Assert
        verify(exactly = 1) { onRowClick(item) }
    }

    @Test
    fun `GIVEN on-chain pill row WHEN row clicked THEN routes the incoming OnChainTx through onTransactionClick`() {
        // Arrange
        val item = OnChainTx.BSDK(txInfo(type = TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = USER_ADDRESS)))

        // Act
        val result = converter.convert(item) as TransactionItemUM.Pill
        result.onClick()

        // Assert
        verify(exactly = 1) { onRowClick(item) }
    }

    @Test
    fun `GIVEN express row WHEN row clicked THEN routes the incoming ExpressTx through onTransactionClick`() {
        // Arrange
        val item = expressSwap()

        // Act
        val result = converter.convert(item) as TransactionItemUM.Content
        result.onClick()

        // Assert
        verify(exactly = 1) { onRowClick(item) }
    }

    private fun txInfo(type: TransactionType): TxInfo.Single = TxInfo.Single(
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
            fromAddress = "from-addr",
            payoutAddress = "payout-addr",
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
            externalTxUrl = null,
            externalTxId = null,
            payinAddress = "payin-addr",
            updatedAtMillis = TIMESTAMP,
            refundAssetId = null,
            refundCurrency = null,
            fromAmount = BigDecimal("1.5"),
            toAmount = BigDecimal("0.001"),
            toActualAmount = null,
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