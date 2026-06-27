package com.tangem.data.txhistory.repository.factory

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Resolves a portfolio [CryptoCurrency] for every express asset (network id + contract address) referenced by a
 * batch of exchange/onramp entities.
 *
 * Strategy: read every account of every wallet ONCE (via [MultiAccountListSupplier]) and match each express asset
 * against the flattened portfolio currencies by network id + contract address. When nothing matches — notably
 * tokens that are not present in any portfolio — a coin is built for the asset's network as a fallback (for now).
 */
internal class ExpressTransactionAssetFactory @Inject constructor(
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
    excludedBlockchains: ExcludedBlockchains,
) {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    /**
     * Builds a `assetId -> resolved currency` map covering both legs of every swap and the to-leg of every onramp.
     * Entries whose currency could not be resolved at all (no match and no fallback coin) are omitted.
     */
    suspend fun create(
        userWalletId: UserWalletId,
        outgoingSwaps: List<ExpressExchangeEntity>,
        incomingSwaps: List<ExpressExchangeEntity>,
        onramps: List<ExpressOnrampEntity>,
    ): Map<ExpressAsset.ID, CryptoCurrency> {
        val assetIds = buildSet {
            (outgoingSwaps + incomingSwaps).forEach { entity ->
                add(entity.from.toAssetId())
                add(entity.to.toAssetId())
            }
            onramps.forEach { entity -> add(entity.to.toAssetId()) }
        }
        if (assetIds.isEmpty()) return emptyMap()

        val portfolioCurrencies = multiAccountListSupplier.invoke()
            .first()
            .flatMap { accountList -> accountList.flattenCurrencies() }

        val userWallet = userWalletsListRepository.userWalletsSync()
            .firstOrNull { it.walletId == userWalletId }

        return buildMap {
            assetIds.forEach { id ->
                val currency = portfolioCurrencies.findMatching(id) ?: createFallbackCoin(id, userWallet)
                if (currency != null) put(id, currency)
            }
        }
    }

    private fun List<CryptoCurrency>.findMatching(id: ExpressAsset.ID): CryptoCurrency? {
        val isCoin = id.contractAddress == ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE
        return firstOrNull { currency ->
            currency.network.rawId == id.networkId &&
                if (isCoin) {
                    currency is CryptoCurrency.Coin
                } else {
                    currency is CryptoCurrency.Token &&
                        currency.contractAddress.equals(id.contractAddress, ignoreCase = true)
                }
        }
    }

    // TODO txHistory: tokens that are not in any portfolio cannot be resolved yet — fall back to a coin on the asset's
    //  network.
    private fun createFallbackCoin(id: ExpressAsset.ID, userWallet: UserWallet?): CryptoCurrency.Coin? {
        userWallet ?: return null
        return cryptoCurrencyFactory.createCoin(
            networkId = id.networkId,
            extraDerivationPath = null,
            userWallet = userWallet,
        )
    }
}

internal fun ExpressExchangeEntity.AssetEmbedded.toAssetId(): ExpressAsset.ID =
    ExpressAsset.ID(networkId = network, contractAddress = contractAddress)

internal fun ExpressOnrampEntity.AssetEmbedded.toAssetId(): ExpressAsset.ID =
    ExpressAsset.ID(networkId = network, contractAddress = contractAddress)