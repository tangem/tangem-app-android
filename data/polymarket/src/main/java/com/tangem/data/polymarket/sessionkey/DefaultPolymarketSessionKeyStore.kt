package com.tangem.data.polymarket.sessionkey

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class DefaultPolymarketSessionKeyStore(
    private val tangemHotSdk: TangemHotSdk,
    private val secureStorage: SecureStorage,
    private val addressFactory: PolymarketAddressFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketSessionKeyStore {

    private val mutexes = ConcurrentHashMap<String, Mutex>()
    private val seedKeys = ConcurrentHashMap<String, ExtendedPublicKey>()

    override suspend fun heldAddress(userWalletId: UserWalletId): String? = withWalletLock(userWalletId) {
        readAddress(userWalletId)
    }

    override suspend fun getOrCreate(userWalletId: UserWalletId): Either<PolymarketSessionKeyError, String> =
        withWalletLock(userWalletId) {
            runCatchingUnlessCancelled {
                val hotWalletId = storedHotWalletId(userWalletId)
                if (hotWalletId != null) address(seedKey(hotWalletId)) else create(userWalletId)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { error ->
                    TangemLogger.e("Failed to read or create the session key of $userWalletId", error)
                    PolymarketSessionKeyError.Failure(error).left()
                },
            )
        }

    override suspend fun sign(
        userWalletId: UserWalletId,
        address: String,
        digest: ByteArray,
    ): Either<PolymarketSessionKeyError, ByteArray> = withWalletLock(userWalletId) {
        if (digest.size != DIGEST_SIZE) return@withWalletLock PolymarketSessionKeyError.InvalidDigest.left()

        runCatchingUnlessCancelled {
            val hotWalletId = storedHotWalletId(userWalletId) ?: return@runCatchingUnlessCancelled null
            val heldAddress = address(seedKey(hotWalletId))
            if (!heldAddress.equals(address, ignoreCase = true)) return@runCatchingUnlessCancelled null
            signDigest(hotWalletId, digest)
        }.fold(
            onSuccess = { it?.right() ?: PolymarketSessionKeyError.KeyMissing.left() },
            onFailure = { error ->
                TangemLogger.e("Failed to sign with the session key of $userWalletId", error)
                PolymarketSessionKeyError.Failure(error).left()
            },
        )
    }

    override suspend fun delete(userWalletId: UserWalletId): Either<PolymarketSessionKeyError, Unit> =
        withWalletLock(userWalletId) {
            runCatchingUnlessCancelled {
                val hotWalletId = storedHotWalletId(userWalletId)
                secureStorage.delete(storageKey(userWalletId))
                hotWalletId
            }.fold(
                onSuccess = { hotWalletId ->
                    hotWalletId?.let { deleteQuietly(it) }
                    Unit.right()
                },
                onFailure = { PolymarketSessionKeyError.Failure(it).left() },
            )
        }

    private suspend fun readAddress(userWalletId: UserWalletId): String? = runCatchingUnlessCancelled {
        storedHotWalletId(userWalletId)?.let { address(seedKey(it)) }
    }
        .onFailure { TangemLogger.e("Failed to read the session key of $userWalletId", it) }
        .getOrNull()

    private suspend fun create(userWalletId: UserWalletId): String {
        val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, MnemonicType.Words12)

        var isStored = false
        try {
            val address = address(seedKey(hotWalletId))
            secureStorage.store(storageKey(userWalletId), hotWalletId.value)
            isStored = true
            return address
        } finally {
            if (!isStored) withContext(NonCancellable) { deleteQuietly(hotWalletId) }
        }
    }

    private suspend fun signDigest(hotWalletId: HotWalletId, digest: ByteArray): ByteArray {
        val seedKey = seedKey(hotWalletId)
        val signature = tangemHotSdk
            .signHashes(
                unlockHotWallet = unlock(hotWalletId),
                dataToSign = listOf(DataToSign(curve = CURVE, derivationPath = null, hashes = listOf(digest))),
            )
            .firstOrNull { it.curve == CURVE }
            ?.signatures
            ?.firstOrNull()
            ?: error("Hot SDK returned no $CURVE signature")

        return UnmarshalHelper
            .unmarshalSignatureExtended(
                signature = signature,
                hash = digest,
                publicKey = seedKey.publicKey.toDecompressedPublicKey(),
            )
            .asRSVLegacyEVM()
    }

    private suspend fun seedKey(hotWalletId: HotWalletId): ExtendedPublicKey {
        seedKeys[hotWalletId.value]?.let { return it }

        val seedKey = tangemHotSdk
            .derivePublicKey(
                unlockHotWallet = unlock(hotWalletId),
                request = DeriveWalletRequest(
                    requests = listOf(DeriveWalletRequest.Request(curve = CURVE, paths = emptyList())),
                ),
            )
            .responses
            .firstOrNull { it.curve == CURVE }
            ?.seedKey
            ?: error("Hot SDK returned no $CURVE master key")

        seedKeys[hotWalletId.value] = seedKey
        return seedKey
    }

    private fun address(seedKey: ExtendedPublicKey): String = addressFactory.createAddress(seedKey).lowercase()

    private suspend fun deleteQuietly(hotWalletId: HotWalletId) {
        runCatchingUnlessCancelled {
            tangemHotSdk.delete(hotWalletId)
            seedKeys.remove(hotWalletId.value)
        }.onFailure { TangemLogger.e("Failed to delete the hot wallet of a session key", it) }
    }

    private fun storedHotWalletId(userWalletId: UserWalletId): HotWalletId? = secureStorage
        .getAsString(storageKey(userWalletId))
        ?.let { HotWalletId(value = it, authType = HotWalletId.AuthType.NoPassword) }

    private fun unlock(hotWalletId: HotWalletId) = UnlockHotWallet(walletId = hotWalletId, auth = HotAuth.NoAuth)

    private suspend fun <T> withWalletLock(userWalletId: UserWalletId, action: suspend () -> T): T =
        withContext(dispatchers.io) {
            mutexes.getOrPut(storageKey(userWalletId)) { Mutex() }.withLock { action() }
        }

    private suspend inline fun <R> runCatchingUnlessCancelled(block: () -> R): Result<R> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        currentCoroutineContext().ensureActive()
        Result.failure(e)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    private fun storageKey(userWalletId: UserWalletId): String = KEY_PREFIX + userWalletId.stringValue.lowercase()

    private companion object {
        const val KEY_PREFIX = "polymarket_session_key_wallet_id_"
        const val DIGEST_SIZE = 32
        val CURVE = EllipticCurve.Secp256k1
    }
}