package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.converter.PolymarketAuthorizeSessionKeyRequestConverter
import com.tangem.data.polymarket.converter.PolymarketRevokeSessionKeyRequestConverter
import com.tangem.data.polymarket.converter.PolymarketSessionKeyConverter
import com.tangem.data.polymarket.converter.PolymarketSessionSignersConverter
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketSessionKeyErrorResolver
import com.tangem.data.polymarket.signer.PolymarketL2HeaderBuilder
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobApi
import com.tangem.grow.datasource.polymarket.models.PolymarketSessionKeyDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSetSessionKeyEnabledRequest
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketSessionKeyRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val clobApi: PolymarketClobApi,
    private val sessionKeyErrorResolver: PolymarketSessionKeyErrorResolver,
    private val authErrorResolver: PolymarketAuthErrorResolver,
    private val l2HeaderBuilder: PolymarketL2HeaderBuilder,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketSessionKeyRepository {

    override suspend fun authorizeSessionKey(
        userWalletId: UserWalletId,
        authorization: PolymarketSessionKeyAuthorization,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.authorizeSessionKey(
                    walletId = userWalletId.stringValue,
                    request = PolymarketAuthorizeSessionKeyRequestConverter.convert(authorization),
                ).bind()
                response.toSessionKey()
            },
            onError = { sessionKeyErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun revokeSessionKey(
        userWalletId: UserWalletId,
        revocation: PolymarketSessionKeyRevocation,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.revokeSessionKey(
                    walletId = userWalletId.stringValue,
                    request = PolymarketRevokeSessionKeyRequestConverter.convert(revocation),
                ).bind()
                response.toSessionKey()
            },
            onError = { sessionKeyErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun setSessionKeyEnabled(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        isEnabled: Boolean,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.setSessionKeyEnabled(
                    walletId = userWalletId.stringValue,
                    sessionKeyAddress = sessionKeyAddress,
                    request = PolymarketSetSessionKeyEnabledRequest(isEnabled = isEnabled),
                ).bind()
                response.toSessionKey()
            },
            onError = { sessionKeyErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun getSessionSigners(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
    ): Either<PolymarketAuthError, List<PolymarketSessionSigner>> = withContext(dispatchers.io) {
        val headers = Either
            .catch {
                l2HeaderBuilder.build(
                    ownerAddress = ownerAddress,
                    credentials = credentials,
                    requestPath = SESSION_SIGNERS_SIGNED_PATH,
                )
            }
            .mapLeft { PolymarketAuthError.Unknown(httpCode = null, detail = it.message) }
            .getOrElse { return@withContext it.left() }

        safeApiCall(
            call = {
                val response = clobApi.getSessionSigners(headers).bind()
                Either.catch { PolymarketSessionSignersConverter.convert(response) }
                    .mapLeft { PolymarketAuthError.Unknown(httpCode = null, detail = it.message) }
            },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    private fun PolymarketSessionKeyDto.toSessionKey(): Either<PolymarketSessionKeyError, PolymarketSessionKey> =
        Either.catch { PolymarketSessionKeyConverter.convert(this) }
            .mapLeft { PolymarketSessionKeyError.Unknown(httpCode = null, reason = null, detail = it.message) }

    private companion object {
        const val SESSION_SIGNERS_SIGNED_PATH = "/v1/user/session-signers"
    }
}