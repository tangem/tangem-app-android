package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.getOrElse
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PredictionDepositAddressWriter
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.model.PolymarketBridgeAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Brings the funding requisites of an account in step with the bridge, and projects the day-one address onto the
 * account status.
 *
 * The two stores are written in this order on purpose: the caller may treat the requisites becoming available as
 * a signal that the status carries a confirmed address.
 */
class RefreshPredictionDepositRequisitesInteractor(
    private val bridgeRepository: PolymarketBridgeRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val requisitesStore: PredictionDepositRequisitesStore,
    private val depositAddressWriter: PredictionDepositAddressWriter,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
    ): Either<PolymarketBridgeError, PolymarketDepositRequisites> = either {
        val stored = ensureNotNull(deriveAddresses.stored(userWalletId = userWalletId)) {
            PolymarketBridgeError.Unavailable
        }

        val (assets, addresses) = try {
            coroutineScope {
                val assets = async { bridgeRepository.getSupportedAssets().orFail() }
                val addresses = async {
                    bridgeRepository.createDepositAddresses(
                        walletId = userWalletId.stringValue,
                        depositWalletAddress = stored.depositWalletAddress,
                    ).orFail()
                }
                assets.await() to addresses.await()
            }
        } catch (failure: BridgeCallFailed) {
            raise(failure.error)
        }

        val requisites = PolymarketDepositRequisites(networks = groupIntoNetworks(assets, addresses))

        depositAddressWriter.store(userWalletId = userWalletId, address = requisites.default?.address)
        requisitesStore.store(userWalletId = userWalletId, value = requisites)

        requisites
    }

    private fun groupIntoNetworks(
        assets: List<PolymarketBridgeAsset>,
        addresses: PolymarketBridgeAddresses,
    ): List<PolymarketDepositNetwork> = assets
        .groupBy { it.chainId }
        .mapNotNull { (chainId, rows) ->
            val family = PolymarketChainFamily.of(chainId) ?: return@mapNotNull null
            val address = addresses.of(family) ?: return@mapNotNull null

            PolymarketDepositNetwork(
                chainId = chainId,
                chainName = rows.first().chainName,
                family = family,
                address = address,
                tokens = rows.map(::toDepositToken),
            )
        }

    private fun toDepositToken(row: PolymarketBridgeAsset) = PolymarketDepositToken(
        symbol = row.tokenSymbol,
        address = row.tokenAddress,
        decimals = row.decimals,
        minCheckoutUsd = row.minCheckoutUsd,
    )

    private fun <T> Either<PolymarketBridgeError, T>.orFail(): T = getOrElse { throw BridgeCallFailed(error = it) }

    private fun PolymarketBridgeAddresses.of(family: PolymarketChainFamily): String? = when (family) {
        PolymarketChainFamily.EVM -> evm
        PolymarketChainFamily.SVM -> svm
        PolymarketChainFamily.BTC -> btc
        PolymarketChainFamily.TRON -> tron
    }

    private class BridgeCallFailed(val error: PolymarketBridgeError) : Exception()
}