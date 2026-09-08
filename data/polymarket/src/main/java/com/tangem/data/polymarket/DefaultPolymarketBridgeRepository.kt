package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.common.api.safeApiCall
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.grow.datasource.polymarket.bridge.PolymarketBridgeApi
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositRequest
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketSupportedAssetDto
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketBridgeRepository @Inject constructor(
    private val bridgeApi: PolymarketBridgeApi,
    private val polymarketRepository: PolymarketRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketBridgeRepository {

    private val supportedAssetsCache = RuntimeSharedStore<List<PolymarketBridgeAsset>>()

    override suspend fun getSupportedAssets(): Either<PolymarketBridgeError, List<PolymarketBridgeAsset>> =
        withContext(dispatchers.io) {
            supportedAssetsCache.getSyncOrNull()?.let { return@withContext it.right() }

            safeApiCall(
                call = {
                    bridgeApi.getSupportedAssets().bind().supportedAssets
                        .map(::toAsset)
                        .also { supportedAssetsCache.store(it) }
                        .right()
                },
                onError = { PolymarketBridgeError.Network.left() },
            )
        }

    private fun toAsset(dto: PolymarketSupportedAssetDto) = PolymarketBridgeAsset(
        chainId = dto.chainId,
        chainName = dto.chainName,
        tokenSymbol = dto.token.symbol,
        tokenAddress = dto.token.address,
        decimals = dto.token.decimals,
        minCheckoutUsd = dto.minCheckoutUsd,
    )

    override suspend fun createDepositAddresses(
        walletId: String,
        depositWalletAddress: String,
    ): Either<PolymarketBridgeError, PolymarketBridgeAddresses> = withContext(dispatchers.io) {
        val builderCode = polymarketRepository.getBuilderCode(walletId = walletId).getOrNull()
            ?: return@withContext PolymarketBridgeError.Unavailable.left()

        safeApiCall(
            call = {
                bridgeApi.createDepositAddresses(
                    builderCode = builderCode,
                    body = PolymarketDepositRequest(address = depositWalletAddress),
                ).bind()
                    .let { response ->
                        PolymarketBridgeAddresses(
                            evm = response.address.evm,
                            svm = response.address.svm,
                            btc = response.address.btc,
                            tron = response.address.tron,
                        )
                    }
                    .right()
            },
            onError = { PolymarketBridgeError.Network.left() },
        )
    }
}