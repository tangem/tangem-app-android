package com.tangem.features.txhistory.converter.fields

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
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
import com.tangem.features.txhistory.converter.ExpressTxToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.JointOperationTxToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryInfoToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Pins [TxHistoryInfoFieldConverter] — the per-cell mapping — to the row-shaped converters it replaces: every
 * on-chain transaction type, in both directions and all three statuses, must land on the same [TransactionItemUM].
 *
 * The lookup context is empty on purpose: the counterparty resolution it drives is shared code, so what is compared
 * here is the per-cell dispatch itself (shape, amount, icon, title, subtitle).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryInfoFieldConverterParityTest {

    private val uiActions: TxHistoryUiActions = mockk(relaxed = true)
    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Ethereum)
    private val token: CryptoCurrency.Token = currencyFactory.createToken(Blockchain.Ethereum, symbol = "USDT")

    private val lookup = TxHistoryLookupContext(
        ownAccountByNetwork = emptyMap(),
        isAccountsModeEnabled = false,
        walletInfoById = emptyMap(),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ParityModel) {
        // Arrange
        val row = model.row

        // Act
        val old = oldConverter(model.currency).convert(row)
        val new = fieldConverter(model.currency).convert(row)

        // Assert
        assertThat(new.withoutCallbacks()).isEqualTo(old.withoutCallbacks())
    }

    private fun provideTestModels(): List<ParityModel> = buildList {
        TRANSACTION_TYPES.forEach { type ->
            listOf(coin, token).forEach { currency ->
                listOf(true, false).forEach { isOutgoing ->
                    STATUSES.forEach { status ->
                        add(
                            onChain(
                                currency = currency,
                                txInfo = txInfo(type = type, status = status, isOutgoing = isOutgoing),
                            ),
                        )
                    }
                }
            }
        }
        // An undecoded outgoing call that moved no coin is a fee row, not an operation.
        add(
            onChain(
                currency = coin,
                txInfo = txInfo(
                    type = TransactionType.Operation(name = "stakeTokens"),
                    isOutgoing = true,
                    amount = BigDecimal.ZERO,
                ),
            ),
        )
        add(
            onChain(
                currency = coin,
                txInfo = txInfo(
                    type = TransactionType.Operation(name = "", recognized = TransactionType.YieldSupply.Topup),
                    isOutgoing = true,
                    amount = BigDecimal.ZERO,
                ),
            ),
        )
        // The counterparty wordings an address type drives.
        listOf(
            TxInfo.InteractionAddressType.User(address = COUNTERPARTY),
            TxInfo.InteractionAddressType.Contract(address = COUNTERPARTY),
            TxInfo.InteractionAddressType.Validator(address = COUNTERPARTY),
            TxInfo.InteractionAddressType.Multiple(addresses = listOf(COUNTERPARTY, OWN_ADDRESS)),
        ).forEach { addressType ->
            listOf(true, false).forEach { isOutgoing ->
                add(
                    onChain(
                        currency = coin,
                        txInfo = txInfo(
                            type = TransactionType.Transfer,
                            isOutgoing = isOutgoing,
                            interactionAddressType = addressType,
                        ),
                    ),
                )
            }
        }
        // An incoming undecoded call reads its sender from the source side.
        add(
            onChain(
                currency = token,
                txInfo = txInfo(
                    type = TransactionType.UnknownOperation,
                    isOutgoing = false,
                    sourceType = TxInfo.SourceType.Multiple(addresses = listOf(COUNTERPARTY, OWN_ADDRESS)),
                ),
            ),
        )
        EXCHANGE_STATUSES.forEach { status ->
            listOf(true, false).forEach { isOutgoing ->
                add(ParityModel(currency = coin, row = expressSwap(status = status, isOutgoing = isOutgoing)))
            }
        }
        FinishedJointOperation.Status.entries.forEach { status ->
            add(ParityModel(currency = coin, row = jointOperation(status = status)))
        }
    }

    private fun onChain(currency: CryptoCurrency, txInfo: TxInfo) =
        ParityModel(currency = currency, row = OnChainTx.BSDK(txInfo))

    internal data class ParityModel(val currency: CryptoCurrency, val row: TxHistoryInfo) {
        override fun toString(): String = when (row) {
            is OnChainTx.BSDK -> with(row.txInfo) {
                "${type::class.simpleName}, ${currency.symbol}, outgoing=$isOutgoing, $status"
            }
            else -> "${row::class.simpleName}, ${currency.symbol}, ${row.txId}"
        }
    }

    private fun expressSwap(status: ExpressExchangeStatus, isOutgoing: Boolean): ExpressTx.Swap = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "swap-$status-$isOutgoing",
            status = status,
            createdAtMillis = TIMESTAMP,
            provider = null,
            payinHash = null,
            payoutHash = null,
            fromAddress = OWN_ADDRESS,
            payoutAddress = COUNTERPARTY,
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
            payinAddress = COUNTERPARTY,
            updatedAtMillis = TIMESTAMP,
            refundAssetId = null,
            refundCurrency = null,
            fromAmount = BigDecimal("1.5"),
            toAmount = BigDecimal("0.001"),
            toActualAmount = null,
        ),
        isOutgoing = isOutgoing,
        txInfo = null,
    )

    private fun jointOperation(status: FinishedJointOperation.Status) = JointOperationTx(
        operation = FinishedJointOperation(
            operationId = "operation-1",
            chainId = 1,
            safeAddress = "0xsafe",
            safeTx = SafeTx(
                to = COUNTERPARTY,
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
            initiator = OWN_ADDRESS,
            signatures = emptyList(),
            createdAt = "2026-09-08T09:20:37.000Z",
            finishedAtMillis = TIMESTAMP,
            txHash = TX_HASH.takeIf { status == FinishedJointOperation.Status.EXECUTED },
        ),
        transfer = JointOperationTransfer(
            currency = coin,
            intent = JointTransferIntent.Native(recipient = COUNTERPARTY, amountWei = BigInteger.TEN.pow(18)),
        ),
        txInfo = null,
    )

    private fun oldConverter(currency: CryptoCurrency) = TxHistoryInfoToTransactionItemUMConverter(
        txInfoConverter = TxHistoryItemToTransactionItemUMConverter(
            currency = currency,
            txHistoryUiActions = uiActions,
            lookupContext = lookup,
            networkTokens = setOf(token),
        ),
        expressConverter = ExpressTxToTransactionItemUMConverter(
            currency = currency,
            txHistoryUiActions = uiActions,
            lookup = lookup,
        ),
        jointConverter = JointOperationTxToTransactionItemUMConverter(
            currency = currency,
            txHistoryUiActions = uiActions,
            lookup = lookup,
        ),
        txHistoryUiActions = uiActions,
    )

    private fun fieldConverter(currency: CryptoCurrency) = TxHistoryInfoFieldConverter(
        currency = currency,
        txHistoryUiActions = uiActions,
        lookup = lookup,
        networkTokens = setOf(token),
    )

    @Suppress("LongParameterList")
    private fun txInfo(
        type: TransactionType,
        status: TxInfo.TransactionStatus = TxInfo.TransactionStatus.Confirmed,
        isOutgoing: Boolean = false,
        amount: BigDecimal = BigDecimal.ONE,
        interactionAddressType: TxInfo.InteractionAddressType? = TxInfo.InteractionAddressType.User(COUNTERPARTY),
        sourceType: TxInfo.SourceType = TxInfo.SourceType.Single(address = COUNTERPARTY),
    ): TxInfo.Single = TxInfo.Single(
        txHash = TX_HASH,
        timestampInMillis = TIMESTAMP,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(OWN_ADDRESS)),
        sourceType = sourceType,
        interactionAddressType = interactionAddressType,
        status = status,
        type = type,
        amount = amount,
        fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18),
    )

    /** Click handlers are fresh lambdas on every conversion, so they are normalized away before comparing. */
    private fun TransactionItemUM.withoutCallbacks(): TransactionItemUM = when (this) {
        is TransactionItemUM.Content -> copy(onClick = NO_OP)
        is TransactionItemUM.Pill -> copy(onClick = NO_OP)
    }

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val OWN_ADDRESS = "0x1234567890abcdef1234"
        const val COUNTERPARTY = "0xExternalAddress5678"
        val NO_OP: () -> Unit = {}

        val EXCHANGE_STATUSES = listOf(
            ExpressExchangeStatus.Finished,
            ExpressExchangeStatus.Exchanging,
            ExpressExchangeStatus.Verifying,
            ExpressExchangeStatus.Refunded,
        )

        val STATUSES = listOf(
            TxInfo.TransactionStatus.Confirmed,
            TxInfo.TransactionStatus.Unconfirmed,
            TxInfo.TransactionStatus.Failed,
        )

        val TRANSACTION_TYPES = listOf(
            TransactionType.Transfer,
            TransactionType.Swap,
            TransactionType.UnknownOperation,
            TransactionType.Operation(name = "execute"),
            TransactionType.GaslessFee,
            TransactionType.IncentiveClaim,
            TransactionType.Approve(
                amount = SdkAmount(currencySymbol = "USDT", value = BigDecimal.TEN, decimals = 6),
                address = COUNTERPARTY,
            ),
            TransactionType.NetworkFee(
                amount = SdkAmount(currencySymbol = "USDT", value = BigDecimal.TEN, decimals = 6),
                address = COUNTERPARTY,
            ),
            TransactionType.Joint.Transfer(
                recipient = COUNTERPARTY,
                amount = BigDecimal.ONE,
                refundCap = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.01"), decimals = 18),
                signaturesCount = 2,
            ),
            TransactionType.Staking.Stake,
            TransactionType.Staking.Unstake,
            TransactionType.Staking.Restake,
            TransactionType.Staking.Withdraw,
            TransactionType.Staking.ClaimRewards,
            TransactionType.Staking.Vote(validatorAddress = COUNTERPARTY),
            TransactionType.YieldSupply.Enter(address = COUNTERPARTY),
            TransactionType.YieldSupply.Exit(address = COUNTERPARTY),
            TransactionType.YieldSupply.Topup,
            TransactionType.YieldSupply.Send(address = COUNTERPARTY, isYieldSupplyWithdraw = true),
            TransactionType.YieldSupply.Send(address = COUNTERPARTY, isYieldSupplyWithdraw = false),
            TransactionType.YieldSupply.DeployContract(address = COUNTERPARTY),
            TransactionType.YieldSupply.InitializeToken(address = COUNTERPARTY),
            TransactionType.YieldSupply.ReactivateToken(address = COUNTERPARTY),
        )
    }
}