package com.tangem.tap.domain.tasks

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.utils.logging.TangemLogger

/**
 * Derives [derivations] and fails the session with [TangemSdkError.WalletNotFound] when the tapped card derived
 * none of them.
 *
 * A per-path [TangemSdkError.WalletNotFound] is a soft error inside [DeriveMultipleWalletPublicKeysTask]: derivation
 * continues with the remaining paths, so a card holding only some of the requested wallets still returns what it has.
 * A card holding none of them — a factory-reset one, which the preflight filter cannot identify either — would
 * otherwise finish the session successfully with an empty response, leaving the user with a "Done" NFC sheet and an
 * operation that did nothing.
 *
 * A partial result stays a success, as before.
 */
internal class DerivePublicKeysTask(
    private val derivations: Map<ByteArrayKey, List<DerivationPath>>,
    private val inner: CardSessionRunnable<DerivationTaskResponse> = DeriveMultipleWalletPublicKeysTask(derivations),
) : CardSessionRunnable<DerivationTaskResponse> by inner {

    override fun run(session: CardSession, callback: CompletionCallback<DerivationTaskResponse>) {
        inner.run(session) { result ->
            if (result is CompletionResult.Success && result.data.hasNoDerivedKeys()) {
                TangemLogger.e("Tapped card derived no keys of ${derivations.size} requested wallet(s)")
                callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            } else {
                callback(result)
            }
        }
    }

    private fun DerivationTaskResponse.hasNoDerivedKeys(): Boolean {
        return derivations.isNotEmpty() && entries.values.all(Map<*, *>::isEmpty)
    }
}