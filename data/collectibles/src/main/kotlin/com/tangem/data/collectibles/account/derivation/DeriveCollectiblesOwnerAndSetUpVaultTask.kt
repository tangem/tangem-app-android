package com.tangem.data.collectibles.account.derivation

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.run
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Derives the owner key at [COLLECTIBLES_OWNER_DERIVATION_PATH], stores it and runs [setUpVault], all on the
 * session it was started with — one tap. The derivation is a nested runnable because
 * `DerivationsRepository.derivePublicKeys` would open a session of its own, and the key is stored before
 * [setUpVault] because the wallet manager it signs through resolves the key from stored derivations. It stays
 * stored when the rest fails, so a retry is still one tap.
 */
internal class DeriveCollectiblesOwnerAndSetUpVaultTask(
    private val userWallet: UserWallet.Cold,
    private val seedKey: ByteArray,
    private val derivationsRepository: DerivationsRepository,
    private val scope: CoroutineScope,
    private val setUpVault: suspend (session: CardSession, callback: CompletionCallback<Unit>) -> Unit,
) : CardSessionRunnable<Unit> {

    private val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)

    override fun run(session: CardSession, callback: CompletionCallback<Unit>) {
        TangemLogger.d("cardTask: card session opened, deriving the Solana address from the main key")
        scope.launch { deriveAndSetUpVault(session, callback) }
    }

    private suspend fun deriveAndSetUpVault(session: CardSession, callback: CompletionCallback<Unit>) {
        val derivation = runSuspendCatching { deriveAndStoreOwner(session) }
            .getOrElse { error ->
                TangemLogger.e("Collectibles vault: the owner derivation threw", error)
                CompletionResult.Failure(GachaVaultError.Unexpected(cause = error).tangemError)
            }

        when (derivation) {
            is CompletionResult.Failure -> {
                TangemLogger.d("cardTask: owner key derivation failed: ${derivation.error.customMessage}")
                callback(CompletionResult.Failure(derivation.error))
            }
            is CompletionResult.Success -> {
                TangemLogger.d("cardTask: owner key derived and saved, continuing in the same session")
                setUpVault(session, callback)
            }
        }
    }

    private suspend fun deriveAndStoreOwner(session: CardSession): CompletionResult<Unit> {
        val owner = when (val result = deriveOwner(session)) {
            is CompletionResult.Failure -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success -> result.data
        }

        derivationsRepository.storeDerivedKeys(
            userWalletId = userWallet.walletId,
            derivedKeys = mapOf(ByteArrayKey(seedKey) to ExtendedPublicKeysMap(mapOf(path to owner))),
        )

        return CompletionResult.Success(Unit)
    }

    private suspend fun deriveOwner(session: CardSession): CompletionResult<ExtendedPublicKey> {
        return DeriveWalletPublicKeyTask(walletPublicKey = seedKey, derivationPath = path).run(session = session)
    }
}