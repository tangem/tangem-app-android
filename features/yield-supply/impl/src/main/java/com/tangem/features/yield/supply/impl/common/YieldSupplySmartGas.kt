package com.tangem.features.yield.supply.impl.common

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendEvmGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetAvailableFeeTokensUseCase
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import java.math.BigDecimal
import javax.inject.Inject

/** Sends a Yield Mode batch with the fee it was shown with: Smart Gas or the native coin. */
@ModelScoped
internal class YieldSupplySmartGas @Inject constructor(
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createAndSendEvmGaslessTransactionUseCase: CreateAndSendEvmGaslessTransactionUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getAvailableFeeTokensUseCase: GetAvailableFeeTokensUseCase,
) {

    /** A Smart Gas batch goes on-chain as one transaction, so it returns a single hash. */
    suspend fun sendTransactions(
        userWallet: UserWallet,
        network: Network,
        yieldSupplyFeeUM: YieldSupplyFeeUM.Content,
    ): Either<SendTransactionError, List<String>> = if (yieldSupplyFeeUM.gaslessFee != null) {
        createAndSendEvmGaslessTransactionUseCase(
            userWallet = userWallet,
            transactionDataList = yieldSupplyFeeUM.transactionDataList,
            fee = yieldSupplyFeeUM.gaslessFee,
        ).map { listOf(it) }
    } else {
        sendTransactionUseCase(
            txsData = yieldSupplyFeeUM.transactionDataList,
            userWallet = userWallet,
            network = network,
            sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
        )
    }

    /** Matches iOS: the fee currency opens for picking only when the list would offer more than one. */
    suspend fun hasFeeCurrencyChoice(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        enteringYieldTokenId: CryptoCurrency.ID? = null,
    ): Boolean {
        return getAvailableFeeTokensUseCase(
            userWallet = userWallet,
            network = cryptoCurrencyStatus.currency.network,
            sentCurrencyStatus = cryptoCurrencyStatus,
            enteringYieldTokenId = enteringYieldTokenId,
        ).fold(ifLeft = { false }, ifRight = { it.tokens.size > 1 })
    }

    suspend fun getFeeTokenStatus(
        userWalletId: UserWalletId,
        feeTokenId: CryptoCurrency.ID,
        network: Network,
    ): CryptoCurrencyStatus? {
        return singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            .getCryptoCurrencyStatus(currencyId = feeTokenId, network = network)
            .getOrNull()
    }
}

internal data class YieldSupplySmartGasFee(
    val fee: TransactionFeeExtended,
    val feeTokenStatus: CryptoCurrencyStatus,
    val feeAmount: BigDecimal,
    val transactions: List<TransactionData.Uncompiled>,
)

internal fun YieldSupplyFeeUM.Content.feeAssetType(feeCurrency: CryptoCurrency): AnalyticsParam.FeeAssetType {
    return if (gaslessFee == null && feeCurrency is CryptoCurrency.Coin) {
        AnalyticsParam.FeeAssetType.Coin
    } else {
        AnalyticsParam.FeeAssetType.Token
    }
}