package com.tangem.data.walletconnect.network.ethereum

import arrow.core.Either
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcAddNetworkUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class WcEthAddNetworkUseCase @AssistedInject constructor(
    private val respondService: WcRespondService,
    @Assisted val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.AddEthereumChain,
) : WcAddNetworkUseCase {

    override val session: WcSession
        get() = context.session
    override val rawSdkRequest: WcSdkSessionRequest
        get() = context.rawSdkRequest
    override val network: Network
        get() = context.network
    override val walletAddress: String
        get() = context.accountAddress

    override suspend fun approve(): Either<WcRequestError, String> {
        return respondService.respond(rawSdkRequest, "")
    }

    override fun reject() {
        respondService.rejectRequestNonBlock(rawSdkRequest)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.AddEthereumChain): WcEthAddNetworkUseCase
    }
}