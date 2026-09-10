package com.tangem.data.collectibles.delegate

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.models.account.AccountId
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.tangem.domain.collectibles.CollectiblesAccountError

/**
 * Drives the hot SDK directly with [HotAuth.NoAuth] instead of going through
 * [com.tangem.domain.wallets.hot.HotWalletAccessor]: the accessor looks wallets up in `UserWalletsListRepository`,
 * where this key deliberately is not, and prompts for auth on every call.
 */
@Singleton
internal class DefaultCollectiblesHotDelegate @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val store: CollectiblesHotDelegateStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesHotDelegate {

    private val lock = Mutex()

    override suspend fun getPublicKey(accountId: AccountId): HotDelegatePublicKey? {
        return withContext(dispatchers.io) {
            store.get(accountId)?.let { HotDelegatePublicKey(it.publicKeyBase58) }
        }
    }

    override suspend fun provision(accountId: AccountId): Either<CollectiblesAccountError, HotDelegatePublicKey> {
        return withContext(dispatchers.io) {
            lock.withLock {
                val stored = store.get(accountId)

                if (stored != null) return@withLock HotDelegatePublicKey(stored.publicKeyBase58).right()

                runSuspendCatching {
                    val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, MnemonicType.Words12)

                    runSuspendCatching {
                        val publicKey = derivePublicKey(hotWalletId)

                        store.put(
                            accountId = accountId,
                            record = CollectiblesHotDelegateRecord(
                                hotWalletId = hotWalletId.value,
                                authType = hotWalletId.authType.name,
                                publicKeyBase58 = publicKey.base58,
                            ),
                        )

                        publicKey
                    }
                        // A keystore entry no record points at is unreachable: drop it before reporting the failure.
                        .onFailure { deleteQuietly(hotWalletId) }
                        .getOrThrow()
                }.fold(
                    onSuccess = { it.right() },
                    onFailure = { CollectiblesAccountError.DelegateKeyCreationFailed(it).left() },
                )
            }
        }
    }

    override suspend fun sign(accountId: AccountId, message: ByteArray): Either<CollectiblesAccountError, ByteArray> {
        return withContext(dispatchers.io) {
            val record = store.get(accountId)
                ?: return@withContext CollectiblesAccountError.DelegateNotProvisioned.left()

            runSuspendCatching {
                val signed = tangemHotSdk.signHashes(
                    unlockHotWallet = record.toUnlock(),
                    dataToSign = listOf(
                        DataToSign(curve = CURVE, derivationPath = derivationPath, hashes = listOf(message)),
                    ),
                )

                signed.firstOrNull { it.curve == CURVE }
                    ?.signatures
                    ?.firstOrNull()
                    ?: error("Hot SDK returned no signature for $CURVE")
            }.fold(
                onSuccess = { it.right() },
                onFailure = { CollectiblesAccountError.DelegateSigningFailed(it).left() },
            )
        }
    }

    override suspend fun forget(accountId: AccountId) {
        withContext(dispatchers.io) {
            lock.withLock {
                val record = store.get(accountId)

                if (record != null) {
                    // Keystore entry goes first: while the record survives, a failed delete stays retryable.
                    runSuspendCatching { tangemHotSdk.delete(record.toHotWalletId()) }
                        .onSuccess { store.remove(accountId) }
                        .onFailure { TangemLogger.e("Failed to delete collectibles hot delegate", it) }
                }
            }
        }
    }

    private suspend fun deleteQuietly(hotWalletId: HotWalletId) {
        runSuspendCatching { tangemHotSdk.delete(hotWalletId) }
            .onFailure { TangemLogger.e("Failed to delete collectibles hot delegate", it) }
    }

    private suspend fun derivePublicKey(hotWalletId: HotWalletId): HotDelegatePublicKey {
        val response = tangemHotSdk.derivePublicKey(
            unlockHotWallet = UnlockHotWallet(walletId = hotWalletId, auth = HotAuth.NoAuth),
            request = DeriveWalletRequest(
                requests = listOf(DeriveWalletRequest.Request(curve = CURVE, paths = listOf(derivationPath))),
            ),
        )

        val publicKey = response.responses
            .firstOrNull { it.curve == CURVE }
            ?.publicKeys
            ?.get(derivationPath)
            ?.publicKey
            ?: error("Hot SDK returned no public key for $CURVE at $derivationPath")

        return HotDelegatePublicKey(publicKey.encodeBase58())
    }

    private fun CollectiblesHotDelegateRecord.toHotWalletId() = HotWalletId(
        value = hotWalletId,
        authType = HotWalletId.AuthType.valueOf(authType),
    )

    private fun CollectiblesHotDelegateRecord.toUnlock() = UnlockHotWallet(
        walletId = toHotWalletId(),
        auth = HotAuth.NoAuth,
    )

    private companion object {

        val CURVE = EllipticCurve.Ed25519Slip0010

        /** Mainnet path though the program is devnet: only the public key matters, and pinning it keeps it stable. */
        val derivationPath: DerivationPath = requireNotNull(
            Blockchain.Solana.derivationPath(DerivationStyle.V3),
        ) {
            "Solana must have a V3 derivation path"
        }
    }
}