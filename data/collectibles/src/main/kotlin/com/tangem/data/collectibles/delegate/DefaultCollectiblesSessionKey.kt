package com.tangem.data.collectibles.delegate

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
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
internal class DefaultCollectiblesSessionKey @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val accountStore: CollectiblesAccountStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesSessionKey {

    private val lock = Mutex()

    override suspend fun provision(accountId: AccountId): Either<CollectiblesAccountError, SessionKeyPublicKey> {
        return withContext(dispatchers.io) {
            lock.withLock {
                runSuspendCatching {
                    val stored = storedSessionKey(accountId)

                    if (stored != null) {
                        TangemLogger.d("sessionKey: is a hot session key stored on the phone -> Yes")
                        derivePublicKey(stored.hotWalletId)
                    } else {
                        TangemLogger.d("sessionKey: is a hot session key stored on the phone -> No")
                        create(accountId)
                    }
                }.fold(
                    onSuccess = { it.right() },
                    onFailure = { error ->
                        TangemLogger.d("sessionKey: provision failed, error=$error")
                        CollectiblesAccountError.SessionKeyCreationFailed(error).left()
                    },
                )
            }
        }
    }

    override suspend fun getPublicKeyOrNull(accountId: AccountId): SessionKeyPublicKey? {
        return withContext(dispatchers.io) {
            val stored = storedSessionKey(accountId) ?: return@withContext null

            runSuspendCatching { derivePublicKey(stored.hotWalletId) }
                .onFailure { TangemLogger.e("Failed to read the collectibles session key", it) }
                .getOrNull()
        }
    }

    private suspend fun create(accountId: AccountId): SessionKeyPublicKey {
        val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, MnemonicType.Words12)

        return runSuspendCatching {
            val publicKey = derivePublicKey(hotWalletId)

            accountStore.storeSessionKey(userWalletId = accountId.userWalletId, hotWalletId = hotWalletId.value)

            publicKey
        }
            // A keystore entry no record points at is unreachable: drop it before reporting the failure.
            .onFailure { deleteQuietly(hotWalletId) }
            .getOrThrow()
    }

    override suspend fun sign(accountId: AccountId, message: ByteArray): Either<CollectiblesAccountError, ByteArray> {
        return withContext(dispatchers.io) {
            val stored = storedSessionKey(accountId)
                ?: return@withContext CollectiblesAccountError.SessionKeyNotProvisioned.left()

            runSuspendCatching {
                val signed = tangemHotSdk.signHashes(
                    unlockHotWallet = UnlockHotWallet(
                        walletId = stored.hotWalletId,
                        auth = HotAuth.NoAuth,
                    ),
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
                onFailure = { CollectiblesAccountError.SessionKeySigningFailed(it).left() },
            )
        }
    }

    override suspend fun forget(accountId: AccountId) {
        withContext(dispatchers.io) {
            lock.withLock {
                val stored = storedSessionKey(accountId)

                if (stored != null) {
                    runSuspendCatching {
                        tangemHotSdk.delete(stored.hotWalletId)
                        accountStore.removeSessionKey(accountId.userWalletId)
                    }.onFailure { TangemLogger.e("Failed to delete collectibles session key", it) }
                }
            }
        }
    }

    private suspend fun deleteQuietly(hotWalletId: HotWalletId) {
        runSuspendCatching { tangemHotSdk.delete(hotWalletId) }
            .onFailure { TangemLogger.e("Failed to delete collectibles session key", it) }
    }

    private suspend fun derivePublicKey(hotWalletId: HotWalletId): SessionKeyPublicKey {
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

        return SessionKeyPublicKey(publicKey.encodeBase58())
    }

    private suspend fun storedSessionKey(accountId: AccountId): CollectiblesHotWalletId? {
        return accountStore.getSessionKey(accountId.userWalletId)
            ?.let { CollectiblesHotWalletId(accountId = accountId, keystoreId = it) }
    }

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