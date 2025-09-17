package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.PrepareAndSignUseCase
import com.tangem.domain.transaction.usecase.SendLargeSolanaTransactionUseCase
import com.tangem.domain.walletconnect.error.parseSendError
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.SignRequirements
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.ByteString.Companion.decodeBase64
import timber.log.Timber

@Suppress("LongParameterList")
internal class WcSolanaSignTransactionUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    private val prepareAndSign: PrepareAndSignUseCase,
    private val sendLargeSolanaTransactionUseCase: SendLargeSolanaTransactionUseCase,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcSolanaMethod.SignTransaction,
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
            Timber.w("The transaction hash is too large to be signed directly: ${formattedHash.size} bytes")
            sendLargeSolanaTransactionUseCase(context.session.wallet as UserWallet.Cold, context.network, formattedHash)
                .fold(
                    ifLeft = {
                        Timber.e(it.toString())
                        emit(state.toResult(parseSendError(it).left()))
                    },
                    ifRight = {
                        val emptyRespond = ByteArray(0).formatAsSolanaSignature()
                        val respondResult = respondService.respond(rawSdkRequest, emptyRespond)
                        emit(state.toResult(respondResult))
                    },
                )
        } else {
            val signedHash =
                prepareAndSign.invoke(transactionData = state.signModel, userWallet = wallet, network = network)
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
        val data = method.transaction.decodeBase64()?.toByteArray() ?: ByteArray(0)

        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(data),
        )
        return delegate.invoke(transactionData)
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
        return hash.size > LARGE_HASH_SIZE
    }

    private fun getFormattedHash(hash: ByteArray): ByteArray {
        return try {
            SolanaTransactionHelper.removeSignaturesPlaceholders(hash)
        } catch (e: Exception) {
            Timber.e("Failed to format the hash: ${e.message}")
            hash
        }
    }

    override fun isMultipleSignRequired(): Boolean {
        val data = method.transaction.decodeBase64()?.toByteArray() ?: ByteArray(0)
        return isLargeHash(data)
    }

    private companion object {
        private const val LARGE_HASH_SIZE = 930 // bytes
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcSolanaMethod.SignTransaction,
        ): WcSolanaSignTransactionUseCase
    }
}