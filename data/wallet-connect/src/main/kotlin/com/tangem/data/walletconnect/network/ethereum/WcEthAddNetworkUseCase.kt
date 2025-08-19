package com.tangem.data.walletconnect.network.ethereum

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.extensions.hexToInt
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.network.ethereum.WcEthNetwork.NamespaceConverter.Companion.ETH_NAMESPACE_KEY
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcAddNetworkUseCase
import com.tangem.domain.walletconnect.usecase.method.WcNetworkDerivationState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class WcEthAddNetworkUseCase @AssistedInject constructor(
    private val respondService: WcRespondService,
    addSwitchCommonDelegateFactory: WcEthAddSwitchCommonDelegate.Factory,
    @Assisted val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.AddEthereumChain,
) : WcAddNetworkUseCase {

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

    override suspend fun invoke(): Either<HandleMethodError, WcAddNetworkUseCase.AddNetwork> {
        return addSwitchCommonDelegate
            .commonChecks(method.rawChain.chainId)
            .map { addedNetwork -> WcAddNetworkUseCase.AddNetwork(addedNetwork) }
    }

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

internal class WcEthAddSwitchCommonDelegate @AssistedInject constructor(
    private val networksConverter: WcNetworksConverter,
    @Assisted val context: WcMethodUseCaseContext,
) {

    private val wallet: UserWallet get() = context.session.wallet

    suspend fun commonChecks(hexChainId: String): Either<HandleMethodError, Network> {
        val caip2 = CAIP2.fromRaw("$ETH_NAMESPACE_KEY:${hexChainId.hexToInt()}")
            ?: return HandleMethodError.UnknownError("Failed to parse CAIP2").left()
        val generalNetwork = networksConverter.createNetwork(caip2.raw, wallet)
        if (generalNetwork == null) {
            return HandleMethodError.TangemUnsupportedNetwork(caip2.raw).left()
        }
        val addedNetwork = networksConverter.mainOrAnyWalletNetworkForRequest(caip2.raw, wallet)
        if (addedNetwork == null) {
            return HandleMethodError.NotAddedNetwork(generalNetwork.name).left()
        }
        return addedNetwork.right()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext): WcEthAddSwitchCommonDelegate
    }
}