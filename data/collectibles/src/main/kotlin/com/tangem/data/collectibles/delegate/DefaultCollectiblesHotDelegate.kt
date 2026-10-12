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
    private val accountStore: CollectiblesAccountStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesHotDelegate {

    private val lock = Mutex()

    override suspend fun provision(accountId: AccountId): Either<CollectiblesAccountError, HotDelegatePublicKey> {
        return withContext(dispatchers.io) {
            lock.withLock {
                runSuspendCatching {
                    val stored = storedDelegate(accountId)

                    if (stored != null) {
                        TangemLogger.d("hotDelegate: is a hot session key stored on the phone -> Yes")
                        derivePublicKey(stored.hotWalletId)
                    } else {
                        TangemLogger.d("hotDelegate: is a hot session key stored on the phone -> No")
                        create(accountId)
                    }
                }.fold(
                    onSuccess = { it.right() },
                    onFailure = { error ->
                        TangemLogger.d("hotDelegate: provision failed, error=$error")
                        CollectiblesAccountError.DelegateKeyCreationFailed(error).left()
                    },
                )
            }
        }
    }

    private suspend fun create(accountId: AccountId): HotDelegatePublicKey {
        val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, MnemonicType.Words12)

        return runSuspendCatching {
            val publicKey = derivePublicKey(hotWalletId)

            accountStore.storeDelegate(userWalletId = accountId.userWalletId, hotWalletId = hotWalletId.value)

            publicKey
        }
            // A keystore entry no record points at is unreachable: drop it before reporting the failure.
            .onFailure { deleteQuietly(hotWalletId) }
            .getOrThrow()
    }

    override suspend fun sign(accountId: AccountId, message: ByteArray): Either<CollectiblesAccountError, ByteArray> {
        return withContext(dispatchers.io) {
            val delegate = storedDelegate(accountId)
                ?: return@withContext CollectiblesAccountError.DelegateNotProvisioned.left()

            runSuspendCatching {
                val signed = tangemHotSdk.signHashes(
                    unlockHotWallet = UnlockHotWallet(
                        walletId = delegate.hotWalletId,
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
                onFailure = { CollectiblesAccountError.DelegateSigningFailed(it).left() },
            )
        }
    }

    override suspend fun forget(accountId: AccountId) {
        withContext(dispatchers.io) {
            lock.withLock {
                val delegate = storedDelegate(accountId)

                if (delegate != null) {
                    runSuspendCatching {
                        tangemHotSdk.delete(delegate.hotWalletId)
                        accountStore.removeDelegate(accountId.userWalletId)
                    }.onFailure { TangemLogger.e("Failed to delete collectibles hot delegate", it) }
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

    private suspend fun storedDelegate(accountId: AccountId): CollectiblesHotWalletId? {
        return accountStore.getDelegate(accountId.userWalletId)
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