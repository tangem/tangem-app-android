package com.tangem.data.walletconnect.network.ethereum

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.Wallet.Model
import com.reown.walletkit.client.WalletKit
import com.tangem.blockchain.extensions.hexToInt
import com.tangem.data.walletconnect.model.CAIP10
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class WcEthAddNetworkUseCase @AssistedInject constructor(
    private val respondService: WcRespondService,
    private val networksConverter: WcNetworksConverter,
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
            .map { addedNetwork ->
                WcAddNetworkUseCase.AddNetwork(
                    network = addedNetwork,
                    isExistInWcSession = addSwitchCommonDelegate.existInWcSession(addedNetwork),
                )
            }
    }

    override suspend fun approve(): Either<WcRequestError, String> {
        fun illegalState() = WcRequestError.UnknownError(IllegalStateException("IllegalStateException")).left()
        val requestedNetworkCAIP2 = CAIP2.fromRaw(rawSdkRequest.chainId.orEmpty()) ?: return illegalState()
        val networkToAddCAIP2 = addSwitchCommonDelegate.hexChainIdToCAIP2(method.rawChain.chainId)
            ?: return illegalState()
        val namespaces = session.sdkModel.namespaces[requestedNetworkCAIP2.namespace]
            ?: return illegalState()
        // find and add all derivation
        val networkToAddCAIP10 = networksConverter
            .allAddressForChain(networkToAddCAIP2.raw, wallet)
            .map { address -> CAIP10(networkToAddCAIP2, address).raw }
        val newNamespaces = namespaces.copy(
            chains = namespaces.chains.plus(networkToAddCAIP2.raw),
            accounts = namespaces.accounts.plus(networkToAddCAIP10),
        )
        val sdkNewNamespaces = session.sdkModel.namespaces
            .plus(requestedNetworkCAIP2.namespace to newNamespaces)
            .mapValues { (_, session) ->
                Model.Namespace.Session(
                    chains = session.chains,
                    accounts = session.accounts,
                    methods = session.methods,
                    events = session.events,
                )
            }

        val sessionUpdate = Wallet.Params.SessionUpdate(
            sessionTopic = context.session.sdkModel.topic,
            namespaces = sdkNewNamespaces,
        )
        sdkUpdateSession(sessionUpdate) // ignore result for now
        return respondService.respond(rawSdkRequest, "")
    }

    private suspend fun sdkUpdateSession(sessionUpdate: Wallet.Params.SessionUpdate): Either<Throwable, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.updateSession(
                params = sessionUpdate,
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
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

    fun hexChainIdToCAIP2(hexChainId: String): CAIP2? = CAIP2.fromRaw("$ETH_NAMESPACE_KEY:${hexChainId.hexToInt()}")

    fun existInWcSession(network: Network): Boolean {
        return context.session.networks.any { it.rawId == network.rawId }
    }

    suspend fun commonChecks(hexChainId: String): Either<HandleMethodError, Network> {
        val caip2 = hexChainIdToCAIP2(hexChainId)
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