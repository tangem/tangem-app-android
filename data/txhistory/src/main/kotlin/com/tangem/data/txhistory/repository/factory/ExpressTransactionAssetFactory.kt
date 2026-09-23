package com.tangem.data.txhistory.repository.factory

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.TokenInfoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Resolves a [CryptoCurrency] for every express asset (network id + contract address) referenced by a batch of
 * exchange/onramp entities.
 */
internal class ExpressTransactionAssetFactory @Inject constructor(
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val tokenInfoRepository: TokenInfoRepository,
    excludedBlockchains: ExcludedBlockchains,
) {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    /**
     * Builds a `assetId -> resolved currency` map covering both legs of every swap and the to-leg of every onramp.
     * Entries whose currency could not be resolved at all are omitted.
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
                entity.toRefundAssetId()?.let { add(it) }
            }
            onramps.forEach { entity -> add(entity.to.toAssetId()) }
        }
        if (assetIds.isEmpty()) return emptyMap()

        val portfolioCurrencies = multiAccountListSupplier.invoke()
            .first()
            .flatMap { accountList -> accountList.flattenCurrencies() }

        val userWallet = userWalletsListRepository.userWalletsSync()
            .firstOrNull { it.walletId == userWalletId }

        val cachedTokens = loadCachedTokens(userWalletId, assetIds)

        return buildMap {
            assetIds.forEach { id ->
                val currency = portfolioCurrencies.findMatching(id) ?: resolveUnmatched(id, cachedTokens, userWallet)
                if (currency != null) put(id, currency)
            }
        }
    }

    private fun resolveUnmatched(
        id: ExpressAsset.ID,
        cachedTokens: Map<ExpressAsset.ID, CryptoCurrency.Token>,
        userWallet: UserWallet?,
    ): CryptoCurrency? {
        return if (id.contractAddress == ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE) {
            createCoin(id, userWallet)
        } else {
            cachedTokens[id]
        }
    }

    private suspend fun loadCachedTokens(
        userWalletId: UserWalletId,
        assetIds: Set<ExpressAsset.ID>,
    ): Map<ExpressAsset.ID, CryptoCurrency.Token> = tokenInfoRepository.getCachedTokens(userWalletId, assetIds)

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

    private fun createCoin(id: ExpressAsset.ID, userWallet: UserWallet?): CryptoCurrency.Coin? {
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

internal fun ExpressExchangeEntity.toRefundAssetId(): ExpressAsset.ID? {
    val refundNetwork = refundNetwork
    val refundContractAddress = refundContractAddress
    return if (refundNetwork != null && refundContractAddress != null) {
        ExpressAsset.ID(refundNetwork, refundContractAddress)
    } else {
        null
    }
}

internal fun ExpressOnrampEntity.AssetEmbedded.toAssetId(): ExpressAsset.ID =
    ExpressAsset.ID(networkId = network, contractAddress = contractAddress)