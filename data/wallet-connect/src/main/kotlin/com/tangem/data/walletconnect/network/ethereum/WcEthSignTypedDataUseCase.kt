package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.network.ethereum.LegacySdkHelper.prepareToSendMessageData
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.error.parseTangemSdkError
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
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

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = context.accountAddress,
    )

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
        emitAll(delegate(model))
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.SignTypedData): WcEthSignTypedDataUseCase
    }
}