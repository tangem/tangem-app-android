package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.error.parseTangemSdkError
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val TransactionSignAttemptException = IllegalStateException(
    "solana_signMessage payload is a serialized Solana transaction; signing rejected ([REDACTED_TASK_KEY])",
)

internal class WcSolanaMessageSignUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcSolanaMethod.SignMessage,
    private val signUseCase: SignUseCase,
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
) : BaseWcSignUseCase<Nothing, WcMessageSignUseCase.SignModel>(),
    WcMessageSignUseCase {

    override val securityStatus: LceFlow<Throwable, CheckTransactionResult> =
        emptyFlow() // BlockAid doesn't support solana_signMessage

    override suspend fun SignCollector<WcMessageSignUseCase.SignModel>.onSign(
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ) {
        val hashToSign = method.rawMessage.decodeBase58() ?: byteArrayOf()

        // Never blind-sign a payload that is actually a serialized Solana transaction message — the
        // resulting signature would be a valid transaction signature that a malicious dApp could broadcast to move
        // the user's funds. Reject such requests instead of signing them.
        if (SolanaTransactionHelper.isTransactionMessage(hashToSign)) {
            emit(state.toResult(WcRequestError.UnknownError(TransactionSignAttemptException).left()))
            return
        }

        val userWallet = session.wallet

        val signedHash = signUseCase(hashToSign, userWallet, network)
            .onLeft { emit(state.toResult(parseTangemSdkError(it).left())) }
            .getOrNull() ?: return

        val respond = "{ signature: \"${signedHash.encodeBase58()}\" }"

        val wcRespondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(wcRespondResult))
    }

    override fun invoke(): Flow<WcSignState<WcMessageSignUseCase.SignModel>> {
        return delegate.invoke(initModel = WcMessageSignUseCase.SignModel(method.humanMsg))
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcSolanaMethod.SignMessage): WcSolanaMessageSignUseCase
    }
}