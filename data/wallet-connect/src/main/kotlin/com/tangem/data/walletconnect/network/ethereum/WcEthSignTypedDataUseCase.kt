package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.network.ethereum.LegacySdkHelper.prepareToSendMessageData
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.WC_TAG
import com.tangem.domain.walletconnect.error.parseTangemSdkError
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

@Suppress("LongParameterList")
internal class WcEthSignTypedDataUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.SignTypedData,
    private val walletManagersFacade: WalletManagersFacade,
    private val signUseCase: SignUseCase,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<Nothing, WcMessageSignUseCase.SignModel>(),
    WcMessageSignUseCase {

    private val violation = WcEthTypedDataPolicy.findViolation(
        typedData = method.dataForSign,
        signerAddress = context.accountAddress,
    )

    override val securityStatus: LceFlow<Throwable, CheckTransactionResult> = if (violation != null) {
        emptyFlow()
    } else {
        blockAidDelegate.getSecurityStatus(
            network = network,
            method = method,
            rawSdkRequest = rawSdkRequest,
            session = session,
            accountAddress = context.accountAddress,
        )
    }

    override suspend fun SignCollector<WcMessageSignUseCase.SignModel>.onSign(
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ) {
        val hashToSign = EthereumUtils.makeTypedDataHash(method.dataForSign)
        val userWallet = session.wallet
        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
            ?: return

        val signedHash = signUseCase(hashToSign, userWallet, network)
            .onLeft { emit(state.toResult(parseTangemSdkError(it).left())) }
            .getOrNull() ?: return

        val respond = prepareToSendMessageData(signedHash, hashToSign, walletManager)
        val wcRespondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(wcRespondResult))
    }

    override fun invoke(): Flow<WcSignState<WcMessageSignUseCase.SignModel>> = flow {
        val model = WcMessageSignUseCase.SignModel(humanMsg = method.humanMsg)
        if (violation != null) {
            TangemLogger.withTag(WC_TAG).e("Signing of forbidden typed data blocked ($violation)")
            emit(WcSignState(model, WcSignStep.Result(WcRequestError.ForbiddenRequest.left())))
        } else {
            emitAll(delegate(model))
        }
    }

    /**
     * A forbidden request never starts the [delegate] flow, so its cancel action would not be collected: reject
     * directly instead, without a `ButtonCancel` event since the user did not dismiss anything.
     */
    override fun cancel() {
        if (violation != null) defaultReject() else super.cancel()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.SignTypedData): WcEthSignTypedDataUseCase
    }
}