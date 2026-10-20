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
import com.tangem.common.core.run
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
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

internal class DefaultDeriveAndSignSession(
    private val derivationsRepository: DerivationsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val hotWalletAccessor: HotWalletAccessor,
    private val deriveInSession: DeriveInSession,
) : DeriveAndSignSession {

    @Inject
    constructor(
        derivationsRepository: DerivationsRepository,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemSdkManager: TangemSdkManager,
        hotWalletAccessor: HotWalletAccessor,
    ) : this(
        derivationsRepository = derivationsRepository,
        cardSdkConfigRepository = cardSdkConfigRepository,
        tangemSdkManager = tangemSdkManager,
        hotWalletAccessor = hotWalletAccessor,
        deriveInSession = { session, request ->
            DeriveWalletPublicKeyTask(walletPublicKey = request.seedKey, derivationPath = request.path).run(session)
        },
    )

    override suspend fun <T> run(
        userWallet: UserWallet,
        seedKey: ByteArray,
        path: DerivationPath,
        block: suspend DeriveAndSignScope.(derivedKey: ExtendedPublicKey) -> T,
    ): Either<TangemSdkError, T> {
        val storedKey = runSuspendCatching {
            derivationsRepository.getExistingDerivedKeys(userWallet, ByteArrayKey(seedKey))[path]
        }.getOrElse { error ->
            TangemLogger.e("Derive and sign session: reading the stored keys threw", error)
            return error.asTangemSdkError().left()
        }

        return when (userWallet) {
            is UserWallet.Cold -> if (storedKey != null) {
                val signer by lazy { userWallet.signer(session = null) }
                val publicKey = publicKey(seedKey, storedKey, path)
                runBlockCatching(DeriveAndSignScope { hashes -> signer.sign(hashes, publicKey) }, storedKey, block)
            } else {
                deriveInCardSession(userWallet = userWallet, seedKey = seedKey, path = path, block = block)
            }
            is UserWallet.Hot -> runHot(
                userWallet = userWallet,
                seedKey = seedKey,
                path = path,
                storedKey = storedKey,
                block = block,
            )
        }
    }

    private suspend fun <T> deriveInCardSession(
        userWallet: UserWallet.Cold,
        seedKey: ByteArray,
        path: DerivationPath,
        block: suspend DeriveAndSignScope.(ExtendedPublicKey) -> T,
    ): Either<TangemSdkError, T> = coroutineScope {
        val outcome = CompletableDeferred<Either<TangemSdkError, T>>()
        val cardId = userWallet.sessionCardId()

        val task = DeriveKeyInSessionTask(
            userWalletId = userWallet.walletId,
            request = DeriveKeyInSessionTask.Request(seedKey = seedKey, path = path),
            derivationsRepository = derivationsRepository,
            deriveInSession = deriveInSession,
            scope = this,
            onDerived = { session, derivedKey, sessionCallback ->
                val signingScope = SingleSignatureScope(
                    signer = lazy { userWallet.signer(session = session) },
                    publicKey = publicKey(seedKey, derivedKey, path),
                    sessionCallback = sessionCallback,
                )
                outcome.complete(runBlockCatching(signingScope, derivedKey, block))
                sessionCallback.close(signingScope.closingResult())
            },
        )

        val sessionResult = runSuspendCatching {
            tangemSdkManager.runTaskAsync(
                runnable = task,
                preflightReadFilter = if (cardId == null) {
                    UserWalletIdPreflightReadFilter(expectedUserWallet = userWallet)
                } else {
                    null
                },
                cardId = cardId,
                registerColdWallet = true,
            )
        }.getOrElse { error ->
            TangemLogger.e("Derive and sign session: starting the card session threw", error)
            CompletionResult.Failure(error.asTangemSdkError())
        }

        when (sessionResult) {
            is CompletionResult.Success -> outcome.await()
            is CompletionResult.Failure -> if (outcome.isCompleted) {
                outcome.await()
            } else {
                TangemLogger.i(
                    "Derive and sign session: the card session ended before the block, ${sessionResult.error}",
                )
                task.abandonSession()
                coroutineContext.cancelChildren()
                sessionResult.error.asTangemSdkError().left()
            }
        }
    }

    private suspend fun <T> runHot(
        userWallet: UserWallet.Hot,
        seedKey: ByteArray,
        path: DerivationPath,
        storedKey: ExtendedPublicKey?,
        block: suspend DeriveAndSignScope.(ExtendedPublicKey) -> T,
    ): Either<TangemSdkError, T> {
        val hotWalletId = userWallet.hotWalletId
        val curve = userWallet.wallets.orEmpty().firstOrNull { it.publicKey.contentEquals(seedKey) }?.curve
            ?: return TangemSdkError.WalletNotFound().left()
        val unlock = RunScopedUnlock(hotWalletId)

        return try {
            val derivedKey = storedKey ?: runSuspendCatching {
                deriveHot(userWallet = userWallet, seedKey = seedKey, path = path, curve = curve, unlock = unlock)
            }.getOrElse { error -> return error.asTangemSdkError().left() }

            val signingScope = DeriveAndSignScope { hashes ->
                runSuspendCatching {
                    signHot(unlock = unlock, curve = curve, path = path, hashes = hashes)
                }.fold(
                    onSuccess = { CompletionResult.Success(it) },
                    onFailure = { CompletionResult.Failure(it.asTangemSdkError()) },
                )
            }
            runBlockCatching(signingScope, derivedKey, block)
        } finally {
            unlock.clearIfEstablished()
        }
    }

    private suspend fun deriveHot(
        userWallet: UserWallet.Hot,
        seedKey: ByteArray,
        path: DerivationPath,
        curve: EllipticCurve,
        unlock: RunScopedUnlock,
    ): ExtendedPublicKey {
        unlock.ensureUnlocked()
        val response = hotWalletAccessor.derivePublicKeysReusingContextualUnlock(
            hotWalletId = userWallet.hotWalletId,
            request = DeriveWalletRequest(
                requests = listOf(DeriveWalletRequest.Request(curve = curve, paths = listOf(path))),
            ),
        )
        val derivedKey = response.responses
            .first { it.seedKey.publicKey.contentEquals(seedKey) }
            .publicKeys.getValue(path)

        derivationsRepository.storeDerivedKeys(
            userWalletId = userWallet.walletId,
            derivedKeys = mapOf(ByteArrayKey(seedKey) to ExtendedPublicKeysMap(mapOf(path to derivedKey))),
        )
        return derivedKey
    }

    private suspend fun signHot(
        unlock: RunScopedUnlock,
        curve: EllipticCurve,
        path: DerivationPath,
        hashes: List<ByteArray>,
    ): List<ByteArray> {
        unlock.ensureUnlocked()
        return hotWalletAccessor.signHashesReusingContextualUnlock(
            hotWalletId = unlock.hotWalletId,
            dataToSign = listOf(DataToSign(curve = curve, derivationPath = path, hashes = hashes)),
        ).flatMap { it.signatures }
    }

    private inner class RunScopedUnlock(val hotWalletId: HotWalletId) {

        private val isRequestedByThisRun = AtomicBoolean(false)
        private val wasUnlockedBeforeRun = AtomicBoolean(false)
        private val establishedByThisRun = AtomicReference<UnlockHotWallet?>(null)

        suspend fun ensureUnlocked() {
            if (isRequestedByThisRun.getAndSet(true)) return

            wasUnlockedBeforeRun.set(hotWalletAccessor.getContextualUnlock(hotWalletId) != null)
            establishedByThisRun.set(hotWalletAccessor.unlockContextual(hotWalletId))
        }

        fun clearIfEstablished() {
            if (!isRequestedByThisRun.get() || wasUnlockedBeforeRun.get()) return

            val current = hotWalletAccessor.getContextualUnlock(hotWalletId) ?: return
            val established = establishedByThisRun.get()
            val isRequestUnfinished = established == null
            if (isRequestUnfinished || current === established) {
                hotWalletAccessor.clearContextualUnlock(hotWalletId)
            }
        }
    }

    private suspend fun <T> runBlockCatching(
        scope: DeriveAndSignScope,
        derivedKey: ExtendedPublicKey,
        block: suspend DeriveAndSignScope.(ExtendedPublicKey) -> T,
    ): Either<TangemSdkError, T> = try {
        runSuspendCatching { scope.block(derivedKey) }.fold(
            onSuccess = { it.right() },
            onFailure = { error ->
                TangemLogger.e("Derive and sign session: the block threw", error)
                error.asTangemSdkError().left()
            },
        )
    } catch (strayCancellation: CancellationException) {
        currentCoroutineContext().ensureActive()
        TangemLogger.e("Derive and sign session: the block was cancelled from inside", strayCancellation)
        strayCancellation.asTangemSdkError().left()
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

    private fun Throwable.asTangemSdkError(): TangemSdkError =
        this as? TangemSdkError ?: TangemSdkError.ExceptionError(this)

    private class SingleSignatureScope(
        private val signer: Lazy<TransactionSigner>,
        private val publicKey: Wallet.PublicKey,
        private val sessionCallback: OneShotSessionCallback,
    ) : DeriveAndSignScope {

        private val lastFailure = AtomicReference<TangemError?>(null)

        override suspend fun sign(hashes: List<ByteArray>): CompletionResult<List<ByteArray>> {
            if (!sessionCallback.isOpen) return CompletionResult.Failure(sessionClosedAfterSignature())

            return signer.value.sign(hashes, publicKey).also { result ->
                when (result) {
                    is CompletionResult.Success -> sessionCallback.close(CompletionResult.Success(Unit))
                    is CompletionResult.Failure -> lastFailure.set(result.error)
                }
            }
        }

        fun closingResult(): CompletionResult<Unit> =
            lastFailure.get()?.let { CompletionResult.Failure(it) } ?: CompletionResult.Success(Unit)

        private fun sessionClosedAfterSignature() =
            TangemSdkError.ExceptionError(IllegalStateException("The card session closes after the first signature"))
    }
}