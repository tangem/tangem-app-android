package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.SendLargeSolanaTransactionUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletconnect.WcAnalyticEvents.SolanaLargeTransactionStatus
import com.tangem.domain.walletconnect.error.parseSendError
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.SignRequirements
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import com.tangem.lib.crypto.BlockchainUtils.SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("LongParameterList")
internal class WcSolanaSignAndSendTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val sendTransaction: SendTransactionUseCase,
    private val sendLargeSolanaTransactionUseCase: SendLargeSolanaTransactionUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcSolanaMethod.SignAndSendTransaction,
    blockAidDelegate: BlockAidVerificationDelegate,
    addressConverter: SolanaBlockAidAddressConverter,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcTransactionUseCase,
    SignRequirements {

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = addressConverter.convert(context.accountAddress),
    ).map { lce -> lce.map { result -> BlockAidTransactionCheck.Result.Plain(result) } }

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        val hash = state.signModel.getTxHashFromCompiled()
        val formattedHash = getFormattedHash(hash) // uses for flow sendLargeSolanaTransaction
        if (context.session.wallet is UserWallet.Cold && isLargeHash(formattedHash)) {
            // workaround for large transactions that cannot be signed directly by card
            TangemLogger.i("The transaction hash is too large to be signed directly: ${formattedHash.size} bytes")
            sendLargeSolanaTransactionUseCase(context.session.wallet as UserWallet.Cold, context.network, formattedHash)
                .fold(
                    ifLeft = { error ->
                        analytics.send(SolanaLargeTransactionStatus(SolanaLargeTransactionStatus.Status.Failed))
                        TangemLogger.e(error.toString())
                        emit(state.toResult(parseSendError(error).left()))
                    },
                    ifRight = {
                        analytics.send(SolanaLargeTransactionStatus(SolanaLargeTransactionStatus.Status.Success))
                        val emptyRespond = ByteArray(0).formatAsSolanaSignature()
                        val respondResult = respondService.respond(rawSdkRequest, emptyRespond)
                        emit(state.toResult(respondResult))
                    },
                )
        } else {
            val signedHash =
                sendTransaction.invoke(txData = state.signModel, userWallet = wallet, network = network)
                    .onLeft { error ->
                        emit(state.toResult(parseSendError(error).left()))
                    }
                    .getOrNull()
                    ?: return
            val respond = signedHash.formatAsSolanaSignature()
            val respondResult = respondService.respond(rawSdkRequest, respond)
            emit(state.toResult(respondResult))
        }
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> {
        val data = method.transaction.decodeBase58() ?: byteArrayOf()

        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(data),
        )
        return delegate.invoke(transactionData)
    }

    private fun String.formatAsSolanaSignature(): String {
        return "{ signature: \"${this}\" }"
    }

    private fun ByteArray.formatAsSolanaSignature(): String {
        return "{ signature: \"${this.encodeBase58()}\" }"
    }

    private fun TransactionData.getTxHashFromCompiled(): ByteArray {
        return when (this) {
            is TransactionData.Compiled -> (value as? TransactionData.Compiled.Data.Bytes)?.data
                ?: error("Invalid transaction data")
            is TransactionData.Uncompiled -> error("Transaction must be compiled")
        }
    }

    private fun isLargeHash(hash: ByteArray): Boolean {
        return hash.size > SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
    }

    private fun getFormattedHash(hash: ByteArray): ByteArray {
        return try {
            SolanaTransactionHelper.removeSignaturesPlaceholders(hash)
        } catch (e: Exception) {
            TangemLogger.e("Failed to format the hash: ${e.message}")
            hash
        }
    }

    override fun isMultipleSignRequired(): Boolean {
        val data = method.transaction.decodeBase58() ?: byteArrayOf()
        return isLargeHash(data)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcSolanaMethod.SignAndSendTransaction,
        ): WcSolanaSignAndSendTransactionUseCase
    }
}