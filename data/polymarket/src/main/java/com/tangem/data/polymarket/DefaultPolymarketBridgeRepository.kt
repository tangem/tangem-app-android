package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.api.safeApiCall
import com.tangem.domain.polymarket.PolymarketBridgeAssetsStore
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.grow.datasource.polymarket.bridge.PolymarketBridgeApi
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositRequest
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketSupportedAssetDto
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketWithdrawAddressRequest
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketBridgeRepository @Inject constructor(
    private val bridgeApi: PolymarketBridgeApi,
    private val polymarketRepository: PolymarketRepository,
    private val assetsStore: PolymarketBridgeAssetsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketBridgeRepository {

    override suspend fun getSupportedAssets(): Either<PolymarketBridgeError, List<PolymarketBridgeAsset>> =
        withContext(dispatchers.io) {
            assetsStore.getSyncOrNull()?.let { return@withContext it.right() }

            safeApiCall(
                call = {
                    bridgeApi.getSupportedAssets().bind().supportedAssets
                        .map(::toAsset)
                        .also { assetsStore.store(it) }
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

    override suspend fun createWithdrawAddress(
        walletId: String,
        depositWalletAddress: String,
        toChainId: String,
        toTokenAddress: String,
        recipientAddress: String,
    ): Either<PolymarketBridgeError, String> = withContext(dispatchers.io) {
        val builderCode = polymarketRepository.getBuilderCode(walletId = walletId).getOrNull()
            ?: return@withContext PolymarketBridgeError.Unavailable.left()

        safeApiCall(
            call = {
                val response = bridgeApi.createWithdrawAddress(
                    builderCode = builderCode,
                    body = PolymarketWithdrawAddressRequest(
                        address = depositWalletAddress,
                        toChainId = toChainId,
                        toTokenAddress = toTokenAddress,
                        recipientAddr = recipientAddress,
                    ),
                ).bind()

                (response.nested?.evm ?: response.evm)?.right()
                    ?: PolymarketBridgeError.Rejected(detail = MISSING_EVM_DETAIL).left()
            },
            onError = { PolymarketBridgeError.Network.left() },
        )
    }

    private companion object {
        const val MISSING_EVM_DETAIL = "The bridge answered without an evm address"
    }
}