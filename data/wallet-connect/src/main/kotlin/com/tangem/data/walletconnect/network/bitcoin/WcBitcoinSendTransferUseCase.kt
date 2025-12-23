package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.left
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result as SdkResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.walletconnect.WcTransactionSignerProvider
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcMutableFee
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/**
 * Use case for Bitcoin sendTransfer WalletConnect method.
 *
 * Sends a Bitcoin transfer transaction with optional memo (OP_RETURN) and custom change address.
 */
@Suppress("LongParameterList")
internal class WcBitcoinSendTransferUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcBitcoinMethod.SendTransfer,
    private val walletManagersFacade: WalletManagersFacade,
    private val signerProvider: WcTransactionSignerProvider,
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<WcBitcoinTxAction, TransactionData>(),
    WcTransactionUseCase,
    WcMutableFee {

    override val wallet get() = context.session.wallet

    private var dAppFee: Fee? = null

    private val transferAmount: Amount by lazy {
        createAmountFromSatoshis(method.amount)
    }

    override val securityStatus: LceFlow<Throwable, BlockAidTransactionCheck.Result> =
        blockAidDelegate.getSecurityStatus(
            network = network,
            method = method,
            rawSdkRequest = rawSdkRequest,
            session = session,
            accountAddress = context.accountAddress,
        ).map { lce ->
            lce.map { result -> BlockAidTransactionCheck.Result.Plain(result) }
        }

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
            ?: run {
                emit(state.toResult(HandleMethodError.UnknownError("Failed to create wallet manager").left()))
                return
            }

        val signer = signerProvider.createSigner(wallet)
        when (val result = walletManager.send(state.signModel, signer)) {
            is SdkResult.Success -> {
                val response = buildJsonResponse(result.data.hash)
                val wcRespondResult = respondService.respond(rawSdkRequest, response)
                emit(state.toResult(wcRespondResult))
            }
            is SdkResult.Failure -> {
                emit(state.toResult(HandleMethodError.UnknownError(result.error.customMessage).left()))
            }
        }
    }

    override suspend fun FlowCollector<TransactionData>.onMiddleAction(
        signModel: TransactionData,
        action: WcBitcoinTxAction,
    ) {
        val uncompiled = signModel as? TransactionData.Uncompiled ?: return
        val newState = when (action) {
            is WcBitcoinTxAction.UpdateFee -> uncompiled.copy(fee = action.fee)
        }
        emit(newState)
    }

    override suspend fun dAppFee(): Fee? {
        if (dAppFee != null) return dAppFee

        // Update wallet manager to refresh UTXO data
        walletManagersFacade.update(
            userWalletId = wallet.walletId,
            network = network,
            extraTokens = emptySet(),
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
            ?: return null

        val feeResult = walletManager.getFee(
            amount = transferAmount,
            destination = method.recipientAddress,
        )

        return when (feeResult) {
            is SdkResult.Success -> feeResult.data.normal.also { dAppFee = it }
            is SdkResult.Failure -> null
        }
    }

    override fun updateFee(fee: Fee) {
        middleAction(WcBitcoinTxAction.UpdateFee(fee))
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> = flow {
        val fee = dAppFee()
        val transactionData = createTransactionData(fee)
        emitAll(delegate.invoke(transactionData))
    }

    private fun createTransactionData(fee: Fee?): TransactionData.Uncompiled {
        return TransactionData.Uncompiled(
            amount = transferAmount,
            fee = fee,
            sourceAddress = context.accountAddress,
            destinationAddress = method.recipientAddress,
            extras = BitcoinTransactionExtras(
                memo = method.memo,
                changeAddress = method.changeAddress,
            ),
        )
    }

    private fun createAmountFromSatoshis(satoshis: String): Amount {
        val btcValue = BigDecimal(satoshis).divide(SATOSHI_IN_BTC)
        return Amount(
            currencySymbol = network.currencySymbol,
            value = btcValue,
            decimals = BITCOIN_DECIMALS,
        )
    }

    private fun buildJsonResponse(txid: String): String = "{\"txid\":\"$txid\"}"

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcBitcoinMethod.SendTransfer): WcBitcoinSendTransferUseCase
    }

    private companion object {
        val SATOSHI_IN_BTC = BigDecimal("100000000")
        const val BITCOIN_DECIMALS = 8
    }
}