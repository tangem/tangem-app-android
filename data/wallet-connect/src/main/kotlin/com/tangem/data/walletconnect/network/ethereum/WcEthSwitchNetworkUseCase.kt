package com.tangem.data.walletconnect.network.ethereum

import arrow.core.Either
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcNetworkDerivationState
import com.tangem.domain.walletconnect.usecase.method.WcSwitchNetworkUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class WcEthSwitchNetworkUseCase @AssistedInject constructor(
    private val respondService: WcRespondService,
    @Assisted val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.SwitchEthereumChain,
    addSwitchCommonDelegateFactory: WcEthAddSwitchCommonDelegate.Factory,
) : WcSwitchNetworkUseCase {

    override val session: WcSession
        get() = context.session
    override val rawSdkRequest: WcSdkSessionRequest
        get() = context.rawSdkRequest
    override val network: Network
        get() = context.network
    override val derivationState: WcNetworkDerivationState = when {
        context.networkDerivationsCount > 1 -> WcNetworkDerivationState.Multiple(walletAddress = context.accountAddress)
        else -> WcNetworkDerivationState.Single
    }

    private val addSwitchCommonDelegate = addSwitchCommonDelegateFactory.create(context)

    override suspend fun invoke(): Either<HandleMethodError, WcSwitchNetworkUseCase.SwitchNetwork> {
        return addSwitchCommonDelegate
            .commonChecks(method.rawChain.chainId)
            .map { addedNetwork -> WcSwitchNetworkUseCase.SwitchNetwork(addedNetwork) }
    }

    override fun reject() {
        respondService.rejectRequestNonBlock(rawSdkRequest)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.SwitchEthereumChain): WcEthSwitchNetworkUseCase
    }
}