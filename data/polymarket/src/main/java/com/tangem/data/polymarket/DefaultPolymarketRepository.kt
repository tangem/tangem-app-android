package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.polymarket.converter.PolymarketApiKeyConverter
import com.tangem.data.polymarket.converter.PolymarketEventConverter
import com.tangem.data.polymarket.converter.PolymarketWalletConverter
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketWalletErrorResolver
import com.tangem.data.polymarket.signer.PolymarketL2HeaderBuilder
import com.tangem.datasource.api.polymarket.PolymarketApi
import com.tangem.datasource.api.polymarket.clob.PolymarketClobApi
import com.tangem.datasource.api.polymarket.geo.PolymarketGeoApi
import com.tangem.datasource.api.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.datasource.api.polymarket.relayer.PolymarketRelayerApi
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
internal class DefaultPolymarketRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val geoApi: PolymarketGeoApi,
    private val relayerApi: PolymarketRelayerApi,
    private val clobApi: PolymarketClobApi,
    private val eventConverter: PolymarketEventConverter,
    private val walletConverter: PolymarketWalletConverter,
    private val walletErrorResolver: PolymarketWalletErrorResolver,
    private val authErrorResolver: PolymarketAuthErrorResolver,
    private val l2HeaderBuilder: PolymarketL2HeaderBuilder,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketRepository {

    override suspend fun getCategories(): Either<DataError, List<PolymarketCategory>> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.getCategories(locale = null).bind().categories
                    .map { PolymarketCategory(id = it.id, label = it.label, iconUrl = it.icon) }
                    .right()
            },
            onError = { DataError.NetworkError.NoInternetConnection.left() },
        )
    }

    override suspend fun getEvents(category: Int?): Either<DataError, List<PolymarketEvent>> =
        withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    polymarketApi.getEvents(category = category, limit = DEFAULT_LIMIT, cursor = null)
                        .bind().events.map(eventConverter::convert).right()
                },
                onError = { DataError.NetworkError.NoInternetConnection.left() },
            )
        }

    override suspend fun getWalletStatus(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletState> =
        withContext(dispatchers.io) {
            safeApiCall(
                call = { walletConverter.toState(polymarketApi.getWalletStatus(ownerAddress).bind()).right() },
                onError = { walletErrorResolver.resolve(it).left() },
            )
        }

    override suspend fun deployWallet(
        ownerAddress: String,
        userWalletId: UserWalletId,
        depositWalletAddress: String,
    ): Either<PolymarketWalletError, PolymarketWalletStatus> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.deployWallet(
                    PolymarketWalletDeployRequest(
                        ownerAddress = ownerAddress,
                        walletId = userWalletId.stringValue,
                        depositWalletAddress = depositWalletAddress,
                    ),
                ).bind()
                PolymarketWalletStatus.fromRaw(response.status).right()
            },
            onError = { walletErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun submitApprovals(
        batch: PolymarketApprovalsBatch,
    ): Either<PolymarketWalletError, PolymarketWalletStatus> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.submitApprovals(walletConverter.toRequest(batch)).bind()
                PolymarketWalletStatus.fromRaw(response.status).right()
            },
            onError = { walletErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun checkGeoblock(): Either<DataError, Boolean> = withContext(dispatchers.io) {
        safeApiCall(
            call = { geoApi.getGeoblock().bind().blocked.right() },
            onError = { DataError.NetworkError.NoInternetConnection.left() },
        )
    }

    override suspend fun getRelayerNonce(ownerAddress: String): Either<DataError, BigInteger> =
        withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    relayerApi.getNonce(address = ownerAddress, type = WALLET_NONCE_TYPE).bind().nonce
                        .toBigIntegerOrNull()?.right()
                        ?: DataError.NetworkError.NoInternetConnection.left()
                },
                onError = { DataError.NetworkError.NoInternetConnection.left() },
            )
        }

    override suspend fun deriveApiCredentials(
        headers: PolymarketL1Headers,
    ): Either<PolymarketAuthError, PolymarketApiCredentials> = withContext(dispatchers.io) {
        safeApiCall(
            call = { PolymarketApiKeyConverter.convert(clobApi.deriveApiKey(headers.toMap()).bind()).right() },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun createApiCredentials(
        headers: PolymarketL1Headers,
    ): Either<PolymarketAuthError, PolymarketApiCredentials> = withContext(dispatchers.io) {
        safeApiCall(
            call = { PolymarketApiKeyConverter.convert(clobApi.createApiKey(headers.toMap()).bind()).right() },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun syncBalanceAllowance(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
    ): Either<PolymarketAuthError, Unit> = withContext(dispatchers.io) {
        val headers = runCatching {
            l2HeaderBuilder.build(
                ownerAddress = ownerAddress,
                credentials = credentials,
                requestPath = BALANCE_ALLOWANCE_SIGNED_PATH,
            )
        }.getOrElse { return@withContext PolymarketAuthError.Unknown(httpCode = null, detail = it.message).left() }

        safeApiCallWithTimeout(
            timeoutMillis = SYNC_BALANCE_ALLOWANCE_TIMEOUT,
            call = {
                clobApi.updateBalanceAllowance(
                    headers = headers,
                    assetType = ASSET_TYPE_COLLATERAL,
                    signatureType = SIGNATURE_TYPE_DEPOSIT_WALLET,
                ).bind().right()
            },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    private companion object {

        const val DEFAULT_LIMIT = 20
        const val WALLET_NONCE_TYPE = "WALLET"
        const val ASSET_TYPE_COLLATERAL = "COLLATERAL"

        /** Polymarket's signature type for a contract-owned deposit wallet (ERC-1271 verification), not a plain EOA. */
        const val SIGNATURE_TYPE_DEPOSIT_WALLET = 3

        val SYNC_BALANCE_ALLOWANCE_TIMEOUT = 5.seconds

        /** Signed by the HMAC without the query string, unlike the relative path Retrofit resolves. */
        const val BALANCE_ALLOWANCE_SIGNED_PATH = "/balance-allowance/update"
    }
}