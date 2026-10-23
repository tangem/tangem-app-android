package com.tangem.data.walletconnect.network.ethereum

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.core.local.moshi.SdkMoshi
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.network.ethereum.WcEthNetwork.NamespaceConverter.Companion.ETH_NAMESPACE_KEY
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter.Companion.fromJson
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.domain.walletconnect.WC_TAG
import com.tangem.domain.walletconnect.model.WcEthAddChain
import com.tangem.domain.walletconnect.model.WcEthMethodName
import com.tangem.domain.walletconnect.model.WcJsonRpcError
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import kotlinx.coroutines.withContext

/**
 * Answers `wallet_switchEthereumChain` and `wallet_addEthereumChain` from the session's networks alone,
 * before anything is shown to the user.
 *
 * A dApp that wants a network the session does not carry asks to switch to it first and, once that is
 * refused, asks to add it. Showing a sheet for the switch too made the user answer the same question
 * twice, so a switch is now answered silently — with [WcJsonRpcError.UnrecognizedChain] when the network
 * is missing, which is precisely what makes the dApp follow up with the add request that does show a
 * sheet.
 *
 * Membership comes from the SDK's own session, the only view of it that a just-approved add has already
 * reached: our [com.tangem.domain.walletconnect.repository.WcSessionsManager] models are rebuilt on wallet,
 * store and account changes, and a chain added through `updateSession` is none of those.
 */
internal class WcEthChainRequestResolver @Inject constructor(
    @SdkMoshi private val moshi: Moshi,
    private val wcSdk: WcSdk,
    private val respondService: WcRespondService,
    private val chainChangedEmitter: WcEthChainChangedEmitter,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Returns how [request] can be answered, or `null` when this resolver has nothing to say about it —
     * another method, or a session we cannot read, which the existing per-method flow handles as before.
     */
    suspend fun resolve(methodName: WcMethodName, request: WcSdkSessionRequest): Resolution? {
        val isChainRequest = methodName == WcEthMethodName.SwitchEthereumChain ||
            methodName == WcEthMethodName.AddEthereumChain
        if (!isChainRequest) return null

        val chainId = parseChainId(request.request.params) ?: return Resolution.InvalidParams
        val caip2 = CAIP2(namespace = ETH_NAMESPACE_KEY, reference = chainId.toString())

        // A session we cannot read leaves nothing to decide from, so the request goes on as before.
        val isInSession = withContext(dispatchers.io) {
            wcSdk.sessionCarriesChain(topic = request.topic, rawChainId = caip2.raw)
        } ?: return null

        return when (methodName) {
            WcEthMethodName.SwitchEthereumChain ->
                if (isInSession) Resolution.Switched(caip2) else Resolution.UnrecognizedChain
            else ->
                if (isInSession) Resolution.AlreadyAdded(caip2) else Resolution.NeedsUserApproval
        }
    }

    suspend fun respond(request: WcSdkSessionRequest, resolution: Resolution) {
        TangemLogger.withTag(WC_TAG).i(
            "Resolved ${request.request.method} request ${request.request.id} without UI: $resolution",
        )
        when (resolution) {
            is Resolution.Answered -> respondService
                .respond(request = request, response = null)
                .onRight { chainChangedEmitter.emit(topic = request.topic, chainId = resolution.chainId) }
            Resolution.UnrecognizedChain -> respondService.rejectRequestNonBlock(
                request = request,
                error = WcJsonRpcError.UnrecognizedChain,
            )
            Resolution.InvalidParams -> respondService.rejectRequestNonBlock(
                request = request,
                error = WcJsonRpcError.InvalidParams,
            )
            Resolution.NeedsUserApproval -> Unit
        }
    }

    /** Both EIP-3085 and EIP-3326 take exactly one entry carrying a `0x`-prefixed chain ID. */
    private fun parseChainId(rawParams: String): Int? {
        val entries = moshi.fromJson<List<WcEthAddChain>>(rawParams).getOrNull() ?: return null
        val entry = entries.singleOrNull() ?: return null
        return entry.chainId.removePrefix(HEX_PREFIX).toIntOrNull(radix = HEX_RADIX)
    }

    sealed interface Resolution {

        /**
         * Answered with success, and followed by `chainChanged` so the dApp's own provider moves to
         * [chainId] rather than keeping the chain it defaulted to.
         */
        sealed interface Answered : Resolution {
            val chainId: CAIP2
        }

        /** The network is already in the session, so the switch is simply true. */
        data class Switched(override val chainId: CAIP2) : Answered

        /** Nothing to add — the network is already in the session, so the add succeeds with no sheet. */
        data class AlreadyAdded(override val chainId: CAIP2) : Answered

        /** The session has no such network. For a switch this is the cue for the dApp to ask to add it. */
        data object UnrecognizedChain : Resolution

        data object InvalidParams : Resolution

        /** Only the user can answer this one, so the request goes on to the confirmation sheet. */
        data object NeedsUserApproval : Resolution
    }

    private companion object {
        const val HEX_RADIX = 16
    }
}