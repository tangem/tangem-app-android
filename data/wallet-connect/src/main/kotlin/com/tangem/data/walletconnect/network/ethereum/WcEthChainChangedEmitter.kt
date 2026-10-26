package com.tangem.data.walletconnect.network.ethereum

import com.reown.walletkit.client.Wallet
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.domain.walletconnect.WC_TAG
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Tells a dApp that the chain of its session has changed, through EIP-1193's `chainChanged`.
 *
 * A dApp's provider keeps a default chain of its own, and Uniswap does not ask to switch again once an
 * add succeeds. Without this event the next transaction it builds for the newly added chain arrives as a
 * request on the old one — which is exactly what [WcEthTransactionTargetValidator] then blocks.
 */
internal class WcEthChainChangedEmitter @Inject constructor(
    private val wcSdk: WcSdk,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun emit(topic: String, chainId: CAIP2) {
        val reference = chainId.reference.toIntOrNull() ?: return

        if (!awaitSessionCarrying(topic = topic, rawChainId = chainId.raw)) {
            TangemLogger.withTag(WC_TAG).e(
                "Failed to emit $CHAIN_CHANGED_EVENT: ${chainId.raw} did not appear in session $topic",
            )
            return
        }

        wcSdk.emitSessionEvent(
            Wallet.Params.SessionEmit(
                topic = topic,
                event = Wallet.Model.SessionEvent(
                    name = CHAIN_CHANGED_EVENT,
                    data = HEX_PREFIX + reference.toString(radix = HEX_RADIX),
                ),
                chainId = chainId.raw,
            ),
        ).fold(
            ifLeft = { error ->
                TangemLogger.withTag(WC_TAG).e("Failed to emit $CHAIN_CHANGED_EVENT for ${chainId.raw}", error)
            },
            ifRight = { TangemLogger.withTag(WC_TAG).i("Emitted $CHAIN_CHANGED_EVENT to ${chainId.raw}") },
        )
    }

    /**
     * The SDK refuses an event for a chain its own session does not carry, and the session update that
     * adds one reaches its store after the update call has returned. Each read goes to the SDK's database
     * on the calling thread, and the callers are on the main one, hence the IO dispatcher.
     */
    private suspend fun awaitSessionCarrying(topic: String, rawChainId: String): Boolean {
        return withContext(dispatchers.io) {
            withTimeoutOrNull(SESSION_UPDATE_TIMEOUT_MILLIS) {
                while (wcSdk.sessionCarriesChain(topic = topic, rawChainId = rawChainId) != true) {
                    delay(POLL_INTERVAL_MILLIS)
                }
                true
            } == true
        }
    }

    private companion object {
        const val CHAIN_CHANGED_EVENT = "chainChanged"
        const val HEX_RADIX = 16
        const val SESSION_UPDATE_TIMEOUT_MILLIS = 10_000L
        const val POLL_INTERVAL_MILLIS = 200L
    }
}