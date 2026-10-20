package com.tangem.data.wallets.signing

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.domain.wallets.signing.DeriveAndSignError
import com.tangem.domain.wallets.signing.DeriveAndSignSession
import com.tangem.domain.wallets.signing.SignRequest
import com.tangem.domain.wallets.signing.Signed
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal class DefaultDeriveAndSignSession @Inject constructor(
    private val derivationsRepository: DerivationsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val hotWalletAccessor: HotWalletAccessor,
    private val inSessionKeyDeriver: InSessionKeyDeriver,
) : DeriveAndSignSession {

    override suspend fun <T> run(
        userWallet: UserWallet,
        seedKey: ByteArray,
        path: DerivationPath,
        prepare: suspend (derivedKey: ExtendedPublicKey) -> SignRequest<T>,
    ): Either<DeriveAndSignError, Signed<T>> = runSuspendCatching {
        val storedKey = derivationsRepository.getExistingDerivedKeys(userWallet, ByteArrayKey(seedKey))[path]
        when (userWallet) {
            is UserWallet.Cold -> if (storedKey != null) {
                signWithStoredKey(
                    userWallet = userWallet,
                    publicKey = publicKey(seedKey = seedKey, derivedKey = storedKey, path = path),
                    storedKey = storedKey,
                    prepare = prepare,
                )
            } else {
                deriveAndSignInCardSession(userWallet = userWallet, seedKey = seedKey, path = path, prepare = prepare)
            }
            is UserWallet.Hot -> runHot(
                userWallet = userWallet,
                seedKey = seedKey,
                path = path,
                storedKey = storedKey,
                prepare = prepare,
            )
        }
    }.getOrElse { error ->
        TangemLogger.e("Derive and sign session failed", error)
        error.toDeriveAndSignError().left()
    }

    private suspend fun <T> signWithStoredKey(
        userWallet: UserWallet.Cold,
        publicKey: Wallet.PublicKey,
        storedKey: ExtendedPublicKey,
        prepare: suspend (ExtendedPublicKey) -> SignRequest<T>,
    ): Either<DeriveAndSignError, Signed<T>> {
        val request = prepare(storedKey)
        if (request.hashes.isEmpty()) return Signed(request.value, emptyList()).right()

        return when (val signed = userWallet.signer(session = null).sign(request.hashes, publicKey)) {
            is CompletionResult.Success -> Signed(request.value, signed.data).right()
            is CompletionResult.Failure -> signed.error.toDeriveAndSignError().left()
        }
    }

    private suspend fun <T> deriveAndSignInCardSession(
        userWallet: UserWallet.Cold,
        seedKey: ByteArray,
        path: DerivationPath,
        prepare: suspend (ExtendedPublicKey) -> SignRequest<T>,
    ): Either<DeriveAndSignError, Signed<T>> = coroutineScope {
        val cardId = userWallet.sessionCardId()
        val task = DeriveAndSignTask(
            scope = this,
            derive = { session ->
                deriveAndStore(session = session, userWallet = userWallet, seedKey = seedKey, path = path)
            },
            prepare = prepare,
            sign = { session, derivedKey, hashes ->
                val publicKey = publicKey(seedKey = seedKey, derivedKey = derivedKey, path = path)
                userWallet.signer(session).sign(hashes, publicKey)
            },
        )

        val result = tangemSdkManager.runTaskAsync(
            runnable = task,
            preflightReadFilter = if (cardId == null) {
                UserWalletIdPreflightReadFilter(expectedUserWallet = userWallet)
            } else {
                null
            },
            cardId = cardId,
            registerColdWallet = true,
        )
        task.abandon()
        coroutineContext.cancelChildren()

        when (result) {
            is CompletionResult.Success -> result.data.right()
            is CompletionResult.Failure -> result.error.toDeriveAndSignError().left()
        }
    }

    private suspend fun deriveAndStore(
        session: CardSession,
        userWallet: UserWallet.Cold,
        seedKey: ByteArray,
        path: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> {
        val derived = inSessionKeyDeriver.derive(session, seedKey, path)
        if (derived is CompletionResult.Success) {
            storeDerivedKey(userWallet = userWallet, seedKey = seedKey, path = path, derivedKey = derived.data)
        }
        return derived
    }

    private suspend fun <T> runHot(
        userWallet: UserWallet.Hot,
        seedKey: ByteArray,
        path: DerivationPath,
        storedKey: ExtendedPublicKey?,
        prepare: suspend (ExtendedPublicKey) -> SignRequest<T>,
    ): Either<DeriveAndSignError, Signed<T>> {
        val curve = userWallet.wallets.orEmpty().firstOrNull { it.publicKey.contentEquals(seedKey) }?.curve
            ?: return DeriveAndSignError.WalletNotFound.left()
        val unlock = RunScopedUnlock(userWallet.hotWalletId)

        return try {
            val derivedKey = storedKey ?: deriveHot(
                userWallet = userWallet,
                seedKey = seedKey,
                path = path,
                curve = curve,
                unlock = unlock.current(),
            )
            val request = prepare(derivedKey)
            val signatures = if (request.hashes.isEmpty()) {
                emptyList()
            } else {
                hotWalletAccessor.signHashesWithUnlock(
                    unlockHotWallet = unlock.current(),
                    dataToSign = listOf(DataToSign(curve = curve, derivationPath = path, hashes = request.hashes)),
                ).flatMap { it.signatures }
            }
            Signed(request.value, signatures).right()
        } finally {
            unlock.clearIfEstablished()
        }
    }

    private suspend fun deriveHot(
        userWallet: UserWallet.Hot,
        seedKey: ByteArray,
        path: DerivationPath,
        curve: EllipticCurve,
        unlock: UnlockHotWallet,
    ): ExtendedPublicKey {
        val derivedKey = hotWalletAccessor.derivePublicKeysWithUnlock(
            unlockHotWallet = unlock,
            request = DeriveWalletRequest(
                requests = listOf(DeriveWalletRequest.Request(curve = curve, paths = listOf(path))),
            ),
        ).responses
            .first { it.seedKey.publicKey.contentEquals(seedKey) }
            .publicKeys.getValue(path)
        storeDerivedKey(userWallet = userWallet, seedKey = seedKey, path = path, derivedKey = derivedKey)
        return derivedKey
    }

    private suspend fun storeDerivedKey(
        userWallet: UserWallet,
        seedKey: ByteArray,
        path: DerivationPath,
        derivedKey: ExtendedPublicKey,
    ) {
        derivationsRepository.storeDerivedKeys(
            userWalletId = userWallet.walletId,
            derivedKeys = mapOf(ByteArrayKey(seedKey) to ExtendedPublicKeysMap(mapOf(path to derivedKey))),
        )
    }

    private inner class RunScopedUnlock(private val hotWalletId: HotWalletId) {

        private var wasUnlockedBeforeRun = false
        private var establishedByThisRun: UnlockHotWallet? = null

        suspend fun current(): UnlockHotWallet {
            val established = establishedByThisRun
            if (established != null && hotWalletAccessor.getContextualUnlock(hotWalletId) != null) return established

            wasUnlockedBeforeRun = hotWalletAccessor.getContextualUnlock(hotWalletId) != null
            return hotWalletAccessor.unlockContextual(hotWalletId).also { establishedByThisRun = it }
        }

        fun clearIfEstablished() {
            val established = establishedByThisRun ?: return
            if (wasUnlockedBeforeRun) return

            if (hotWalletAccessor.getContextualUnlock(hotWalletId) === established) {
                hotWalletAccessor.clearContextualUnlock(hotWalletId)
            }
        }
    }

    private fun UserWallet.Cold.signer(session: CardSession?): TransactionSigner =
        cardSdkConfigRepository.getCommonSigner(
            cardId = sessionCardId(),
            twinKey = TwinKey.getOrNull(scanResponse = scanResponse),
            userWalletId = walletId,
            session = session,
        )

    private fun UserWallet.Cold.sessionCardId(): String? {
        val card = scanResponse.card
        return card.cardId.takeIf { card.backupStatus?.isActive != true && !card.isTangemTwins }
    }

    private fun publicKey(seedKey: ByteArray, derivedKey: ExtendedPublicKey, path: DerivationPath) = Wallet.PublicKey(
        seedKey = seedKey,
        derivationType = Wallet.PublicKey.DerivationType.Plain(
            Wallet.HDKey(extendedPublicKey = derivedKey, path = path),
        ),
    )

    private fun Throwable.toDeriveAndSignError(): DeriveAndSignError =
        (this as? TangemError)?.toDeriveAndSignError() ?: DeriveAndSignError.Unknown

    private fun TangemError.toDeriveAndSignError(): DeriveAndSignError = when (this) {
        is TangemSdkError.UserCancelled -> DeriveAndSignError.UserCancelled
        is TangemSdkError.WalletNotFound -> DeriveAndSignError.WalletNotFound
        is TangemSdkError.ExceptionError -> DeriveAndSignError.Unknown
        else -> DeriveAndSignError.CardError
    }
}