package com.tangem.data.wallets.signing

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

internal typealias DeriveInSession =
    suspend (session: CardSession, request: DeriveKeyInSessionTask.Request) -> CompletionResult<ExtendedPublicKey>

internal typealias OnKeyDerived =
    suspend (session: CardSession?, derivedKey: ExtendedPublicKey, sessionCallback: OneShotSessionCallback) -> Unit

class DeriveKeyInSessionTask internal constructor(
    private val userWalletId: UserWalletId,
    private val request: Request,
    private val derivationsRepository: DerivationsRepository,
    private val deriveInSession: DeriveInSession,
    private val scope: CoroutineScope,
    private val onDerived: OnKeyDerived,
) : CardSessionRunnable<Unit> {

    class Request(val seedKey: ByteArray, val path: DerivationPath)

    private val activeSessionCallback = AtomicReference<OneShotSessionCallback?>(null)

    override fun run(session: CardSession, callback: CompletionCallback<Unit>) {
        launchClosingOnCompletion(callback) { sessionCallback ->
            when (val derived = deriveInSession(session, request)) {
                is CompletionResult.Failure -> sessionCallback.close(CompletionResult.Failure(derived.error))
                is CompletionResult.Success -> storeAndContinue(session, derived.data, sessionCallback)
            }
        }
    }

    fun runWithoutCard(derivedKey: ExtendedPublicKey, callback: CompletionCallback<Unit>) {
        launchClosingOnCompletion(callback) { sessionCallback ->
            storeAndContinue(session = null, derivedKey, sessionCallback)
        }
    }

    private fun launchClosingOnCompletion(
        callback: CompletionCallback<Unit>,
        action: suspend (OneShotSessionCallback) -> Unit,
    ) {
        val sessionCallback = OneShotSessionCallback(callback).also(activeSessionCallback::set)
        scope.launch {
            runSuspendCatching { action(sessionCallback) }.onFailure { error ->
                TangemLogger.e("Derive and sign session: the derivation threw", error)
                sessionCallback.close(CompletionResult.Failure(TangemSdkError.ExceptionError(error)))
            }
        }.invokeOnCompletion { sessionCallback.close(CompletionResult.Failure(TangemSdkError.UserCancelled())) }
    }

    internal fun abandonSession() {
        activeSessionCallback.get()?.abandon()
    }

    private suspend fun storeAndContinue(
        session: CardSession?,
        derivedKey: ExtendedPublicKey,
        sessionCallback: OneShotSessionCallback,
    ) {
        derivationsRepository.storeDerivedKeys(
            userWalletId = userWalletId,
            derivedKeys = mapOf(
                ByteArrayKey(request.seedKey) to ExtendedPublicKeysMap(mapOf(request.path to derivedKey)),
            ),
        )
        onDerived(session, derivedKey, sessionCallback)
    }
}