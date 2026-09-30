package com.tangem.data.txhistory.repository.factory

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.TokenInfoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Identifies one leg of an express deal: the asset it moved plus the user's own address on that side.
 *
 * The asset alone does not identify the portfolio currency behind the leg — two accounts of one wallet can hold the
 * same asset on different derivations (a custom-derivation copy next to the regular one), and they are separate
 * [CryptoCurrency] instances differing in derivation path, icon and custom flag. The address is what tells them apart.
 *
 * @property address the user's own address of that leg — the pay-in source, the payout destination or the refund
 * destination. `null` when the deal carries none (no refund address); an address outside the user's portfolios (a
 * send-and-swap payout) simply matches nothing.
 */
internal data class ExpressLegKey(val assetId: ExpressAsset.ID, val address: String?)

/**
 * Resolves a [CryptoCurrency] for every leg ([ExpressLegKey]) referenced by a batch of exchange/onramp entities.
 */
internal class ExpressTransactionAssetFactory @Inject constructor(
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val tokenInfoRepository: TokenInfoRepository,
    excludedBlockchains: ExcludedBlockchains,
) {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    /**
     * Builds a `legKey -> resolved currency` map covering both legs of every swap and the to-leg of every onramp.
     * Entries whose currency could not be resolved at all are omitted.
     */
    suspend fun create(
        userWalletId: UserWalletId,
        outgoingSwaps: List<ExpressExchangeEntity>,
        incomingSwaps: List<ExpressExchangeEntity>,
        onramps: List<ExpressOnrampEntity>,
    ): Map<ExpressLegKey, CryptoCurrency> {
        val legKeys = buildSet {
            (outgoingSwaps + incomingSwaps).forEach { entity ->
                add(entity.fromLegKey())
                add(entity.toLegKey())
                entity.refundLegKey()?.let(::add)
            }
            onramps.forEach { entity -> add(entity.toLegKey()) }
        }
        if (legKeys.isEmpty()) return emptyMap()

        val portfolioStatuses = multiAccountStatusListSupplier()
            .first()
            .flatMap { accountStatusList -> accountStatusList.flattenPersonalCurrencies() }

        val userWallet = userWalletsListRepository.userWalletsSync()
            .firstOrNull { it.walletId == userWalletId }

        val cachedTokens = loadCachedTokens(userWalletId, legKeys.mapTo(mutableSetOf(), ExpressLegKey::assetId))

        return buildMap {
            legKeys.forEach { key ->
                val currency = portfolioStatuses.findHolding(key)
                    ?: portfolioStatuses.findMatching(key.assetId)
                    ?: resolveUnmatched(key.assetId, cachedTokens, userWallet)
                if (currency != null) put(key, currency)
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

    /**
     * The portfolio currency the leg actually sat on: the asset matches **and** the account holds it at the leg's
     * address. This is what separates two same-asset copies living on different derivations.
     */
    private fun List<CryptoCurrencyStatus>.findHolding(key: ExpressLegKey): CryptoCurrency? {
        val address = key.address ?: return null
        return firstOrNull { status ->
            status.currency.matches(key.assetId) && status.value.networkAddress.holds(address)
        }?.currency
    }

    /**
     * Fallback for a leg whose address resolves to no portfolio: an address the user does not own (a send-and-swap
     * payout) or one not loaded yet. Any copy of the asset then carries the name, symbol and icon to show.
     */
    private fun List<CryptoCurrencyStatus>.findMatching(id: ExpressAsset.ID): CryptoCurrency? =
        firstOrNull { status -> status.currency.matches(id) }?.currency

    private fun CryptoCurrency.matches(id: ExpressAsset.ID): Boolean {
        if (network.rawId != id.networkId) return false
        return if (id.contractAddress == ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE) {
            this is CryptoCurrency.Coin
        } else {
            this is CryptoCurrency.Token && contractAddress.equals(id.contractAddress, ignoreCase = true)
        }
    }

    /**
     * Every address of the network, not just the default one — a chain with several address formats (Bitcoin legacy /
     * segwit, Cardano) reports whichever one the deal used. Compared case-insensitively: the wallet derives EVM
     * addresses ERC-55 checksummed while the express backend keeps whatever casing the provider reported.
     */
    private fun NetworkAddress?.holds(address: String): Boolean =
        this?.availableAddresses.orEmpty().any { it.value.equals(address, ignoreCase = true) }

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

/** Pay-in leg: the assets left the user's own [ExpressExchangeEntity.fromAddress]. */
internal fun ExpressExchangeEntity.fromLegKey(): ExpressLegKey =
    ExpressLegKey(assetId = from.toAssetId(), address = fromAddress)

/**
 * Payout leg: the user's own address for a regular swap, an external one for a send-and-swap — which then holds no
 * portfolio currency and falls back to any copy of the asset.
 */
internal fun ExpressExchangeEntity.toLegKey(): ExpressLegKey =
    ExpressLegKey(assetId = to.toAssetId(), address = payoutAddress)

internal fun ExpressExchangeEntity.refundLegKey(): ExpressLegKey? =
    toRefundAssetId()?.let { ExpressLegKey(assetId = it, address = refundAddress) }

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

internal fun ExpressOnrampEntity.toLegKey(): ExpressLegKey =
    ExpressLegKey(assetId = to.toAssetId(), address = payoutAddress)