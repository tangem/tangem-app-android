package com.tangem.data.polymarket.sessionkey

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.withError
import com.tangem.common.extensions.toHexString
import com.tangem.data.polymarket.signing.PolymarketDigestFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketSessionCredentialsError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionCredentialsProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class DefaultPolymarketSessionCredentialsProvider @Inject constructor(
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val polymarketRepository: PolymarketRepository,
    private val digestFactory: PolymarketDigestFactory,
) : PolymarketSessionCredentialsProvider {

    private val identities = ConcurrentHashMap<UserWalletId, PolymarketClobIdentity.Session>()
    private val mutexes = ConcurrentHashMap<UserWalletId, Mutex>()

    override suspend fun get(
        userWalletId: UserWalletId,
    ): Either<PolymarketSessionCredentialsError, PolymarketClobIdentity.Session> =
        mutexes.getOrPut(userWalletId) { Mutex() }.withLock {
            either {
                val address = ensureNotNull(sessionKeyStore.heldAddress(userWalletId)) {
                    PolymarketSessionCredentialsError.Key(PolymarketSessionKeyError.KeyMissing)
                }

                identities[userWalletId]?.takeIf { it.address == address }?.let { return@either it }

                val headers = withError(PolymarketSessionCredentialsError::Key) {
                    signClobAuth(userWalletId = userWalletId, address = address).bind()
                }
                val credentials = withError(PolymarketSessionCredentialsError::Auth) {
                    derivedOrCreated(headers).bind()
                }

                PolymarketClobIdentity.Session(address = address, credentials = credentials)
                    .also { identities[userWalletId] = it }
            }.onLeft { TangemLogger.e("Failed to obtain the session CLOB credentials of $userWalletId: $it") }
        }

    override fun invalidate(userWalletId: UserWalletId, identity: PolymarketClobIdentity.Session) {
        identities.remove(userWalletId, identity)
    }

    private suspend fun signClobAuth(
        userWalletId: UserWalletId,
        address: String,
    ): Either<PolymarketSessionKeyError, PolymarketL1Headers> {
        val clobAuth = PolymarketClobAuthData(timestamp = (System.currentTimeMillis() / MILLIS_IN_SECOND).toString())
        val digest = digestFactory.clobAuth(ownerAddress = address, data = clobAuth)

        return sessionKeyStore.sign(userWalletId = userWalletId, address = address, digest = digest).map { signature ->
            PolymarketL1Headers(
                address = address,
                signature = HEX_PREFIX + signature.toHexString().lowercase(),
                timestamp = clobAuth.timestamp,
                nonce = clobAuth.nonce,
            )
        }
    }

    private suspend fun derivedOrCreated(
        headers: PolymarketL1Headers,
    ): Either<PolymarketAuthError, PolymarketApiCredentials> {
        val derived = polymarketRepository.deriveApiCredentials(headers = headers)
        val refusal = derived.leftOrNull() ?: return derived

        return if (refusal.isClientRefusal()) polymarketRepository.createApiCredentials(headers = headers) else derived
    }

    private fun PolymarketAuthError.isClientRefusal(): Boolean = when (this) {
        PolymarketAuthError.KeyNotFound,
        PolymarketAuthError.InvalidSignature,
        -> true
        is PolymarketAuthError.Unknown -> httpCode in CLIENT_ERROR_CODES
        PolymarketAuthError.RateLimited,
        PolymarketAuthError.Network,
        -> false
    }

    private companion object {
        const val HEX_PREFIX = "0x"
        const val MILLIS_IN_SECOND = 1_000L
        val CLIENT_ERROR_CODES = 400..499
    }
}