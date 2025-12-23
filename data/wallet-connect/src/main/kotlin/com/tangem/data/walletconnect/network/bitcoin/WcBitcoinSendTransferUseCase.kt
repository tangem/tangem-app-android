package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.left
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
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
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
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
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcTransactionUseCase {

    override val wallet get() = context.session.wallet

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
        // Update wallet manager to refresh UTXO data before processing Bitcoin transaction
        walletManagersFacade.update(
            userWalletId = wallet.walletId,
            network = network,
            extraTokens = emptySet(),
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
            ?: run {
                emit(state.toResult(HandleMethodError.UnknownError("Failed to create wallet manager").left()))
                return
            }

        val signer = signerProvider.createSigner(wallet)
        val amountInBtc = BigDecimal(method.amount).divide(BigDecimal("100000000"))
        val amount = Amount(
            currencySymbol = network.currencySymbol,
            value = amountInBtc,
            decimals = 8, // Bitcoin has 8 decimal places
        )
        val feeResult = walletManager.getFee(
            amount = amount,
            destination = method.recipientAddress,
        )

        val fee = when (feeResult) {
            is SdkResult.Success -> feeResult.data.normal
            is SdkResult.Failure -> {
                emit(state.toResult(HandleMethodError.UnknownError(feeResult.error.customMessage).left()))
                return
            }
        }
        val transactionData = TransactionData.Uncompiled(
            amount = amount,
            fee = fee,
            sourceAddress = context.accountAddress,
            destinationAddress = method.recipientAddress,
            extras = BitcoinTransactionExtras(
                memo = method.memo,
                changeAddress = method.changeAddress,
            ),
        )
        when (val result = walletManager.send(transactionData, signer)) {
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

    override fun invoke(): Flow<WcSignState<TransactionData>> {
        val displayInfo = "Send ${method.amount} satoshis to ${method.recipientAddress}"
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.RawString(displayInfo),
        )
        return delegate.invoke(transactionData)
    }

    private fun buildJsonResponse(txid: String): String = "{\"txid\":\"$txid\"}"

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcBitcoinMethod.SendTransfer): WcBitcoinSendTransferUseCase
    }
}