package com.tangem.data.collectibles.account.derivation

import arrow.core.Either
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemError
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
 * Derives the owner key at [COLLECTIBLES_OWNER_DERIVATION_PATH], stores it and runs [setUpVault] — all on the
 * session this task was started with, so onboarding costs a single tap. The derivation runs as a nested runnable,
 * the way `TangemPayGenerateAddressAndSignChallengeTask` does it, because `DerivationsRepository.derivePublicKeys`
 * would open a session of its own.
 *
 * The key is persisted before [setUpVault] runs: the wallet manager the vault is read and signed through is
 * resolved from the wallet's stored derivations. It stays persisted when the rest fails, so a retry still costs
 * one tap.
 *
 * @property setUpVault the caller's work, handed a signer that runs on this very session
 */
internal class DeriveCollectiblesOwnerAndSetUpVaultTask(
    private val userWallet: UserWallet.Cold,
    private val seedKey: ByteArray,
    private val coroutineScope: CoroutineScope,
    private val derivationsRepository: DerivationsRepository,
    private val setUpVault: suspend (signer: TransactionSigner) -> Either<TangemError, Unit>,
) : CardSessionRunnable<Unit> {

    private val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)

    override fun run(session: CardSession, callback: CompletionCallback<Unit>) {
        coroutineScope.launch {
            callback(deriveAndSetUpVault(session))
        }
    }

    private suspend fun deriveAndSetUpVault(session: CardSession): CompletionResult<Unit> = runSuspendCatching {
        val owner = when (val result = deriveOwner(session)) {
            is CompletionResult.Failure -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success -> result.data
        }

        // Without the stored key there is no wallet manager to sign with, so a failed write aborts everything
        storeOwner(owner)

        setUpVaultInSession(session)
    }.getOrElse { error ->
        TangemLogger.e("Collectibles vault: the one-tap set-up threw", error)
        CompletionResult.Failure(GachaVaultError.Unexpected(cause = error).tangemError)
    }

    private suspend fun setUpVaultInSession(session: CardSession): CompletionResult<Unit> {
        return setUpVault(InSessionTransactionSigner(session)).fold(
            // Every failure leaves as one: a Success would end the session with the success animation
            ifLeft = { error -> CompletionResult.Failure(error) },
            ifRight = { CompletionResult.Success(Unit) },
        )
    }

    private suspend fun deriveOwner(session: CardSession): CompletionResult<ExtendedPublicKey> {
        return DeriveWalletPublicKeyTask(walletPublicKey = seedKey, derivationPath = path).run(session = session)
    }

    private suspend fun storeOwner(owner: ExtendedPublicKey) {
        derivationsRepository.storeDerivedKeys(
            userWalletId = userWallet.walletId,
            derivedKeys = mapOf(ByteArrayKey(seedKey) to ExtendedPublicKeysMap(mapOf(path to owner))),
        )
    }
}