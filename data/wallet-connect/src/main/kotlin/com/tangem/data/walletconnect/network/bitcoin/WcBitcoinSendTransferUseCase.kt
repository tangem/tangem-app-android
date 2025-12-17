package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.left
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionExtras
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result as SdkResult
import java.math.BigDecimal
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.datasource.di.SdkMoshi
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
    @SdkMoshi private val moshi: Moshi,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcTransactionUseCase {

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
        walletManagersFacade.update(
            userWalletId = wallet.walletId,
            network = network,
            extraTokens = emptySet(),
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
        if (walletManager !is BitcoinWalletManager) {
            emitError(state, "Invalid wallet manager type")
            return
        }

        val signer = signerProvider.createSigner(wallet)
        val amount = parseAmount() ?: run {
            emitError(state, "Invalid amount format: ${method.amount}")
            return
        }

        val fee = getFee(walletManager, amount, state) ?: return

        val transactionData = buildTransactionData(amount, fee)

        sendTransaction(walletManager, transactionData, signer, state)
    }

    private fun parseAmount(): Amount? {
        val amountInSatoshis = try {
            BigDecimal(method.amount)
        } catch (e: NumberFormatException) {
            return null
        }

        val amountInBtc = amountInSatoshis.movePointLeft(SATOSHI_DECIMALS)
        return Amount(
            currencySymbol = network.currencySymbol,
            value = amountInBtc,
            decimals = SATOSHI_DECIMALS,
            type = AmountType.Coin,
        )
    }

    private suspend fun SignCollector<TransactionData>.getFee(
        walletManager: BitcoinWalletManager,
        amount: Amount,
        state: WcSignState<TransactionData>,
    ): Fee? {
        return when (val feeResult = walletManager.getFee(amount, method.recipientAddress)) {
            is SdkResult.Success -> Fee.Common(feeResult.data.normal.amount)
            is SdkResult.Failure -> {
                emitError(state, feeResult.error.customMessage)
                null
            }
        }
    }

    private fun buildTransactionData(amount: Amount, fee: Fee): TransactionData.Uncompiled {
        val extras = BitcoinTransactionExtras(
            memo = method.memo,
            changeAddress = method.changeAddress,
        )

        return TransactionData.Uncompiled(
            amount = amount,
            fee = fee,
            sourceAddress = method.account,
            destinationAddress = method.recipientAddress,
            extras = extras,
        )
    }

    private suspend fun SignCollector<TransactionData>.sendTransaction(
        walletManager: BitcoinWalletManager,
        transactionData: TransactionData.Uncompiled,
        signer: com.tangem.blockchain.common.TransactionSigner,
        state: WcSignState<TransactionData>,
    ) {
        when (val sendResult = walletManager.send(transactionData, signer)) {
            is SdkResult.Success -> {
                val response = buildJsonResponse(
                    com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SendTransferResponse(
                        txid = sendResult.data.hash,
                    ),
                )
                val wcRespondResult = respondService.respond(rawSdkRequest, response)
                emit(state.toResult(wcRespondResult))
            }
            is SdkResult.Failure -> {
                emitError(state, sendResult.error.customMessage)
            }
        }
    }

    private suspend fun SignCollector<TransactionData>.emitError(
        state: WcSignState<TransactionData>,
        message: String,
    ) {
        emit(state.toResult(HandleMethodError.UnknownError(message).left()))
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> {
        val displayInfo = "Send ${method.amount} satoshis to ${method.recipientAddress}"
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.RawString(displayInfo),
        )
        return delegate.invoke(transactionData)
    }

    private fun buildJsonResponse(
        data: com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SendTransferResponse,
    ): String {
        return moshi.adapter(
            com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SendTransferResponse::class.java,
        ).toJson(data)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcBitcoinMethod.SendTransfer): WcBitcoinSendTransferUseCase
    }

    companion object {
        private const val SATOSHI_DECIMALS = 8
    }
}