package com.tangem.data.walletconnect.network.solana

import arrow.core.left
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class WcSolanaMessageSignUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcSolanaMethod.SignMessage,
    private val signUseCase: SignUseCase,
    override val respondService: WcRespondService,
) : BaseWcSignUseCase<Nothing, WcMessageSignUseCase.SignModel>(),
    WcMessageSignUseCase {

    override val securityStatus: LceFlow<Throwable, CheckTransactionResult> =
        emptyFlow() // BlockAid doesn't support solana_signMessage

    override suspend fun SignCollector<WcMessageSignUseCase.SignModel>.onSign(
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ) {
        val hashToSign = method.rawMessage.decodeBase58() ?: byteArrayOf()
        val userWallet = session.wallet

        val signedHash = signUseCase(hashToSign, userWallet, network)
            .onLeft { emit(state.toResult(it.left())) }
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